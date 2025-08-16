package com.donut.mixfile.server.core.routes.api.routes.webdav.routes

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.routes.api.routes.webdav.davPath
import com.donut.mixfile.server.core.routes.api.routes.webdav.webdav
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val MixFileServer.webDavOptionsRoute: Route.() -> Unit
    get() = {
        webdav("OPTIONS") {
            fun getAllowHeader(): String {
                val file = webDav.getFile(davPath) ?: return "OPTIONS, PUT, MKCOL"
                if (file.isFolder) {
                    return "OPTIONS, DELETE, PROPPATCH, COPY, MOVE, PROPFIND"
                }
                return "OPTIONS, GET, HEAD, POST, DELETE, COPY, MOVE, PROPFIND, PUT"
            }
            call.response.apply {
                header("Allow", getAllowHeader())
                header("Dav", "1")
                header("Ms-Author-Via", "DAV")
            }
            call.respond(HttpStatusCode.OK)
        }
    }