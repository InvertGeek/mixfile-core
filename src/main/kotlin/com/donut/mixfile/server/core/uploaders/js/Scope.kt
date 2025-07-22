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

fun getRhinoScope(context: Context, client: HttpClient): Scriptable {

    val scope: Scriptable = context.initStandardObjects()

    scope.putFunc("hash") {
        val algorithm = it.first().toString().uppercase()
        val data = it[1].toString().decodeBase64Bytes()
        data.hashToHexString(algorithm)
    }

    scope.putFunc("btoa") {
        it.first().toString().encodeBase64()
    }


    scope.putFunc("atob") {
        it.first().toString().decodeBase64String()
    }

    scope.putFunc("appendBase64") {
        val first = it[0].toString().decodeBase64Bytes()
        val second = it[1].toString().decodeBase64Bytes()
        (first + second).encodeBase64()
    }

    scope.putFunc("encodeUrl") {
        it.first().toString().encodeURL()
    }

    scope.putFunc("decodeUrl") {
        it.first().toString().decodeURLQueryComponent()
    }

    scope.putFunc("print") {
        println(it.joinToString(" "))
    }

    scope.putFunc("submitForm") { args ->
        val url = args.first().toString()
        val formValues = (args.getOrNull(1) as? NativeObject) ?: NativeObject()
        val reqHeaders = (args.getOrNull(2) as? NativeObject) ?: NativeObject()
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
                add(key, data)
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
        val reqMethod = args.getOrNull(0) as? String ?: ""
        val reqUrl = args.getOrNull(1) as? String ?: ""
        val reqBody = (args.getOrNull(2)?.toString() ?: "").decodeBase64Bytes()
        val reqHeaders = (args.getOrNull(3) as? NativeObject) ?: NativeObject()
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