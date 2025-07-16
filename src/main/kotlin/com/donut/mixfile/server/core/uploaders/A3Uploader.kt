package com.donut.mixfile.server.core.uploaders

import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.utils.add
import com.donut.mixfile.server.core.utils.fileFormHeaders
import com.donut.mixfile.server.core.utils.getString
import com.donut.mixfile.server.core.utils.parseJsonObject
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import kotlinx.serialization.json.JsonObject

object A3Uploader : Uploader("线路A3") {

    override val referer: String
        get() = ""

    override suspend fun doUpload(fileData: ByteArray, client: HttpClient): String {
        val result =
            client.submitFormWithBinaryData(
                "https://chatbot.weixin.qq.com/weixinh5/webapp/pfnYYEumBeFN7Yb3TAxwrabYVOa4R9/cos/upload",
                formData {
                    add("media", fileData, fileFormHeaders())
                }) {
            }.body<String>().parseJsonObject<JsonObject>()

        return result.getString("url") ?: throw Error("上传失败")
    }
}
