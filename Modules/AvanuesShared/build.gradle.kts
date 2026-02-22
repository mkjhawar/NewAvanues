/**
 * AvanuesShared - KMP Umbrella Module for iOS Framework Export
 *
 * This module re-exports all shared KMP modules as a single iOS framework
 * via the kotlin-cocoapods plugin. The iOS Xcode project uses CocoaPods
 * to consume this framework.
 *
 * Modules included:
 * - VoiceOSCore: Voice command processing, element discovery, handlers
 * - Database: SQLDelight persistence (52 .sq schemas)
 * - Foundation: StateFlow utilities, ViewModels, coroutine dispatchers
 * - AVID: Avanues Voice ID system
 * - Logging: KMP logging infrastructure
 * - SpeechRecognition: Speech engine abstractions, ISpeechEngine interface
 *
 * Phase 4 (not yet included):
 * - AI/NLU: Intent classification, entity extraction (9 missing iOS actuals)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    kotlin("native.cocoapods")
}

// iOS targets only compiled when explicitly requested — matches all dependency modules' pattern
val enableIos = project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
    gradle.startParameter.taskNames.any {
        it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true)
    }

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Android target (required for Gradle to resolve transitive deps)
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets - conditional guard defined above as top-level val
    if (enableIos) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()

        cocoapods {
            summary = "Avanues Shared KMP Framework"
            homepage = "https://avanues.com"
            version = "1.0.0"
            ios.deploymentTarget = "16.0"
            name = "AvanuesShared"
            framework {
                baseName = "AvanuesShared"
                isStatic = true
                // Export all modules so Swift can access their types directly
                export(project(":Modules:VoiceOSCore"))
                export(project(":Modules:Database"))
                export(project(":Modules:Foundation"))
                export(project(":Modules:AVID"))
                export(project(":Modules:SpeechRecognition"))
                export(project(":Modules:Logging"))
                // AI/NLU deferred to Phase 4 — requires 9 missing iosMain actual declarations
                // export(project(":Modules:AI:NLU"))
            }
            // Pod dependencies (none - all native Kotlin)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Re-export all shared modules
                api(project(":Modules:VoiceOSCore"))
                api(project(":Modules:Database"))
                api(project(":Modules:Foundation"))
                api(project(":Modules:AVID"))
                api(project(":Modules:SpeechRecognition"))
                api(project(":Modules:Logging"))
                // AI/NLU deferred to Phase 4
                // api(project(":Modules:AI:NLU"))

                // Core KMP dependencies
                api(libs.kotlinx.coroutines.core)
                api(libs.kotlinx.serialization.json)
                api(libs.kotlinx.datetime)

                // Koin DI (cross-platform)
                api(libs.koin.core)

                // Ktor HTTP client
                api(libs.ktor.client.core)
                api(libs.ktor.client.content.negotiation)
                api(libs.ktor.serialization.kotlinx.json)
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
                implementation(libs.sqldelight.android.driver)
                implementation(libs.ktor.client.okhttp)
            }
        }

        if (enableIos) {
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
                    implementation(libs.ktor.client.darwin)
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
    }
}

android {
    namespace = "com.augmentalis.avanues.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
