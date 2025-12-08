/**
 * Shared NLU Module - Unified Intent Classification
 *
 * Cross-platform NLU module using AVU format for intent definitions.
 * Provides hybrid classification (pattern + semantic) for VoiceOS and AVA.
 *
 * Created: 2025-12-07
 */

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.2"
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets for future KMP expansion
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
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
                implementation("app.cash.sqldelight:android-driver:2.0.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.shared.nlu"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("SharedNluDatabase") {
            packageName.set("com.augmentalis.shared.nlu.db")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}
