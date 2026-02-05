/**
 * VoiceOSCore - Unified KMP Voice Control Module
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

// Note: Versions now sourced from gradle/libs.versions.toml
// kotlinx-coroutines = 1.8.1, kotlinx-serialization = 1.6.0, kotlinx-datetime = 0.5.0
// sqldelight = 2.0.1, androidx-core = 1.12.0, junit = 4.13.2

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS Targets - only compiled when explicitly requested
    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

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
                // Foundation - StateFlow utilities, ViewModels, NumberToWords
                api(project(":Modules:Foundation"))

                // AVID - Avanues Voice ID
                implementation(project(":Modules:AVID"))

                // AVUCodec - Avanues Universal Codec (for ACD parsing)
                implementation(project(":Modules:AVUCodec"))

                // SpeechRecognition - KMP unified speech module
                implementation(project(":Modules:SpeechRecognition"))

                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Atomicfu
                implementation(libs.kotlinx.atomicfu)

                // Serialization
                implementation(libs.kotlinx.serialization.json)

                // DateTime
                implementation(libs.kotlinx.datetime)

                // SQLDelight
                implementation(libs.sqldelight.coroutines.extensions)
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
                // Device Manager - for IMU, sensors, device capabilities
                implementation(project(":Modules:DeviceManager"))

                // Localization - for multi-language support
                implementation(project(":Modules:Localization"))

                // Voice Data Manager - for data management types
                //implementation(project(":Modules:VoiceDataManager"))

                // Android Core
                implementation(libs.androidx.core.ktx)
                implementation(libs.lifecycle.livedata.ktx)
                implementation(libs.lifecycle.viewmodel.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.lifecycle.viewmodel.savedstate)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.lifecycle.viewmodel.compose)

                // Compose (use .get() to resolve provider for platform())
                implementation(platform(libs.compose.bom.get()))
                implementation(libs.compose.ui.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.runtime.livedata)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.androidx.lifecycle.runtime.compose)
                // material-icons-extended removed to reduce APK size (~15MB)
                // Icons are now provided as vector drawables in VoiceOS app
                implementation(libs.compose.ui.tooling.preview)

                // SQLDelight Android Driver
                implementation(libs.sqldelight.android.driver)

                // Speech Recognition
                implementation(project(":Modules:SpeechRecognition"))

                // Vivoka SDK
                implementation(project(":vivoka:Android"))

                // NLU and LLM
                implementation(project(":Modules:AI:NLU"))
                implementation(project(":Modules:AI:LLM"))

                // AVA Core Utils
                implementation(project(":Modules:AVA:core:Utils"))

                // AVA Core Utils
                implementation(project(":Modules:Rpc"))

                implementation(project(":Modules:VoiceCursor"))
                // Unified Database - for command persistence and scraping repositories
                // (Consolidated from VoiceOS:core:database into Modules:Database)
                implementation(project(":Modules:Database"))
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.test.espresso.core)
            }
        }

        // iOS source sets
        if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
            gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
        ) {
            val iosX64Main by getting
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
            val iosMain by creating {
                dependsOn(commonMain)
                iosX64Main.dependsOn(this)
                iosArm64Main.dependsOn(this)
                iosSimulatorArm64Main.dependsOn(this)
                dependencies {
                    implementation(libs.sqldelight.native.driver)
                    // NLU (CoreML-based)
                    implementation(project(":Modules:AI:NLU"))
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
        }

        // Desktop source sets
        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
                // LLM (OllamaProvider)
                implementation(project(":Modules:AI:LLM"))
            }
        }

        val desktopTest by getting {
            dependsOn(commonTest)
        }
    }
}

android {
    namespace = "com.augmentalis.voiceoscore"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

sqldelight {
    databases {
        create("VoiceOSDatabase") {
            packageName.set("com.augmentalis.database")
        }
    }
}
