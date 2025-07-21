import com.donut.mixfile.server.core.uploaders.js.JSFunction
import com.donut.mixfile.server.core.uploaders.js.put
import org.mozilla.javascript.Context

fun runScript(code: String): Any? {
    val context = Context.enter()
    context.optimizationLevel = -1
    context.languageVersion = Context.VERSION_ES6
    try {
        val scope = context.initStandardObjects()
        scope.put("print", JSFunction {
            println(it.joinToString(" "))
        })

        val result = context.evaluateString(scope, code, "script", 1, null)
        return result
    } finally {
        Context.exit()
    }
}


fun main() {
    val code = """
       let a = {bb: 1}
       let {bb} = a
     
       print(bb,"foo")
       bb
    """.trimIndent()
    println(runScript(code))
}