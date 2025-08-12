package com.donut.mixfile.server.core.routes.api.webdav.routes

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.objects.WebDavFile
import com.donut.mixfile.server.core.routes.api.webdav.davPath
import com.donut.mixfile.server.core.routes.api.webdav.respondRootFile
import com.donut.mixfile.server.core.routes.api.webdav.respondXml
import com.donut.mixfile.server.core.routes.api.webdav.webdav
import com.donut.mixfile.server.core.utils.extensions.decodedPath
import com.donut.mixfile.server.core.utils.getHeader
import io.ktor.server.routing.*

val MixFileServer.webDavPropfindRoute: Route.() -> Unit
    get() = {
        webdav("PROPFIND") {

            val depth = getHeader("depth").toIntOrNull() ?: 0

            val file = webDav.getFile(davPath)
            if (depth == 0) {
                respondRootFile(file)
                return@webdav
            }

            val fileList = webDav.listFiles(davPath)

            if (fileList == null) {
                respondRootFile(file)
                return@webdav
            }

            val xmlFileList = fileList.toMutableList().apply {
                if (isNotEmpty()) {
                    add(WebDavFile("当前目录存档.mix_dav", isFolder = false))
                }
            }.joinToString(separator = "") {
                it.toXML(decodedPath)
            }

            val rootFile = webDav.getFile(davPath) ?: WebDavFile("root", isFolder = true)

            val text = """
                <D:multistatus xmlns:D="DAV:">
                ${rootFile.toXML(decodedPath, true)}
                $xmlFileList
                </D:multistatus>
                """
            call.respondXml(text)
        }
    }