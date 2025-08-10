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
            val fileList = webDav.listFiles(davParentPath)
            if (fileList == null) {
                call.respond(HttpStatusCode.Conflict)
                return@webdav
            }
            val shareInfo = davShareInfo
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
            val node = WebDavFile(isFolder = true, name = davFileName)
            webDav.addFileNode(davParentPath, node)
            call.respond(HttpStatusCode.Created)
            webDav.saveData()
        }
    }