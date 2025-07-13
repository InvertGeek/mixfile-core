import com.donut.mixfile.server.core.MixFileServer;
import com.donut.mixfile.server.core.Uploader;
import com.donut.mixfile.server.core.objects.MixShareInfo;
import com.donut.mixfile.server.core.uploaders.A2Uploader;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;

import java.util.Base64;

import static com.donut.mixfile.server.core.utils.ShareCodeKt.resolveMixShareInfo;

public class ServerTestJava {
    public static void main(String[] args) {
        MixFileServer server = new MixFileServer(8084) {

            @Override
            public int getDownloadTaskCount() {
                return 5;
            }

            @Override
            public int getUploadTaskCount() {
                return 10;
            }

            @Override
            public int getUploadRetryCount() {
                return 10;
            }

            @Override
            public void onError(Throwable error) {
                // 处理错误
            }

            @Override
            public @NotNull Uploader getUploader() {
                return A2Uploader.INSTANCE;
            }


            @Override
            public Object genDefaultImage(@NotNull Continuation<? super byte[]> $completion) {
                return Base64.getDecoder().decode("R0lGODlhAQABAIAAAP///wAAACwAAAAAAQABAAACAkQBADs=");
            }

        };

        MixShareInfo shareInfo = resolveMixShareInfo(
                "demmGp0ywJ1A29dfuKpqbCCdBe6fmd15daMSjYm8UIvTxcZMXOS8u5r4ruWjPb8U4EF2Qdw2mvr07qpIKS37SMlfQpKS9OQc1vLDlReDGGAQqDmdlqp9snNTx5xk4BdaHGkLf0CYPqFStejRC7GpiDFwBoCRyGkeGZ4CaK75hM1ff4pIGwdVawE6ItsGPOeUSnWsJuE1n2xK5HXimrHrAqzNlQUoO8YWm4JfwGEdfSl"
        );


        if (shareInfo != null) {
            System.out.println("文件信息: " + shareInfo.getFileName() + " 大小: " + shareInfo.getFileSize() + "字节");
            System.out.println("http://127.0.0.1:8084/api/download?s=" + shareInfo);
            System.out.println("访问地址: http://127.0.0.1:8084");
        }

        server.start(true);
    }
}
