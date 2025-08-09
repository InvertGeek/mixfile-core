package com.donut.mixfile.server.core

import com.donut.mixfile.server.core.routes.getRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

val MixFileServer.defaultModule: Application.() -> Unit
    get() = {
        install(ContentNegotiation) {

        }
        install(CORS) {
            allowOrigins { true }
            allowHeaders { true }
            anyHost()
            anyMethod()
            allowMethod(HttpMethod("PROPFIND"))
            allowMethod(HttpMethod("MOVE"))
            allowMethod(HttpMethod("MKCOL"))
            allowMethod(HttpMethod("COPY"))
            allowHeader(HttpHeaders.AccessControlAllowOrigin)
            allowHeader(HttpHeaders.AccessControlAllowMethods)
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowCredentials = true
        }
        install(DefaultHeaders) {
            header(
                "Date", DateTimeFormatter.RFC_1123_DATE_TIME
                    .format(ZonedDateTime.now(java.time.ZoneOffset.UTC))
            )
            header("x-powered-by", "MixFile")
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


