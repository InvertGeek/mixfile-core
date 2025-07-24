package com.donut.mixfile.server.core.uploaders

import com.donut.mixfile.server.core.Uploader
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

abstract class HttpUploader : Uploader("自定义") {

    abstract var reqUrl: String

    abstract override var referer: String


    override suspend fun genHead(client: HttpClient): ByteArray {
        return client.get {
            url(reqUrl)
        }.also {
            val customReferer = it.headers["referer"]
            if (!customReferer.isNullOrEmpty()) {
                this.referer = customReferer
            }
        }.readRawBytes()
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