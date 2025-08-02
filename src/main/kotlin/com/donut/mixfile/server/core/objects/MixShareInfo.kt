package com.donut.mixfile.server.core.objects

import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.aes.decryptAES
import com.donut.mixfile.server.core.aes.encryptAES
import com.donut.mixfile.server.core.utils.*
import com.donut.mixfile.server.core.utils.basen.Alphabet
import com.donut.mixfile.server.core.utils.basen.BigIntBaseN
import com.donut.mixfile.server.core.utils.extensions.mb
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MixShareInfo(
    @SerialName("f") val fileName: String,
    @SerialName("s") val fileSize: Long,
    @SerialName("h") val headSize: Int,
    @SerialName("u") val url: String,
    @SerialName("k") val key: String,
    @SerialName("r") val referer: String,
) {

    @kotlinx.serialization.Transient
    var cachedCode: String? = null

    companion object {

        val ENCODER =
            BigIntBaseN(Alphabet.fromString("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"))

        private val DEFAULT_PASSWORD = "123".hashMD5()

        fun fromString(string: String) = fromJson(dec(string))

        fun tryFromString(string: String) = try {
            fromString(string).also {
                it.cachedCode = string
            }
        } catch (e: Exception) {
            null
        }

        private fun fromJson(json: String): MixShareInfo =
            json.parseJsonObject()

        private fun enc(input: String): String {
            val bytes = input.encodeToByteArray()
            val result = encryptAES(bytes, DEFAULT_PASSWORD)
            return ENCODER.encode(result)
        }

        private fun dec(input: String): String {
            val bytes = ENCODER.decode(input)
            val result = decryptAES(bytes, DEFAULT_PASSWORD)
            return result!!.decodeToString()
        }

    }

    fun shareCode() = enc(toJson()).also { cachedCode = it }


    override fun toString(): String {
        return cachedCode ?: shareCode()
    }

    fun toDataLog(): FileDataLog {
        return FileDataLog(
            shareInfoData = this.toString(),
            name = this.fileName,
            size = this.fileSize
        )
    }


    private fun toJson(): String = this.toJsonString()

    private suspend fun doFetchFile(
        url: String,
        client: HttpClient,
        referer: String = this.referer,
        limit: Int = 20.mb
    ): ByteArray {

        val transformedUrl = Uploader.transformUrl(url)
        val transformedReferer = Uploader.transformReferer(url, referer)

        val result: ByteArray = client.prepareGet(transformedUrl) {
            if (transformedReferer.isNotEmpty()) {
                header("Referer", transformedReferer)
            }
        }.execute {
            val contentLength = it.contentLength() ?: 0
            // iv + ghash 各96位,12字节,共24字节
            val overSize = contentLength - (limit + headSize + 24)
            if (overSize > 0) {
                throw NoRetryException("分片文件过大: ${overSize} bytes")
            }
            val channel = it.bodyAsChannel()
            channel.discard(headSize.toLong())

            decryptAES(channel, ENCODER.decode(key), limit)
        }

        val hash = Url(url).fragment.trim()

        if (hash.isNotEmpty()) {
            val currentHash = result.hashMixSHA256()
            if (!currentHash.contentEquals(hash)) {
                throw NoRetryException("文件遭到篡改: ${currentHash} != ${hash}")
            }
        }

        return result
    }

    suspend fun fetchFile(
        url: String,
        server: MixFileServer,
        referer: String = this.referer,
        limit: Int = 20.mb
    ): ByteArray {
        return retry(server.downloadRetryCount, 0) {
            doFetchFile(url, server.httpClient, referer, limit)
        }
    }

    override fun hashCode(): Int {
        return url.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is MixShareInfo) {
            return url == other.url
        }
        return false
    }

    fun contentType() = fileName.parseFileMimeType()

    suspend fun fetchMixFile(server: MixFileServer, referer: String = this.referer): MixFile {
        val decryptedBytes = fetchFile(url, server, referer = referer)
        return MixFile.fromBytes(decryptedBytes)
    }

}
