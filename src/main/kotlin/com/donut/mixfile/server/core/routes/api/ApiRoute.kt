package com.donut.mixfile.server.core.routes.api

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.mixBasicAuth
import com.donut.mixfile.server.core.routes.api.webdav.getWebDAVRoute
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import com.donut.mixfile.server.core.utils.toJsonString
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun MixFileServer.getAPIRoute(): Route.() -> Unit {
    return {
        mixBasicAuth({ password })

        route("/webdav/{param...}", getWebDAVRoute())

        get("/download/{name?}", getDownloadRoute())

        put("/upload/{name?}", getUploadRoute())

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
}