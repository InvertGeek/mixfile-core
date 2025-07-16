package com.donut.mixfile.server.core.objects


import com.donut.mixfile.server.core.utils.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


fun ByteArray.hashMixSHA256() = MixShareInfo.ENCODER.encode(hashSHA256())

@Serializable
data class MixFile(
    @SerialName("chunk_size") val chunkSize: Int,
    @SerialName("file_size") val fileSize: Long,
    @SerialName("version") val version: Long,
    @SerialName("file_list") val fileList: List<String>,
) {

    companion object {
        fun fromBytes(data: ByteArray): MixFile =
            decompressGzip(data).parseJsonObject()
    }

    fun getFileListByStartRange(startRange: Long): List<Pair<String, Int>> {
        val startIndex = (startRange / chunkSize).toInt()
        val startOffset = (startRange % chunkSize).toInt()
        return fileList.subList(startIndex, fileList.size)
            .mapIndexed { index, file ->
                val offset = if (index == 0) startOffset else 0
                file to offset
            }
    }


    fun toBytes() = compressGzip(this.toJsonString())

}