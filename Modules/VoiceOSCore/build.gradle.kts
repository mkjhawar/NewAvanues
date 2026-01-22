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

// Version constants
object Versions {
    const val coroutines = "1.8.0"
    const val serialization = "1.6.3"
    const val datetime = "0.5.0"
    const val sqldelight = "2.0.1"
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
                // AVID - Avanues Voice ID
                implementation(project(":Modules:AVID"))

                // SpeechRecognition - KMP unified speech module
                implementation(project(":Modules:SpeechRecognition"))

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}")

                // Atomicfu
                implementation(libs.kotlinx.atomicfu)

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.serialization}")

                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:${Versions.datetime}")

                // SQLDelight
                implementation("app.cash.sqldelight:coroutines-extensions:${Versions.sqldelight}")
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
                // Android Core
                implementation("androidx.core:core-ktx:${Versions.androidxCore}")

                // Compose
                implementation(platform("androidx.compose:compose-bom:2024.06.00"))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.compose.ui:ui-tooling-preview")

                // SQLDelight Android Driver
                implementation("app.cash.sqldelight:android-driver:${Versions.sqldelight}")

                // Speech Recognition
                implementation(project(":Modules:SpeechRecognition"))

                // Vivoka SDK
                implementation(project(":vivoka:Android"))

                // NLU and LLM
                implementation(project(":Modules:AI:NLU"))
                implementation(project(":Modules:AI:LLM"))

                // AVA Core Utils
                implementation(project(":Modules:AVA:core:Utils"))

                // VoiceOS Database - for command persistence and scraping repositories
                implementation(project(":Modules:VoiceOS:core:database"))
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
                    implementation("app.cash.sqldelight:native-driver:${Versions.sqldelight}")
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
                implementation("app.cash.sqldelight:sqlite-driver:${Versions.sqldelight}")
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
