import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    java // 确保应用了 Java 插件
    kotlin("jvm") version "2.1.20"
    id("io.ktor.plugin") version "3.1.2"
    `maven-publish`
    `java-library`
}
val projectVersion = "1.0.2"
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
        mavenLocal() // 发布到本地仓库
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}


tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
    }
}



dependencies {
    testImplementation(kotlin("test"))
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.56")
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