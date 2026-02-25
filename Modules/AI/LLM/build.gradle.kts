import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "AvaLLM"
            isStatic = true
        }
    }

    // Desktop/JVM target
    jvm("desktop") {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Kotlin Serialization
                implementation(libs.kotlinx.serialization.json)

                // Ktor for HTTP client (cross-platform)
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
                // AVA Core modules (Android)
                implementation(project(":Modules:AVA:core:Domain"))
                implementation(project(":Modules:AVA:core:Utils"))
                implementation(project(":Modules:AVA:core:Data"))

                // NLU module - needed for IntentClassification
                implementation(project(":Modules:AI:NLU"))

                // Hilt Dependency Injection (Android only)
                implementation(libs.hilt.android)

                // Android Coroutines
                implementation(libs.kotlinx.coroutines.android)

                // TVM Runtime (for MLC LLM) - Android only
                implementation(files("libs/tvm4j_core.jar"))

                // Android utilities
                implementation(libs.androidx.core.ktx)

                // Security (EncryptedSharedPreferences)
                implementation(libs.androidx.security.crypto)

                // HTTP client for cloud LLM APIs (Android)
                implementation(libs.okhttp)
                implementation(libs.okhttp.logging)

                // Ktor Android engine
                implementation(libs.ktor.client.okhttp)

                // WorkManager for background downloads
                implementation(libs.androidx.work.runtime.ktx)

                // Apache Commons Compress for .ALM (tar) extraction
                implementation(libs.commons.compress)

                // LiteRT (TFLite) for Gemma 3n support
                implementation(libs.tensorflow.lite)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockk)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.test.espresso.core)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                // Ktor Darwin engine for iOS
                implementation("io.ktor:ktor-client-darwin:${libs.versions.ktor.get()}")
            }
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }

        val desktopMain by getting {
            dependencies {
                // Ktor CIO engine for desktop
                implementation("io.ktor:ktor-client-cio:${libs.versions.ktor.get()}")

                // ONNX Runtime for local inference (optional)
                implementation("com.microsoft.onnxruntime:onnxruntime:${libs.versions.onnxruntime.get()}")

                // SLF4J for logging
                implementation(libs.slf4j.api)
                implementation(libs.logback.classic)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.llm"
    compileSdk = 35

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Native library configuration
        ndk {
            // Support 64-bit ARM (most modern Android devices)
            abiFilters += listOf("arm64-v8a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // TVM v0.22.0 native libraries are managed at app level
    // Located in android/ava/src/main/jniLibs/arm64-v8a/
}
