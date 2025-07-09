package com.donut.mixfile.server.core

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import okhttp3.Dispatcher

private val client = HttpClient(OkHttp) {
    engine {
        config {
            dispatcher(Dispatcher().apply {
                maxRequestsPerHost = Int.MAX_VALUE
                maxRequests = Int.MAX_VALUE
            })
        }
    }
    install(ContentNegotiation) {

    }
    install(HttpTimeout) {
        requestTimeoutMillis = 1000 * 300
        socketTimeoutMillis = 1000 * 60
    }
    install(DefaultRequest) {
        userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Safari/537.36")
    }
}

val MixFileServer.defaultClient
    get() = client