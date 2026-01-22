// AI Server - Ktor HTTP service wrapping NLU and Embedding models
// Port: 3851

plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("io.ktor.plugin") version "2.3.7"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.augmentalis.ai"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

application {
    mainClass.set("com.augmentalis.ai.server.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("ai-server.jar")
    }
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core-jvm:2.3.7")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:2.3.7")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.7")
    implementation("io.ktor:ktor-server-cors-jvm:2.3.7")

    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // ONNX Runtime for model inference
    implementation("com.microsoft.onnxruntime:onnxruntime:1.16.3")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:2.3.7")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(17)
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "com.augmentalis.ai.server.ApplicationKt"
    }
}
