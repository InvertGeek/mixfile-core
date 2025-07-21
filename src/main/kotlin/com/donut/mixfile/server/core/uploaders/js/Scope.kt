package com.donut.mixfile.server.core.uploaders.js

import com.donut.mixfile.server.core.utils.add
import com.donut.mixfile.server.core.utils.encodeURL
import com.donut.mixfile.server.core.utils.fileFormHeaders
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

fun getRhinoScope(context: Context, client: HttpClient): Scriptable {

    val scope: Scriptable = context.initStandardObjects()

    scope.put("hash", JSFunction {
        val algorithm = it.first().toString().uppercase()
        val data = it[1].toString().decodeBase64Bytes()
        data.hashToHexString(algorithm)
    })

    scope.put("btoa", JSFunction {
        it.first().toString().encodeBase64()
    })


    scope.put("atob", JSFunction {
        it.first().toString().decodeBase64String()
    })

    scope.put("appendBase64", JSFunction {
        val first = it[0].toString().decodeBase64Bytes()
        val second = it[1].toString().decodeBase64Bytes()
        (first + second).encodeBase64()
    })

    scope.put("encodeUrl", JSFunction {
        it.first().toString().encodeURL()
    })

    scope.put("decodeUrl", JSFunction {
        it.first().toString().decodeURLQueryComponent()
    })

    scope.put("print", JSFunction {
        println(it.joinToString(" "))
    })

    scope.put("submitForm", JSFunction { args ->
        val url = args.first().toString()
        val formValues = (args.getOrNull(1) as? NativeObject) ?: NativeObject()
        val reqHeaders = (args.getOrNull(2) as? NativeObject) ?: NativeObject()
        val reqForm = formData {
            formValues.forEach {
                val data = it.value
                if (data is NativeArray) {
                    val imageData = data.first().toString().decodeBase64Bytes()
                    add(it.key.toString(), imageData, fileFormHeaders())
                    return@forEach
                }
                add(it.key.toString(), data)
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
    })

    scope.put("http", JSFunction { args ->
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
    })

    return scope
}