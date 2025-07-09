import com.donut.mixfile.server.core.MixFileServer
import com.donut.mixfile.server.core.Uploader
import com.donut.mixfile.server.core.uploaders.A2Uploader
import com.donut.mixfile.server.core.utils.resolveMixShareInfo
import java.io.InputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun main() {
    val server = object : MixFileServer(
        serverPort = 8084,
    ) {
        override val downloadTaskCount: Int
            get() = 5

        override val uploadTaskCount: Int
            get() = 10

        override val uploadRetryCount
            get() = 10

        override fun onError(error: Throwable) {

        }

        override fun getUploader(): Uploader {
            return A2Uploader
        }

        override suspend fun getStaticFile(path: String): InputStream? {
            return null
        }

        override suspend fun genDefaultImage(): ByteArray {
            //固定返回1像素gif,最小尺寸
            return Base64.decode("R0lGODlhAQABAIAAAP///wAAACwAAAAAAQABAAACAkQBADs=")
        }

        override suspend fun getFileHistory(): String {
            return ""
        }

    }
    val shareInfo = resolveMixShareInfo("demmGp0ywJ1A29dfuKpqbCCdBe6fmd15daMSjYm8UIvTxcZMXOS8u5r4ruWjPb8U4EF2Qdw2mvr07qpIKS37SMlfQpKS9OQc1vLDlReDGGAQqDmdlqp9snNTx5xk4BdaHGkLf0CYPqFStejRC7GpiDFwBoCRyGkeGZ4CaK75hM1ff4pIGwdVawE6ItsGPOeUSnWsJuE1n2xK5HXimrHrAqzNlQUoO8YWm4JfwGEdfSl")
    shareInfo!!
    println("文件信息: ${shareInfo.fileName} 大小: ${shareInfo.fileSize}字节")
    println("http://127.0.0.1:8084/api/download?s=demmGp0ywJ1A29dfuKpqbCCdBe6fmd15daMSjYm8UIvTxcZMXOS8u5r4ruWjPb8U4EF2Qdw2mvr07qpIKS37SMlfQpKS9OQc1vLDlReDGGAQqDmdlqp9snNTx5xk4BdaHGkLf0CYPqFStejRC7GpiDFwBoCRyGkeGZ4CaK75hM1ff4pIGwdVawE6ItsGPOeUSnWsJuE1n2xK5HXimrHrAqzNlQUoO8YWm4JfwGEdfSl")
    server.start(wait = true)
}