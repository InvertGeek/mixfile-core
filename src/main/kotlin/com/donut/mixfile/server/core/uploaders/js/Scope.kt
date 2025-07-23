package com.donut.mixfile.server.core.uploaders.js

import com.donut.mixfile.server.core.utils.add
import com.donut.mixfile.server.core.utils.encodeURL
import com.donut.mixfile.server.core.utils.hashToHexString
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeArray
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.Scriptable
import java.util.concurrent.ConcurrentHashMap


fun Scriptable.put(name: String, value: Any?) {
    put(name, this, value)
}

fun <T> Scriptable.putFunc(name: String, func: (args: Array<out Any>) -> T?) {
    put(name, JSFunction {
        try {
            func(it)
        } catch (e: Exception) {
            //不能抛出Exception Rhino会提示无法将Exception cast为error
            throw Error("JS函数(${name})执行出错: ${e.message}")
        }
    })
}

inline fun <reified T> Array<out Any?>.param(index: Int, default: T): T {
    val param = this.getOrNull(index) ?: return default
    //处理 org.mozilla.javascript.ConsString
    if (default is String) {
        return param.toString() as T
    }
    return param as T
}

private val GLOBAL_CACHE = ConcurrentHashMap<String, Pair<String, Long>>()
private const val NEVER_EXPIRE: Long = Long.MAX_VALUE

fun getRhinoScope(context: Context, client: HttpClient): Scriptable {

    val scope: Scriptable = context.initStandardObjects()

    // 缓存 putCache(key, value, expireSeconds)
    scope.putFunc("putCache") {
        val key = it.param(0, "")
        val value = it.param(1, "")
        val expireSec = it.param(2, 0.0).toInt()
        val expireAt = if (expireSec == -1) NEVER_EXPIRE else System.currentTimeMillis() + expireSec * 1000
        GLOBAL_CACHE[key] = value to expireAt
    }

    // 缓存 getCache(key) => string or null
    scope.putFunc("getCache") {
        val key = it.param(0, "")
        val entry = GLOBAL_CACHE[key] ?: return@putFunc null
        val (data, expireTime) = entry
        if (System.currentTimeMillis() <= expireTime) {
            return@putFunc data
        }
        GLOBAL_CACHE.remove(key)
        null
    }

    scope.putFunc("hash") {
        val algorithm = it.param(0, "").uppercase()
        val data = it[1].toString().decodeBase64Bytes()
        data.hashToHexString(algorithm)
    }

    scope.putFunc("btoa") {
        it.param(0, "").encodeBase64()
    }

    scope.putFunc("atob") {
        it.param(0, "").decodeBase64String()
    }

    scope.putFunc("appendBase64") {
        val first = it.param(0, "").decodeBase64Bytes()
        val second = it.param(1, "").decodeBase64Bytes()
        (first + second).encodeBase64()
    }

    scope.putFunc("encodeUrl") {
        it.param(0, "").encodeURL()
    }

    scope.putFunc("decodeUrl") {
        it.param(0, "").decodeURLQueryComponent()
    }

    scope.putFunc("print") {
        println(it.joinToString(" "))
    }

    scope.putFunc("submitForm") { args ->
        val url = args.param(0, "")
        val formValues = args.param(1, NativeObject())
        val reqHeaders = args.param(2, NativeObject())
        val reqForm = formData {
            formValues.forEach { entry ->
                val data = entry.value
                val key = entry.key.toString()
                if (data is NativeArray) {
                    val fileInfo = data.toList().map { it.toString() }
                    val imageData = fileInfo[0].decodeBase64Bytes()
                    val fileName = fileInfo[1]
                    val contentType = fileInfo[2]
                    add(key, imageData, Headers.build {
                        append(HttpHeaders.ContentType, contentType)
                        append(
                            HttpHeaders.ContentDisposition,
                            "filename=\"${fileName}\""
                        )
                    })
                    return@forEach
                }
                //处理 org.mozilla.javascript.ConsString
                add(key, data.toString())
            }
        }
        runBlocking {
            client.submitFormWithBinaryData(
                url,
                reqForm
            ) {
                reqHeaders.forEach {
                    header(it.key.toString(), it.value)
                }
            }.body<ByteArray>().encodeBase64()
        }
    }

    scope.putFunc("http") { args ->
        val reqMethod = args.param(0, "")
        val reqUrl = args.param(1, "")
        val reqBody = args.param(2, "").decodeBase64Bytes()
        val reqHeaders = args.param(3, NativeObject())
        runBlocking {
            client.request {
                method = HttpMethod(reqMethod.uppercase())
                url(reqUrl)
                reqHeaders.forEach {
                    header(it.key.toString(), it.value)
                }
                setBody(reqBody)
            }.body<ByteArray>().encodeBase64()
        }
    }

    return scope
}