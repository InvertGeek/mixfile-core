package com.donut.mixfile.server.core.routes.api

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.mixBasicAuth
import com.donut.mixfile.server.core.routes.api.routes.downloadRoute
import com.donut.mixfile.server.core.routes.api.routes.uploadRoute
import com.donut.mixfile.server.core.routes.api.routes.webdav.webDavRoute
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import com.donut.mixfile.server.core.utils.toJsonString
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

val MixFileServer.apiRoute: Route.() -> Unit
    get() = {

        mixBasicAuth({ password })

        route("/webdav/{param...}", webDavRoute)

        get("/download/{name?}", downloadRoute)

        put("/upload/{name?}", uploadRoute)

        get("/upload_history") {
            call.respond(getFileHistory())
        }

        get("/file_info") {

            val shareInfoStr = call.parameters["s"]
            if (shareInfoStr == null) {
                call.respondText("分享信息为空", status = HttpStatusCode.InternalServerError)
                return@get
            }

            val shareInfo = resolveMixShareInfo(shareInfoStr)

            if (shareInfo == null) {
                call.respondText(
                    "分享信息解析失败",
                    status = HttpStatusCode.InternalServerError
                )
                return@get
            }

            val jsonObject = buildJsonObject {
                put("name", shareInfo.fileName)
                put("size", shareInfo.fileSize)
            }

            call.respondText(jsonObject.toJsonString())
        }
    }