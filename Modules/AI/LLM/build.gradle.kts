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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Kotlin Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                // Ktor for HTTP client (cross-platform)
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

                // TVM Runtime (for MLC LLM) - Android only
                implementation(files("libs/tvm4j_core.jar"))

                // Android utilities
                implementation("androidx.core:core-ktx:1.12.0")

                // Security (EncryptedSharedPreferences)
                implementation("androidx.security:security-crypto:1.1.0-alpha06")

                // HTTP client for cloud LLM APIs (Android)
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

                // Ktor Android engine
                implementation("io.ktor:ktor-client-okhttp:2.3.7")

                // WorkManager for background downloads
                implementation("androidx.work:work-runtime-ktx:2.9.0")

                // Apache Commons Compress for .ALM (tar) extraction
                implementation("org.apache.commons:commons-compress:1.25.0")

                // LiteRT (TFLite) for Gemma 3n support
                implementation("org.tensorflow:tensorflow-lite:2.14.0")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("junit:junit:4.13.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk:1.13.8")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test.espresso:espresso-core:3.5.1")
            }
        }

        val desktopMain by getting {
            dependencies {
                // Ktor CIO engine for desktop
                implementation("io.ktor:ktor-client-cio:2.3.7")

                // ONNX Runtime for local inference (optional)
                implementation("com.microsoft.onnxruntime:onnxruntime:1.16.3")

                // SLF4J for logging
                implementation("org.slf4j:slf4j-api:2.0.9")
                implementation("ch.qos.logback:logback-classic:1.4.14")
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.llm"
    compileSdk = 34

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
