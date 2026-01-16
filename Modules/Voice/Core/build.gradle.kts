plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

// Version constants for standalone builds
object Versions {
    const val coroutines = "1.8.0"
    const val serialization = "1.6.3"
    const val datetime = "0.5.0"
    const val androidxCore = "1.12.0"
    const val junit = "4.13.2"
    const val androidxTestJunit = "1.1.5"
    const val espresso = "3.5.1"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS Targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Desktop/JVM Target
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // AVID - Avanues Voice ID (unified identifier system)
                implementation(project(":Modules:AVID"))

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")

                // Atomicfu - Thread-safe atomic operations for KMP
                implementation(libs.kotlinx.atomicfu)

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")

                // DateTime (KMP-compatible time utilities)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.datetime}")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific dependencies
                implementation("androidx.core:core-ktx:${Versions.androidxCore}")

                // Compose dependencies for UI components
                implementation(platform("androidx.compose:compose-bom:2024.02.00"))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.compose.ui:ui-tooling-preview")

                // ==========================================================
                // Speech Recognition Library (contains all engine implementations)
                // ==========================================================
                // Provides: VivokaEngine, VoskEngine, GoogleSTT, WhisperEngine
                // Also includes: VivokaPathResolver, model management, initialization
                implementation(project(":Modules:VoiceOS:libraries:SpeechRecognition"))

                // ==========================================================
                // Vivoka SDK (via wrapper module)
                // ==========================================================
                // The VivokaSDK wrapper module contains:
                // - Extracted JAR files from AARs (in libs/)
                // - Native .so libraries (in src/main/jniLibs/)
                // This avoids Gradle's AAR-in-AAR restriction for library modules.
                // Runtime: SDK looks for models in external storage (VivokaPathResolver)
                implementation(project(":Modules:VoiceOS:libraries:VivokaSDK"))

                // VoiceOS Database (SQLDelight repositories for Android command persistence)
                implementation(project(":Modules:VoiceOS:core:database"))

                // ==========================================================
                // NLU (BERT-based intent classification)
                // ==========================================================
                // Provides: IntentClassifier, BertTokenizer, OnnxSessionManager
                // Uses ONNX Runtime for BERT inference with semantic embeddings
                implementation(project(":Modules:AI:NLU"))

                // ==========================================================
                // LLM (Local language model for natural language fallback)
                // ==========================================================
                // Provides: LocalLLMProvider, ALCEngine, model discovery
                // Loads models from external storage: /sdcard/ava-ai-models/llm/
                implementation(project(":Modules:AI:LLM"))

                // ==========================================================
                // AVA Core Utils (Result type used by NLU and LLM modules)
                // ==========================================================
                // Provides: Result sealed class for handling success/error states
                // Required because NLU and LLM use this Result type in their APIs
                implementation(project(":Modules:AVA:core:Utils"))
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:${Versions.junit}")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test.ext:junit:${Versions.androidxTestJunit}")
                implementation("androidx.test.espresso:espresso-core:${Versions.espresso}")
            }
        }

        // iOS source sets
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
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

        // Desktop source sets
        val desktopMain by getting {
            dependsOn(commonMain)
        }

        val desktopTest by getting {
            dependsOn(commonTest)
        }
    }
}

android {
    namespace = "com.augmentalis.voiceoscoreng"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}
