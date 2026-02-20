/**
 * VoiceDataManager Module - KMP Data Management Layer
 *
 * Provides data management services wrapping the KMP Database module.
 * Android-specific UI and utilities in androidMain.
 * Replaces the archived VoiceOS/managers/VoiceDataManager.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
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

    // JVM target (Desktop) - stubbed for future use
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
                implementation(libs.kotlinx.coroutines.core)
                // KMP Database module
                implementation(project(":Modules:Database"))
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
                implementation(libs.kotlinx.coroutines.android)
                // Android Core
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                // Compose - using string notation for platform() in KMP
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.compose.runtime:runtime-livedata")
                // GSON for import/export
                implementation(libs.gson)
            }
        }

        val desktopMain by getting {
            dependencies {
                // Desktop-specific dependencies if needed
            }
        }
    }
}

android {
    namespace = "com.augmentalis.datamanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Re-enable tests for this module
tasks.withType<Test> {
    enabled = true
}
