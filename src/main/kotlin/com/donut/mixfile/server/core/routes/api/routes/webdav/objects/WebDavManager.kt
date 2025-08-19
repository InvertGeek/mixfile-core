package com.donut.mixfile.server.core.routes.api.routes.webdav.objects


import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.server.core.objects.WebDavFile
import com.donut.mixfile.server.core.utils.*
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap


open class WebDavManager {

    companion object {
        const val VERSION_PREFIX = "V2_:\n"
    }

    open var WEBDAV_DATA = WebDavFile("root", isFolder = true)
    open var loaded = true

    // 私有锁对象
    private val lock = Any()

    fun dataToBytes(data: WebDavFile = WEBDAV_DATA): ByteArray = synchronized(lock) {
        compressGzip(VERSION_PREFIX + data.toJsonString())
    }

    fun loadDataFromBytes(data: ByteArray) = synchronized(lock) {
        WEBDAV_DATA = parseDataFromBytes(data)
    }

    fun parseDataFromBytes(data: ByteArray): WebDavFile = synchronized(lock) {
        try {
            val dataStr = decompressGzip(data)
            if (dataStr.startsWith(VERSION_PREFIX)) {
                return dataStr.substring(VERSION_PREFIX.length).parseJsonObject()
            }
            return loadLegacyData(dataStr)
        } catch (e: Exception) {
            throw Exception("载入WebDAV存档失败", e)
        }
    }

    fun importMixList(list: List<FileDataLog>, path: String = "") = synchronized(lock) {
        list.forEach {
            addFileNode(path, WebDavFile(it.getCategory(), isFolder = true))
            addFileNode(
                "${path}/${it.getCategory()}".normalPath(),
                WebDavFile(
                    name = it.name,
                    shareInfoData = it.shareInfoData,
                    size = it.size
                )
            )
        }
    }

    private fun loadLegacyData(data: String): WebDavFile {
        val serializer = ConcurrentHashMapSerializer(String.serializer(), SetSerializer(WebDavFile.serializer()))

        val davData: ConcurrentHashMap<String, Set<WebDavFile>> = Json.decodeFromString(serializer, data)

        val rootFile = WebDavFile("root", isFolder = true)

        davData.forEach { (path, fileList) ->
            if (path.isBlank()) return@forEach
            val pathSegments = path.split("/").filter { it.isNotEmpty() }
            var segmentFile = rootFile

            // 构建文件夹结构
            pathSegments.forEach { segment ->
                segmentFile = segmentFile.files.getOrPut(segment) {
                    WebDavFile(name = segment, isFolder = true)
                }
            }

            fileList.forEach {
                // 非文件夹则添加，添加文件夹可能覆盖已经有子files的文件夹
                if (!it.isFolder) {
                    segmentFile.files[it.getName()] = it
                }
            }
        }

        return rootFile
    }

    suspend fun saveData() {
        saveWebDavData(dataToBytes())
    }

    open suspend fun saveWebDavData(data: ByteArray) {}

    // 添加文件或目录到指定路径
    open fun addFileNode(path: String, file: WebDavFile, overwrite: Boolean = true): Boolean = synchronized(lock) {
        val folder = getFile(path) ?: return false
        if (!folder.isFolder) {
            return false
        }
        if (!overwrite) {
            val existingFile = getFile("${path}/${file.getName()}")
            if (existingFile != null) {
                return false
            }
        }
        folder.addFile(file)
        return true
    }

    open fun copyFile(
        path: String,
        dest: String,
        overwrite: Boolean,
        keep: Boolean = true
    ): Boolean = synchronized(lock) {

        if (path.contentEquals(dest)) {
            return false
        }

        //禁止移动到自己的子目录
        if (!keep && dest.startsWith("${path}/")) {
            return false
        }

        val srcFile = getFile(path) ?: return false

        if (!overwrite) {
            val destFile = getFile(dest)
            //目标存在且不覆盖
            if (destFile != null) {
                return false
            }
        }


        val destName = dest.pathFileName()

        val added = addFileNode(
            dest.parentPath(),
            srcFile.copy(
                name = destName,
                shareInfoData = srcFile.shareInfoData.let {
                    val shareInfo = resolveMixShareInfo(srcFile.shareInfoData)
                    shareInfo?.copy(fileName = destName)?.toString() ?: it
                })
        )

        if (!added) {
            return false
        }

        if (!keep) {
            removeFileNode(path)
        }
        return true
    }

    // 删除指定路径的文件或目录
    open fun removeFileNode(path: String): WebDavFile? = synchronized(lock) {
        val normalizedPath = normalizePath(path)
        val parentPath = normalizedPath.parentPath()
        val name = normalizedPath.pathFileName()
        val parentFolder = getFile(parentPath) ?: return null
        return parentFolder.files.remove(name)
    }

    open fun getFile(path: String): WebDavFile? = synchronized(lock) {
        val normalizedPath = normalizePath(path)
        val pathSegments = normalizedPath.split("/").filter { it.isNotEmpty() }
        var file = WEBDAV_DATA
        for (segment in pathSegments) {
            val pathFile = file.files[segment] ?: return null
            file = pathFile
        }
        return file
    }

    // 列出指定路径下的文件和目录
    open fun listFiles(path: String): List<WebDavFile>? = synchronized(lock) {
        val folder = getFile(path) ?: return null
        return folder.listFiles()
    }

}




