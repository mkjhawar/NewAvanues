/**
 * VoiceAvanue - Unified Voice Control + Browser Module
 *
 * Combines VoiceOSCore and WebAvanue functionality into a single KMP module.
 * Shared resources, common interfaces, unified command system.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-05
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.dokka)
}

kotlin {
    // Android Target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iOS Targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Desktop/JVM Target
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        // ============================================================
        // Common Main - 95% shared code across all platforms
        // ============================================================
        val commonMain by getting {
            dependencies {
                // Shared Foundation - StateFlow utilities, ViewModels, NumberToWords
                api(project(":Modules:Foundation"))

                // AVID - Avanues Voice ID unified identifier system
                implementation(project(":Modules:AVID"))

                // AVU - Avanues Universal Codec (for ACD parsing)
                implementation(project(":Modules:AVU"))

                // Database - Unified KMP database
                implementation(project(":Modules:Database"))

                // Logging - KMP logging infrastructure
                implementation(project(":Modules:Logging"))

                // VoiceCursor - KMP cursor control
                implementation(project(":Modules:VoiceCursor"))

                // SpeechRecognition - KMP unified speech module
                implementation(project(":Modules:SpeechRecognition"))

                // RPC - Cross-platform communication
                implementation(project(":Modules:Rpc"))

                // Kotlin
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.atomicfu)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Napier logging
                implementation(libs.napier)

                // SQLDelight
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)

                // Voyager - KMP Navigation
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenmodel)
                implementation(libs.voyager.tab.navigator)
                implementation(libs.voyager.transitions)

                // Koin for Dependency Injection
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // ============================================================
        // Android Main
        // ============================================================
        val androidMain by getting {
            dependencies {
                // Device Manager - for IMU, sensors, device capabilities
                implementation(project(":Modules:DeviceManager"))

                // Localization - for multi-language support
                implementation(project(":Modules:Localization"))

                // AI Modules
                implementation(project(":Modules:AI:NLU"))
                implementation(project(":Modules:AI:LLM"))

                // AVA Core Utils
                implementation(project(":Modules:AVA:core:Utils"))

                // Android Core
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.androidx.lifecycle.runtime.compose)
                implementation(libs.lifecycle.viewmodel.ktx)
                implementation(libs.lifecycle.livedata.ktx)
                implementation(libs.lifecycle.viewmodel.savedstate)
                implementation(libs.androidx.activity.compose)

                // Compose
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation(libs.compose.ui.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.runtime.livedata)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.compose.ui.tooling.preview)

                // WebView
                implementation(libs.androidx.webkit)

                // SQLDelight Android Driver
                implementation(libs.sqldelight.android.driver)

                // SQLCipher for encryption
                implementation(libs.sqlcipher.android)
                implementation(libs.androidx.sqlite.ktx)
                implementation(libs.androidx.sqlite.framework)

                // Vivoka SDK
                implementation(project(":vivoka:Android"))

                // DocumentFile for custom download paths
                implementation("androidx.documentfile:documentfile:1.0.1")

                // Security - EncryptedSharedPreferences
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlin.test.junit)
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.compose.ui.test.manifest)
                implementation(libs.sqldelight.android.driver)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.test.espresso.core)
            }
        }

        // ============================================================
        // iOS source sets
        // ============================================================
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

        // ============================================================
        // Desktop source sets
        // ============================================================
        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
                implementation(project(":Modules:AI:LLM"))
                implementation(compose.desktop.currentOs)
            }
        }

        val desktopTest by getting {
            dependsOn(commonTest)
        }
    }
}

android {
    namespace = "com.augmentalis.voiceavanue"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
}

sqldelight {
    databases {
        create("VoiceAvanueDatabase") {
            packageName.set("com.augmentalis.voiceavanue.data.db")
            dialect(libs.sqldelight.sqlite.dialect)
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(false)
        }
    }
}

// Dokka v2 Configuration
dokka {
    moduleName.set("VoiceAvanue")
    moduleVersion.set("1.0.0-alpha")
}

group = "com.augmentalis.voiceavanue"
version = "1.0.0-alpha"
