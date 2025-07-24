package com.donut.mixfile.server.core.uploaders.base

import com.donut.mixfile.server.core.Uploader
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

abstract class HttpUploader(name: String) : Uploader(name) {

    abstract val reqUrl: String

    abstract override val referer: String

    abstract suspend fun setReferer(value: String)


    override suspend fun genHead(client: HttpClient): ByteArray {
        return client.get {
            url(reqUrl)
        }.also {
            val customReferer = it.headers["referer"]
            if (!customReferer.isNullOrEmpty()) {
                setReferer(customReferer)
            }
        }.bodyAsBytes()
    }

    override suspend fun doUpload(fileData: ByteArray, client: HttpClient, headSize: Int): String {
        val response = client.put {
            url(reqUrl)
            setBody(fileData)
        }
        val resText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            throw Exception(resText)
        }
        return resText
    }

}