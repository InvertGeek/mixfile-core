package com.donut.mixfile.server.core.uploaders.base.js

import com.donut.mixfile.server.core.utils.add
import com.donut.mixfile.server.core.utils.encodeURL
import com.donut.mixfile.server.core.utils.hashToHexString
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import kotlinx.coroutines.runBlocking
import org.mozilla.javascript.*
import org.mozilla.javascript.Function
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock


fun Scriptable.put(name: String, value: Any?) {
    put(name, this, value)
}

fun <T> Scriptable.putFunc(name: String, func: (args: Array<out Any>) -> T?) {
    put(name, JSFunction {
        try {
            func(it)
        } catch (e: Exception) {
            //不能抛出Exception Rhino会提示无法将Exception cast为error
            throw Error("JS函数(${name})执行出错: ${e.message}", e)
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

private val GLOBAL_LOCKS = ConcurrentHashMap<String, ReentrantLock>()

fun getRhinoScope(context: Context, client: HttpClient): Scriptable {

    val scope: Scriptable = context.initStandardObjects()

    // JS: lock("key", () => { ... })
    scope.putFunc("lock") { args ->
        val key = args.param(0, "")
        val callback = args[1] as Function

        val lock = GLOBAL_LOCKS.computeIfAbsent(key) { ReentrantLock() }

        return@putFunc try {
            lock.lock()
            callback.call(context, scope, scope, emptyArray())
        } finally {
            lock.unlock()
        }
    }

    // 缓存 putCache(key, value, expireSeconds)
    scope.putFunc("putCache") {
        val key = it.param(0, "")
        val value = it.param(1, "")
        val expireSec = it.param(2, 0.0).toInt()
        val expireAt = if (expireSec < 0) NEVER_EXPIRE else System.currentTimeMillis() + expireSec * 1000L
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

    fun parseFormData(obj: NativeObject) = formData {
        obj.forEach { entry ->
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

    scope.putFunc("submitForm") { args ->
        val url = args.param(0, "")
        val formValues = args.param(1, NativeObject())
        val reqHeaders = args.param(2, NativeObject())
        val reqForm = parseFormData(formValues)
        runBlocking {
            client.submitFormWithBinaryData(
                url,
                reqForm
            ) {
                reqHeaders.forEach {
                    header(it.key.toString(), it.value)
                }
            }.bodyAsBytes().encodeBase64()
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
            }.bodyAsBytes().encodeBase64()
        }
    }

    scope.putFunc("request") { args ->

        val reqMethod = args.param(0, "")

        val reqUrl = args.param(1, "")

        val reqHeaders = args.param(3, NativeObject())

        val reqBody = args.param(2, Any()).let {
            if (it is String) {
                return@let it
            }
            if (it is NativeObject) {
                return@let MultiPartFormDataContent(parseFormData(it))
            }
            if (it is NativeArray) {
                val first = it.firstOrNull() ?: return@let null
                return@let first.toString().decodeBase64Bytes()
            }
            return@let null
        }


        runBlocking {

            val response = client.request {
                method = HttpMethod(reqMethod.uppercase())
                url(reqUrl)
                reqHeaders.forEach {
                    header(it.key.toString(), it.value)
                }
                setBody(reqBody)
            }

            val body = response.bodyAsBytes()

            val headers = response.headers.let {
                context.newObject(scope).apply {
                    it.names().forEach { name ->
                        put(name, it[name].toString())
                    }
                }
            }

            context.newObject(scope).apply {
                put("statusCode", response.status.value)
                put("headers", headers)
                put("rawData", body.encodeBase64())
                put("text", body.decodeToString())
            }

        }
    }

    return scope
}