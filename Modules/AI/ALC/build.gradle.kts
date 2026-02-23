plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

group = "com.augmentalis.alc"
version = "2.0.0"

// Desktop CLI executable configuration (via jvm target)
// Only register when native targets are enabled (desktop target exists)
afterEvaluate {
    if (kotlin.targets.findByName("desktop") != null) {
        tasks.register<JavaExec>("runCli") {
            group = "application"
            description = "Run the AVA Model Manager CLI"
            mainClass.set("com.augmentalis.alc.cli.ALCModelToolKt")
            classpath = kotlin.targets.getByName<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>("desktop")
                .compilations.getByName("main").runtimeDependencyFiles
        }
    }
}

kotlin {
    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    // Desktop targets (JVM-based)
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Linux native (for ARM devices, Raspberry Pi, etc.)
    //linuxX64()
    //linuxArm64()

    // Windows native
    //mingwX64()
    // macOS native
    macosX64()
    macosArm64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin standard
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Ktor for HTTP (cloud providers)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                // Ktor Android engine
                implementation(libs.ktor.client.okhttp)

                // Hilt
                implementation(libs.hilt.android)

                // LLM module (ApiKeyManager, LLMResult)
                implementation(project(":Modules:AI:LLM"))

                // TVM Runtime (local)
                implementation(files("libs/tvm4j_core.jar"))

                // Logging
                implementation("com.jakewharton.timber:timber:5.0.1")
            }
        }
        // iOS shared source set
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        // Desktop (JVM) source set
        val desktopMain by getting {
            dependencies {
                implementation(libs.ktor.client.cio)
                // ONNX Runtime for desktop inference
                implementation("com.microsoft.onnxruntime:onnxruntime:1.16.3")
                // Logging
                implementation(libs.slf4j.api)
                implementation(libs.logback.classic)
                // Coroutines JVM
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        // macOS native shared
        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependsOn(commonMain)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
        }
        // Linux native shared
//        val linuxX64Main by getting
//        val linuxArm64Main by getting
//        val linuxMain by creating {
//            dependsOn(commonMain)
//            linuxX64Main.dependsOn(this)
//            linuxArm64Main.dependsOn(this)
//        }
//
//        // Windows native
//        val mingwX64Main by getting {
//            dependsOn(commonMain)
//        }
    }
}

android {
    namespace = "com.augmentalis.alc"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Hilt KSP configuration for Android
dependencies {
    add("kspAndroid", libs.hilt.compiler)
}
