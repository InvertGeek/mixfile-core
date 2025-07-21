package com.donut.mixfile.server.core.uploaders.js

import com.dokar.quickjs.ExperimentalQuickJsApi
import com.dokar.quickjs.QuickJs
import com.dokar.quickjs.alias.asyncFunc
import com.dokar.quickjs.alias.def
import com.dokar.quickjs.alias.func
import com.dokar.quickjs.binding.JsObject
import com.dokar.quickjs.quickJs
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

val mutex = Mutex()

suspend fun runScript(code: String, client: HttpClient, variables: QuickJs.() -> Unit = {}): String {
    mutex.withLock {
        quickJs {
            variables()
            defaultVariables(client)()
            return evaluate<String>(code)
        }
    }
}

@OptIn(ExperimentalQuickJsApi::class)
fun defaultVariables(client: HttpClient): QuickJs.() -> Unit =
    {

        def("console") {
            func("log") {
                println(it.joinToString(" "))
            }
        }

        func<String>("hash") {
            val algorithm = it.first().toString().uppercase()
            val data = it[1].toString().decodeBase64Bytes()
            data.hashToHexString(algorithm)
        }

        func<String>("btoa") {
            it.first().toString().encodeBase64()
        }


        func<String>("atob") {
            it.first().toString().decodeBase64String()
        }

        func<String>("appendBase64") {
            val first = it[0].toString().decodeBase64Bytes()
            val second = it[1].toString().decodeBase64Bytes()
            (first + second).encodeBase64()
        }

        func<String>("encodeUrl") {
            it.first().toString().encodeURL()
        }

        func<String>("decodeUrl") {
            it.first().toString().decodeURLQueryComponent()
        }

        asyncFunc<String>("submitForm") { args ->
            val url = args.first().toString()
            val formValues = (args.getOrNull(1) as? JsObject) ?: JsObject(mapOf())
            val reqHeaders = (args.getOrNull(2) as? JsObject) ?: JsObject(mapOf())
            val reqForm = formData {
                formValues.forEach {
                    val data = it.value
                    if (data is List<*>) {
                        val imageData = data.first().toString().decodeBase64Bytes()
                        add(it.key, imageData, fileFormHeaders())
                        return@forEach
                    }
                    add(it.key, data)
                }
            }
            client.submitFormWithBinaryData(
                url,
                reqForm
            ) {
                reqHeaders.forEach {
                    header(it.key, it.value)
                }
            }.body<ByteArray>().encodeBase64()
        }

        asyncFunc<String>("http") { args ->
            val reqMethod = args.getOrNull(0) as? String ?: ""
            val reqUrl = args.getOrNull(1) as? String ?: ""
            val reqBody = (args.getOrNull(2)?.toString() ?: "").decodeBase64Bytes()
            val reqHeaders = (args.getOrNull(3) as? JsObject) ?: JsObject(mapOf())
            client.request {
                method = HttpMethod(reqMethod.uppercase())
                url(reqUrl)
                reqHeaders.forEach {
                    header(it.key, it.value)
                }
                setBody(reqBody)
            }.body<ByteArray>().encodeBase64()
        }


    }


