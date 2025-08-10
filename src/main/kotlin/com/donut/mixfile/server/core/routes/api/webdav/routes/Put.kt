package com.donut.mixfile.server.core.routes.api.webdav.routes

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.objects.FileDataLog
import com.donut.mixfile.server.core.objects.WebDavFile
import com.donut.mixfile.server.core.routes.api.uploadFile
import com.donut.mixfile.server.core.routes.api.webdav.davFileName
import com.donut.mixfile.server.core.routes.api.webdav.davParentPath
import com.donut.mixfile.server.core.routes.api.webdav.receiveBytes
import com.donut.mixfile.server.core.routes.api.webdav.webdav
import com.donut.mixfile.server.core.utils.decompressGzip
import com.donut.mixfile.server.core.utils.extensions.mb
import com.donut.mixfile.server.core.utils.parseJsonObject
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val MixFileServer.webDavPutRoute: Route.() -> Unit
    get() = {
        webdav("PUT") {
            val fileSize = call.request.contentLength() ?: 0
            if (fileSize > 0 && fileSize < 50.mb) {
                if (davFileName.endsWith(".mix_dav")) {
                    val davData =
                        try {
                            webDav.parseDataFromBytes(receiveBytes(50.mb))
                        } catch (e: Exception) {
                            throw Exception("导入存档失败", e)
                        }
                    val parentFile = webDav.getFile(davParentPath)
                    if (parentFile == null || !parentFile.isFolder) {
                        call.respond(HttpStatusCode.Conflict)
                        return@webdav
                    }
                    davData.files.forEach {
                        parentFile.addFile(it.value)
                    }
                    call.respond(HttpStatusCode.Created)
                    webDav.saveData()
                    return@webdav
                }
                if (davFileName.endsWith(".mix_list")) {
                    val dataLogList = try {
                        decompressGzip(
                            receiveBytes(50.mb)
                        ).parseJsonObject<List<FileDataLog>>()
                    } catch (e: Exception) {
                        throw Exception("导入文件列表失败", e)
                    }
                    webDav.importMixList(dataLogList, davParentPath)
                    call.respond(HttpStatusCode.Created)
                    webDav.saveData()
                    return@webdav
                }
            }

            val fileList = webDav.listFiles(davParentPath)
            if (fileList == null) {
                call.respond(HttpStatusCode.Conflict)
                return@webdav
            }
            val (shareInfo, finalSize) = uploadFile(
                call.receiveChannel(),
                davFileName,
                fileSize,
                add = false
            )
            val fileNode =
                WebDavFile(size = finalSize, shareInfoData = shareInfo, name = davFileName)
            webDav.addFileNode(davParentPath, fileNode)
            call.respond(HttpStatusCode.Created)
            webDav.saveData()
        }
    }