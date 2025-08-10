package com.donut.mixfile.server.core.routes.api.webdav


import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.interceptCall
import com.donut.mixfile.server.core.objects.MixShareInfo
import com.donut.mixfile.server.core.objects.WebDavFile
import com.donut.mixfile.server.core.routes.api.webdav.objects.WebDavManager
import com.donut.mixfile.server.core.routes.api.webdav.objects.normalPath
import com.donut.mixfile.server.core.routes.api.webdav.objects.parentPath
import com.donut.mixfile.server.core.routes.api.webdav.objects.pathFileName
import com.donut.mixfile.server.core.routes.api.webdav.routes.*
import com.donut.mixfile.server.core.utils.extensions.decodedPath
import com.donut.mixfile.server.core.utils.extensions.paramPath
import com.donut.mixfile.server.core.utils.extensions.routePrefix
import com.donut.mixfile.server.core.utils.getHeader
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*
import kotlinx.io.readByteArray


suspend fun RoutingContext.receiveBytes(limit: Int) =
    call.receiveChannel().readRemaining(limit.toLong()).readByteArray()


val RoutingContext.davPath: String
    get() = paramPath

val RoutingContext.davParentPath: String
    get() = davPath.parentPath()

val RoutingContext.davFileName: String
    get() = davPath.pathFileName()

val RoutingContext.davShareInfo: MixShareInfo?
    get() = resolveMixShareInfo(
        davPath.substringAfterLast(
            "/"
        )
    )


suspend fun RoutingContext.handleCopy(keep: Boolean, webDavManager: WebDavManager) {

    val overwrite = getHeader("overwrite").contentEquals("T")

    val destination = getHeader("destination").decodeURLQueryComponent().substringAfter(routePrefix).normalPath()

    if (destination.isBlank()) {
        call.respond(HttpStatusCode.BadRequest)
        return
    }

    val moved = webDavManager.copyFile(davPath, destination, overwrite, keep)

    if (!moved) {
        call.respond(HttpStatusCode.PreconditionFailed)
        return
    }

    call.respond(HttpStatusCode.Created)
    webDavManager.saveData()
}

fun MixFileServer.getWebDAVRoute(): Route.() -> Unit {
    return {
        interceptCall({
            if (!webDav.loaded) {
                it.respond(HttpStatusCode.ServiceUnavailable, "WebDav is Loading")
            }
        })
        webDavPropfindRoute()
        webDavOptionsRoute()
        webDavGetRoute()
        webDavPutRoute()
        webDavMkcolRoute()
        webdav("MOVE") {
            handleCopy(false, webDav)
        }
        webdav("COPY") {
            handleCopy(true, webDav)
        }
        webdav("DELETE") {
            webDav.removeFileNode(davPath)
            call.respond(HttpStatusCode.NoContent)
            webDav.saveData()
        }
    }
}

suspend fun ApplicationCall.respondXml(xml: String) {
    respondText(
        contentType = ContentType.Text.Xml.withCharset(Charsets.UTF_8),
        status = HttpStatusCode.MultiStatus,
        text = compressXml(
            """<?xml version="1.0" encoding="UTF-8"?>$xml"""
        )
    )
}

suspend fun RoutingContext.respondRootFile(file: WebDavFile?) {
    if (file == null) {
        call.respond(HttpStatusCode.NotFound)
        return
    }
    val text = """
                <D:multistatus xmlns:D="DAV:">
                ${file.toXML(decodedPath, true)}
                </D:multistatus>
                """
    call.respondXml(text)
}

fun compressXml(xmlString: String): String {
    // 1. 移除换行符和多余的空白字符
    var compressed = xmlString.replace("\\s+".toRegex(), " ")

    // 2. 去除标签之间的多余空格
    compressed = compressed.replace("> <", "><").trim()

    return compressed
}

fun Route.webdav(method: String, handler: RoutingHandler) {
    route("/{path...}") {
        method(HttpMethod(method)) {
            handle(handler)
        }
    }
}


