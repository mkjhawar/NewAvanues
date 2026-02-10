/**
 * VoiceCursor Module - KMP Cursor Control System
 *
 * Cross-platform cursor control with:
 * - Head tracking (IMU-based)
 * - Dwell click (gaze-based auto-click)
 * - Gesture recognition
 * - Cursor filtering and smoothing
 *
 * Platforms: Android, Desktop (JVM)
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

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
                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Serialization
                implementation(libs.kotlinx.serialization.json)

                // DateTime (KMP-compatible time)
                implementation(libs.kotlinx.datetime)

                // Flow
                api(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific
                implementation(libs.androidx.core.ktx)
                implementation(libs.kotlinx.coroutines.android)

                // AvanueUI — for AvanueModuleAccents (theme-aware cursor colors)
                implementation(project(":Modules:AvanueUI"))
            }
        }

        val desktopMain by getting {
            dependencies {
                // Desktop/JVM specific
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.voicecursor"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
