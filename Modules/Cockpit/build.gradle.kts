/**
 * Cockpit — Multi-Window Display & Session Management Module
 *
 * Cross-platform multi-window cockpit with freeform positioning, layout presets,
 * 6 content types (web, PDF, image, video, note, camera), voice commands,
 * and cross-device synchronization via gRPC.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-16
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Desktop/JVM Target
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Foundation — StateFlow utilities, ViewModels
                api(project(":Modules:Foundation"))

                // Logging — KMP structured logging
                api(project(":Modules:Logging"))

                // AVID — Voice identifiers for all interactive elements
                implementation(project(":Modules:AVID"))

                // Database — SQLDelight persistence (CockpitSession, CockpitFrame tables)
                implementation(project(":Modules:Database"))

                // Compose Multiplatform (shared across Android + Desktop)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)

                // AvanueUI v5.1 — Theme + Unified Components (KMP)
                implementation(project(":Modules:AvanueUI"))

                // SpeechRecognition — SpeechMetricsSnapshot for dashboard card
                implementation(project(":Modules:SpeechRecognition"))

                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Serialization — frame content state (JSON in DB)
                implementation(libs.kotlinx.serialization.json)

                // DateTime — timestamps
                implementation(libs.kotlinx.datetime)

                // SQLDelight coroutines extensions
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
                // Avanue Content Modules — standalone viewers/editors for each frame type
                implementation(project(":Modules:WebAvanue"))
                implementation(project(":Modules:PDFAvanue"))
                implementation(project(":Modules:ImageAvanue"))
                implementation(project(":Modules:VideoAvanue"))
                implementation(project(":Modules:NoteAvanue"))
                implementation(project(":Modules:PhotoAvanue"))
                implementation(project(":Modules:RemoteCast"))
                implementation(project(":Modules:AnnotationAvanue"))
                implementation(project(":Modules:FileAvanue"))

                // VoiceOSCore — Voice command integration
                implementation(project(":Modules:VoiceOSCore"))

                // DeviceManager — IMU for head cursor (spatial mode)
                implementation(project(":Modules:DeviceManager"))

                // VoiceCursor — Cursor overlay (spatial mode)
                implementation(project(":Modules:VoiceCursor"))

                // Android Core
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.androidx.activity.compose)

                // Compose UI (Android-only: tooling preview)
                implementation(libs.compose.ui.tooling.preview)

                // SQLDelight Android Driver
                implementation(libs.sqldelight.android.driver)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }

        // Desktop source sets
        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        val desktopTest by getting {
            dependsOn(commonTest)
        }

        // iOS source sets — shared across all iOS architectures
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
            }
        }
    }
}

android {
    namespace = "com.augmentalis.cockpit"
    compileSdk = 35

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
}
