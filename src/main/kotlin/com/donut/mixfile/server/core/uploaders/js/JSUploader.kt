package com.donut.mixfile.server.core.uploaders.js

import com.dokar.quickjs.ExperimentalQuickJsApi
import com.dokar.quickjs.alias.def
import com.dokar.quickjs.alias.func
import com.dokar.quickjs.alias.prop
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.defaultClient
import io.ktor.client.*
import io.ktor.util.*

class JSUploader(name: String, val scriptCode: String) : Uploader(name) {

    private var refererValue: String = ""

    override val referer
        get() = this.refererValue


    @OptIn(ExperimentalQuickJsApi::class)
    override suspend fun doUpload(fileData: ByteArray, client: HttpClient, headSize: Int): String {
        return runScript(scriptCode, defaultClient) {
            func<Unit>("setReferer") {
                refererValue = it.first().toString()
            }
            def("MIX_DATA") {
                prop("image") {
                    getter {
                        fileData.encodeBase64()
                    }
                }
                prop("head_size") {
                    getter {
                        headSize
                    }
                }
            }
        }.trim()
    }

}