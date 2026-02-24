/**
 * NoteAvanue — Cross-Platform Rich Notes Module
 *
 * Voice-first rich text notes with Markdown round-trip, embedded photos,
 * documents, voice transcription, auto-save, tagging, and export.
 * Self-running via NoteAvanueScreen or embeddable via NoteEditor.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
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

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":Modules:Foundation"))
                api(project(":Modules:Logging"))
                implementation(project(":Modules:Database"))

                // Compose Multiplatform (shared across Android + Desktop)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)

                // AvanueUI v5.1 — Theme + Unified Components (KMP)
                implementation(project(":Modules:AvanueUI"))

                // Rich Text Editor — Markdown round-trip
                implementation(libs.richeditor.compose)

                // SQLDelight coroutine extensions (asFlow/mapToList for reactive queries)
                implementation(libs.sqldelight.coroutines.extensions)

                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(project(":Modules:VoiceOSCore"))

                // AI/RAG — on-device semantic search for notes
                implementation(project(":Modules:AI:RAG"))
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }

        val desktopMain by getting { dependsOn(commonMain) }
        val desktopTest by getting { dependsOn(commonTest) }
    }
}

android {
    namespace = "com.augmentalis.noteavanue"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
