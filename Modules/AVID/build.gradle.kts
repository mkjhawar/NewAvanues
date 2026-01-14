/**
 * KMP AVID (Avanues Voice ID) Module
 *
 * Cross-platform AVID generation library - unified identifier system
 * for the Avanues ecosystem.
 *
 * Supported Platforms:
 * - Android
 * - iOS (arm64, x64, simulatorArm64)
 * - JVM (Desktop)
 * - JS (Web)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // JVM target (Desktop)
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets (required for VoiceOSCoreNG dependency)
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // JS target (Web) - conditionally compiled
    if (project.findProperty("kotlin.mpp.enableJsTarget") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("js", ignoreCase = true) }
    ) {
        js(IR) {
            browser()
            nodejs()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
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
                // Android-specific dependencies if needed
            }
        }

        val desktopMain by getting {
            dependencies {
                // Desktop-specific dependencies if needed
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
    }
}

android {
    namespace = "com.augmentalis.avid"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
