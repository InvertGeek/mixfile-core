package com.donut.mixfile.server.core


import com.donut.mixfile.server.core.objects.MixShareInfo
import com.donut.mixfile.server.core.routes.api.webdav.objects.WebDavManager
import com.donut.mixfile.server.core.utils.MixUploadTask
import com.donut.mixfile.server.core.utils.extensions.mb
import com.donut.mixfile.server.core.utils.findAvailablePort
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import java.io.InputStream


abstract class MixFileServer(
    var serverPort: Int = 4719,
    val host: String = "0.0.0.0"
) {

    init {
        initPatchTransforms()
    }


    open val downloadTaskCount: Int = 5

    open val uploadTaskCount: Int = 10

    open val uploadRetryCount: Int = 10

    open val downloadRetryCount: Int = 3

    @Volatile
    var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
        private set

    open val chunkSize = 1.mb

    open val password: String = ""

    open val extendModule: Application.() -> Unit = {}

    abstract fun onError(error: Throwable)

    abstract fun getUploader(): Uploader

    open suspend fun getStaticFile(path: String): InputStream? {
        return javaClass.getResourceAsStream("/mixfile_static/${path}")
    }

    // 默认返回1像素GIF，最小尺寸
    open suspend fun genDefaultImage(): ByteArray =
        "R0lGODlhAQABAIAAAP///wAAACwAAAAAAQABAAACAkQBADs=".decodeBase64Bytes()

    open suspend fun getFileHistory(): String = "[]"

    open val httpClient = defaultClient

    open fun getUploadTask(
        name: String,
        size: Long,
        add: Boolean
    ): MixUploadTask = object : MixUploadTask {
        override var error: Throwable? = null

        override var stopped: Boolean = false

        override suspend fun complete(shareInfo: MixShareInfo) {
        }

        override val stopFunc: MutableList<suspend () -> Unit> = mutableListOf()

        override suspend fun updateProgress(size: Long, total: Long) {
        }

    }

    open fun onDownloadData(data: ByteArray) {

    }

    open fun onUploadData(data: ByteArray) {

    }

    open val webDav = WebDavManager()


    private val serverLock = Any()


    fun start(wait: Boolean) {
        synchronized(serverLock) {
            server?.stop()
            serverPort = findAvailablePort(serverPort) ?: serverPort
            val fileServer = embeddedServer(
                factory = Netty,
                host = host,
                port = serverPort,
                watchPaths = emptyList()
            ) {
                defaultModule()
                extendModule()
            }
            server = fileServer
            fileServer.start(wait = wait)
        }
    }

    fun stop() {
        synchronized(serverLock) {
            server?.stop()
            server = null
        }
    }

}

