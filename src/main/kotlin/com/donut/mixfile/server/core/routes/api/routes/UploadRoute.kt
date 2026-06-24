package com.donut.mixfile.server.core.routes.api.routes

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.aes.generateRandomByteArray
import com.donut.mixfile.server.core.objects.MixFile
import com.donut.mixfile.server.core.objects.MixShareInfo
import com.donut.mixfile.server.core.utils.MixUploadTask
import com.donut.mixfile.server.core.utils.extensions.mb
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.ceil
import kotlin.math.min


val MixFileServer.uploadRoute: RoutingHandler
    get() = route@{

        val params = call.parameters

        val name = params["name"]

        val add = params["add"] ?: "true"

        if (name.isNullOrEmpty()) {
            call.respondText("需要文件名称", status = HttpStatusCode.InternalServerError)
            return@route
        }

        val size = call.request.contentLength() ?: 0

        call.respondText(uploadFile(call.receiveChannel(), name, size, add.toBoolean()) {
            call.respondText("上传已取消", status = HttpStatusCode.InternalServerError)
        }.first)
    }


suspend fun MixFileServer.uploadFile(
    channel: ByteReadChannel,
    name: String,
    size: Long,
    add: Boolean = true,
    key: ByteArray = generateRandomByteArray(32),
    onStop: suspend () -> Unit = {}
): Pair<String, Long> {

    val uploadTask = getUploadTask(name, size, add)

    uploadTask.stopFunc.add(onStop)

    currentCoroutineContext().job.invokeOnCompletion {
        uploadTask.stop(it)
    }

    val uploader = getUploader()

    val head = uploader.genHead(httpClient) ?: genDefaultImage()

    val (mixUrl, fileSize) =
        doUploadFile(channel, head, uploader, key, fileSize = size, uploadTask)

    val mixShareInfo =
        MixShareInfo(
            fileName = name,
            fileSize = fileSize,
            headSize = head.size,
            url = mixUrl,
            key = MixShareInfo.ENCODER.encode(key),
            referer = uploader.referer
        )
    uploadTask.complete(mixShareInfo)
    return mixShareInfo.toString() to fileSize
}

private suspend fun MixFileServer.doUploadFile(
    channel: ByteReadChannel,
    head: ByteArray,
    uploader: Uploader,
    secret: ByteArray,
    fileSize: Long,
    uploadTask: MixUploadTask,
): Pair<String, Long> {

    val chunkSizeMB = chunkSize / 1.mb

    val semaphore = Semaphore((uploadTaskCount / chunkSizeMB.coerceAtLeast(1)).coerceAtLeast(1))

    return coroutineScope {

        uploadTask.stopFunc.add(0) {
            channel.cancel()
        }

        val fixedChunkSize = min(20.mb, chunkSize)

        val chunkCount = ceil(fileSize / fixedChunkSize.toDouble()).toInt()
        val uploadedChunkCount = AtomicInteger(0)
        val chunkList = mutableListOf<String>()

        var chunkIndex = 0

        var totalChunkSize = 0L

        val tasks = mutableListOf<Deferred<Unit>>()


        while (!channel.isClosedForRead) {
            semaphore.acquire()
            // 必须精确读满 fixedChunkSize(末片除外)。不能用 readRemaining: 在 IO 线程
            // 投递节奏异常(如 CPU 卡顿/长 GC)时它可能多读
            val chunkData = channel.readChunkExact(fixedChunkSize)
            if (chunkData.isEmpty()) {
                semaphore.release()
                break
            }
            val currentChunkSize = chunkData.size
            totalChunkSize += currentChunkSize
            val currentIndex = chunkIndex
            chunkList.add("")
            chunkIndex++
            tasks.add(async {
                try {
                    val url = uploader.upload(head, chunkData, secret, this@doUploadFile)
                    chunkList[currentIndex] = url
                    uploadedChunkCount.incrementAndGet()
                    uploadTask.updateProgress(currentChunkSize.toLong(), fileSize)
                } finally {
                    semaphore.release()
                }
            })
        }

        tasks.awaitAll()

        if (uploadedChunkCount.get() < chunkCount) {
            throw Exception("上传失败,分片数量不足: ${uploadedChunkCount} < ${chunkCount}")
        }

        val mixFile =
            MixFile(
                chunkSize = fixedChunkSize,
                version = 0,
                fileList = chunkList,
                fileSize = totalChunkSize
            )

        val mixFileData = mixFile.toBytes()

        try {
            val mixFileUrl =
                uploader.upload(head, mixFileData, secret, this@doUploadFile)
            return@coroutineScope mixFileUrl to totalChunkSize
        } catch (e: Exception) {
            throw Exception("索引文件上传失败", e)
        }

    }
}

/**
 * 精确读满 [size] 字节;通道提前关闭则返回已读到的字节(可能短于 size, 用作末片)。
 *
 * 用 [readAvailable] 写入到固定大小的目标数组——写入量受数组长度物理封顶,
 * 在构造上**不可能**超读。这是为了避开 [readRemaining] 在 IO 线程节奏
 * 异常(CPU 长卡顿/长 GC)下偶发的超读: 单次超读会让某片 != fixedChunkSize,
 */
private suspend fun ByteReadChannel.readChunkExact(size: Int): ByteArray {
    val buf = ByteArray(size)
    var off = 0
    while (off < size) {
        val n = readAvailable(buf, off, size - off)
        if (n < 0) break // 通道关闭
        off += n
    }
    if (off > size) {
        // 物理上不可能, 但作为防御性断言保留
        throw IllegalStateException("分片读取超规格: $off > $size")
    }
    return if (off == size) buf else buf.copyOf(off)
}
