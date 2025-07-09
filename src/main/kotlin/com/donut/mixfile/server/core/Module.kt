package com.donut.mixfile.server.core

import com.donut.mixfile.server.core.routes.getRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

val MixFileServer.defaultModule: Application.() -> Unit
    get() = {
        install(ContentNegotiation) {

        }
        install(CORS) {
            allowOrigins { true }
            anyHost()
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Put)
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            allowHeader(HttpHeaders.AccessControlAllowMethods)
            allowHeader(HttpHeaders.ContentType)
        }
        install(StatusPages) {
            exception<Throwable> { call, cause ->
                if (!call.response.isCommitted) {
                    call.respondText(
                        "发生错误: ${cause.message} ${cause.stackTraceToString()}",
                        status = HttpStatusCode.InternalServerError
                    )
                }
                onError(cause)
            }
        }
        routing(getRoutes())
        extendModule()
    }


