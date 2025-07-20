package com.donut.mixfile.server.core.uploaders


import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.sCode
import com.donut.mixfile.server.core.utils.add
import com.donut.mixfile.server.core.utils.fileFormHeaders
import com.donut.mixfile.server.core.utils.getString
import com.donut.mixfile.server.core.utils.parseJsonObject
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.forms.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject

object A1Uploader : Uploader("线路A1") {

    override val referer: String
        get() = "wf︆︈︇︄︇︄︇︀︇︃︃︊︂️︂️︆︋︆︆︂︎︆︆︆︌︆︁︇︃︆︈︂︎︆︃︆︎︂️ey".sCode


    override suspend fun doUpload(fileData: ByteArray, client: HttpClient): String {
        val result =
            client.submitFormWithBinaryData(
                "${referer}service/upload",
                formData {
                    add("flag", "")
                    add("FileUploadForm[file]", fileData, fileFormHeaders())
                }).body<String>().parseJsonObject<JsonArray>()
        if (result.isEmpty()) {
            throw Exception("上传失败")
        }
        val data = result[0].jsonObject
        val url = data.getString("url") ?: throw Exception("上传失败")
        return "https:${url}"
    }


}