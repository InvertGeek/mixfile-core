package com.donut.mixfile.server.core.routes.api.webdav.objects


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

    @Synchronized
    fun dataToBytes(data: WebDavFile = WEBDAV_DATA) =
        compressGzip(VERSION_PREFIX + data.toJsonString())

    @Synchronized
    fun loadDataFromBytes(data: ByteArray) {
        WEBDAV_DATA = parseDataFromBytes(data)
    }

    @Synchronized
    fun parseDataFromBytes(data: ByteArray): WebDavFile {
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

    @Synchronized
    fun importMixList(list: List<FileDataLog>, path: String = "") {
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

    @Synchronized
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
    @Synchronized
    open fun addFileNode(path: String, file: WebDavFile): Boolean {
        val folder = getFile(path) ?: return false
        if (!folder.isFolder) {
            return false
        }
        folder.addFile(file)
        return true
    }

    @Synchronized
    open fun copyFile(
        path: String,
        dest: String,
        overwrite: Boolean,
        keep: Boolean = true
    ): Boolean {

        if (path.contentEquals(dest)) {
            return false
        }

        //禁止移动到自己的子目录
        if (!keep && dest.startsWith("${path}/")) {
            return false
        }

        val srcFile = getFile(path) ?: return false

        val destFile = getFile(dest)

        //目标存在且不覆盖
        if (!overwrite && destFile != null) {
            return false
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
    @Synchronized
    open fun removeFileNode(path: String): WebDavFile? {
        val normalizedPath = normalizePath(path)
        val parentPath = normalizedPath.parentPath()
        val name = normalizedPath.pathFileName()
        val parentFolder = getFile(parentPath) ?: return null
        return parentFolder.files.remove(name)
    }

    @Synchronized
    open fun getFile(path: String): WebDavFile? {
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
    @Synchronized
    open fun listFiles(path: String): List<WebDavFile>? {
        val folder = getFile(path) ?: return null
        return folder.listFiles()
    }


}



