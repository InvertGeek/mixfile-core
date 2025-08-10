package com.donut.mixfile.server.core.routes.api.webdav.routes

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.routes.api.respondMixFile
import com.donut.mixfile.server.core.routes.api.webdav.davFileName
import com.donut.mixfile.server.core.routes.api.webdav.davParentPath
import com.donut.mixfile.server.core.routes.api.webdav.davPath
import com.donut.mixfile.server.core.routes.api.webdav.webdav
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val MixFileServer.webDavGetRoute: Route.() -> Unit
    get() = {
        webdav("GET") {
            if (davFileName.contentEquals("当前目录存档.mix_dav")) {
                val file = webDav.getFile(davParentPath)
                if (file == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@webdav
                }
                val data = webDav.dataToBytes(file.copy(name = "root"))
                val fileName = "${davParentPath.ifEmpty { "root" }}.mix_dav".encodeURLParameter()
                call.response.apply {
                    header("Cache-Control", "no-cache, no-store, must-revalidate")
                    header("Pragma", "no-cache")
                    header("Expires", "0")
                    header(
                        "Content-Disposition",
                        "attachment;filename=\"$fileName\""
                    )
                }
                call.respondBytes(data, ContentType.Application.OctetStream)
                return@webdav
            }
            val fileNode = webDav.getFile(davPath)
            if (fileNode == null) {
                call.respond(HttpStatusCode.NotFound)
                return@webdav
            }
            if (fileNode.isFolder) {
                call.respond(HttpStatusCode.MethodNotAllowed)
                return@webdav
            }
            val shareInfo = resolveMixShareInfo(fileNode.shareInfoData)
            if (shareInfo == null) {
                call.respond(HttpStatusCode.InternalServerError)
                return@webdav
            }
            respondMixFile(call, shareInfo)
        }
    }