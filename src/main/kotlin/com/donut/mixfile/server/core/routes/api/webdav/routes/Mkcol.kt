package com.donut.mixfile.server.core.routes.api.webdav.routes

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.objects.WebDavFile
import com.donut.mixfile.server.core.routes.api.webdav.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val MixFileServer.webDavMkcolRoute: Route.() -> Unit
    get() = {
        webdav("MKCOL") {

            if (davPath.isEmpty()) {
                call.respond(HttpStatusCode.Created)
                return@webdav
            }

            val shareInfo = davShareInfo
            //文件夹名称是分享码自动创建文件
            if (shareInfo != null) {
                val node = WebDavFile(
                    name = shareInfo.fileName,
                    size = shareInfo.fileSize,
                    shareInfoData = shareInfo.toString()
                )
                webDav.addFileNode(davParentPath, node)
                call.respond(HttpStatusCode.Created)
                webDav.saveData()
                return@webdav
            }

            val file = webDav.getFile(davPath)

            if (file != null) {
                call.respond(HttpStatusCode.MethodNotAllowed)
                return@webdav

            }

            val node = WebDavFile(isFolder = true, name = davFileName)
            val added = webDav.addFileNode(davParentPath, node)

            if (!added) {
                call.respond(HttpStatusCode.Conflict)
                return@webdav
            }

            call.respond(HttpStatusCode.Created)
            webDav.saveData()
        }
    }