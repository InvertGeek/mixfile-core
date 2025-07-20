import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java // 确保应用了 Java 插件
    kotlin("jvm") version "2.2.0"
    id("io.ktor.plugin") version "3.2.2"
    kotlin("plugin.serialization") version "2.2.0"
    `maven-publish`
    `java-library`
}
val projectVersion = "1.0.6"
group = "com.donut.mixfile-core"
version = projectVersion

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.donut.mixfile.server.core.MixFileServer")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")

}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"]) // Java 项目，或 components["kotlin"] 对于 Kotlin Multiplatform
            groupId = "com.donut"
            artifactId = "mixfile-core"
            version = projectVersion
        }
    }

    repositories {
        google()
        mavenLocal() // 发布到本地仓库
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}



dependencies {
    testImplementation(kotlin("test"))
    implementation("io.github.dokar3:quickjs-kt:1.0.0-alpha13")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-logging")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-default-headers")
    implementation("io.ktor:ktor-server-status-pages")
    implementation("io.ktor:ktor-server-cors")
    implementation("io.ktor:ktor-server-content-negotiation")
}


tasks.test {
    useJUnitPlatform()
}