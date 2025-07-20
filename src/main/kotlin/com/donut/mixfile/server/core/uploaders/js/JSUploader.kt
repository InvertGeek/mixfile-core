package com.donut.mixfile.server.core.uploaders.js

import com.dokar.quickjs.ExperimentalQuickJsApi
import com.dokar.quickjs.alias.def
import com.dokar.quickjs.alias.prop
import com.donut.mixfile.server.core.Uploader
import io.ktor.client.*
import io.ktor.util.*

class JSUploader(name: String, val scriptCode: String, private val refererValue: String) : Uploader(name) {

    override val referer
        get() = this.refererValue


    @OptIn(ExperimentalQuickJsApi::class)
    override suspend fun doUpload(fileData: ByteArray, client: HttpClient): String {
        return runScript(scriptCode) {
            def("MIX_DATA") {
                prop("image") {
                    getter {
                        fileData.encodeBase64()
                    }
                }
            }
        }.trim()
    }

}