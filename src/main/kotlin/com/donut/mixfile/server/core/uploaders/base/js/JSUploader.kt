package com.donut.mixfile.server.core.uploaders.base.js

import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.defaultClient
import io.ktor.client.*
import io.ktor.util.*
import org.mozilla.javascript.Scriptable

abstract class JSUploader(name: String) : Uploader(name) {

    abstract val scriptCode: String

    @Volatile
    private var refererValue: String = ""

    open val variables: Scriptable.() -> Unit = {}

    override val referer
        get() = this.refererValue


    override suspend fun doUpload(fileData: ByteArray, client: HttpClient, headSize: Int): String {
        return runScript(scriptCode, defaultClient) {
            variables()
            putFunc("setReferer") {
                refererValue = it.param(0, "").trim()
            }
            put("IMAGE_DATA", fileData.encodeBase64())
            put("HEAD_SIZE", headSize)
        }.trim()
    }

}