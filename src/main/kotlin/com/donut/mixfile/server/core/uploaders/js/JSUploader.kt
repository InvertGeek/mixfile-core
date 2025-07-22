package com.donut.mixfile.server.core.uploaders.js

import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.defaultClient
import io.ktor.client.*
import io.ktor.util.*

class JSUploader(name: String, val scriptCode: String) : Uploader(name) {

    private var refererValue: String = ""

    override val referer
        get() = this.refererValue


    override suspend fun doUpload(fileData: ByteArray, client: HttpClient, headSize: Int): String {
        return runScript(scriptCode, defaultClient) {
            putFunc("setReferer") {
                refererValue = it.param(0, "")
            }
            put("IMAGE_DATA", fileData.encodeBase64())
            put("HEAD_SIZE", headSize)
        }.trim()
    }

}