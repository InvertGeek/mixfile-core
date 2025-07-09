package com.donut.mixfile.server.core.utils.extensions

import com.donut.mixfile.server.core.routes.api.webdav.objects.normalPath
import com.donut.mixfile.server.core.routes.api.webdav.objects.normalizePath
import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.routing.*

val RoutingContext.decodedPath: String get() = call.request.path().decodeURLQueryComponent()

val RoutingContext.paramPath: String
    get() = normalizePath(
        call.parameters.getAll("param")?.joinToString("/") ?: ""
    )

val RoutingContext.routePrefix: String
    get() {
        val dPath = decodedPath.normalPath()
        return dPath.take(dPath.length - paramPath.length).normalPath()
    }