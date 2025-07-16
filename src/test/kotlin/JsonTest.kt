import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.system.measureNanoTime

@Serializable
data class Address(val city: String, val zip: String)

@Serializable
data class Person(val name: String, val age: Int, val address: Address)

fun generateTestData(): Person {
    return Person("Alice", 30, Address("Shanghai", "200000"))
}

fun main() {
    val data = generateTestData()
    val iterations = 100_000

    // Kotlin Serialization
    val json = Json { prettyPrint = false }

    val kotlinSerTime = measureNanoTime {
        repeat(iterations) {
            val jsonStr = json.encodeToString(data)
            json.decodeFromString<Person>(jsonStr)
        }
    }


    println("Kotlin Serialization Time: ${kotlinSerTime / 1_000_000} ms")

}
