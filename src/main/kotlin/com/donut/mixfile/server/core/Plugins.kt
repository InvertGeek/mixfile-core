package com.donut.mixfile.server.core


import com.donut.mixfile.server.core.utils.genRandomString
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*

fun Route.interceptCall(
    call: suspend OnCallContext<Unit>.(PipelineCall) -> Unit,
    name: String = "InterceptCallPlugin-${genRandomString(32)}",
    build: Route.() -> Unit = {}
): Route {
    install(createRouteScopedPlugin(name) {
        onCall(call)
    }) {
        build()
    }
    return this
}

fun Route.mixBasicAuth(passwordFunc: () -> String, build: Route.() -> Unit = {}) =
    interceptCall({ call ->
        val password = passwordFunc()
        if (password.isBlank()) {
            return@interceptCall
        }

        val authHeader = call.request.header("Authorization")
        val key = authHeader.let {
            if (authHeader == null) {
                return@let ""
            }
            if (!authHeader.startsWith("Basic ")) {
                return@let ""
            }
            val encodedBasicValue = authHeader.substring(6)
            encodedBasicValue.decodeBase64String().split(":").lastOrNull() ?: ""
        }.ifEmpty { call.parameters["accessKey"] ?: "" }

        if (!password.trim().contentEquals(key.trim())) {
            call.response.apply {
                header("WWW-Authenticate", "Basic realm=\"mixfile\"")
            }
            call.respond(HttpStatusCode.Unauthorized)
        }
    }, name = "MixBasicAuthPlugin", build = build)
