import com.dokar.quickjs.ExperimentalQuickJsApi
import com.dokar.quickjs.alias.def
import com.dokar.quickjs.alias.prop
import com.dokar.quickjs.quickJs
import kotlinx.coroutines.runBlocking

@OptIn(ExperimentalUnsignedTypes::class, ExperimentalQuickJsApi::class)
fun main() {
    runBlocking {
        val result = quickJs {
            def("data") {
                prop("a") {
                    getter { "wdad".toByteArray().toUByteArray() }
                }
            }

            evaluate<Any?>(
                """
            data.a
            """.trimIndent()
            )
        }
        println(result)
//        println((result as ByteArray).toList())
    }
}