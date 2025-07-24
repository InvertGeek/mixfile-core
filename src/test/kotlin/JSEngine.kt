import com.donut.mixfile.server.core.uploaders.base.js.runScript


fun main() {

    val code = """
    
       let a = {bb: 1}
       let {bb} = a
     
   
       print(bb,"foo")
       bb
    """.trimIndent()
    println(runScript(code))
}