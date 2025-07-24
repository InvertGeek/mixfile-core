package com.donut.mixfile.server.core.uploaders.base.js

import com.donut.mixfile.server.core.defaultClient
import io.ktor.client.*
import org.mozilla.javascript.Context
import org.mozilla.javascript.Scriptable


fun getContext(): Context = Context.enter().apply {
    //禁用优化(动态创建class)，否则无法在安卓运行
    optimizationLevel = -1
    languageVersion = Context.VERSION_ES6
}

fun runScript(code: String, client: HttpClient = defaultClient, variables: Scriptable.() -> Unit = {}): String {
    val context = getContext()
    context.use {
        val scope = getRhinoScope(it, client)
        listOf(
            "Packages",
            "getClass",
            "JavaAdapter",
            "JavaImporter",
            "java",
            "javax",
            "org",
            "com",
            "edu",
            "net",
            "android"
        ).forEach { name ->
            scope.delete(name)
        }
        scope.variables()
        val result = it.evaluateString(scope, code, "mixfile_uploader", 1, null)
        return result.toString()
    }
}

