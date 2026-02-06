/**
 * VoiceAvanue - Unified Voice Control + Browser App
 *
 * The main unified application combining all VoiceAvanue modules:
 * - VoiceAvanue module: Unified voice commands + browser control
 * - Foundation: Shared utilities (StateFlow, ViewModel, etc.)
 * - Gaze: Eye tracking and calibration
 * - VoiceCursor: Cursor control, dwell click
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-05
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.augmentalis.voiceavanue"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.augmentalis.voiceavanue"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-alpha01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Support arm64-v8a for modern devices
        ndk {
            abiFilters.clear()
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        create("release") {
            val keystoreFile = project.findProperty("KEYSTORE_FILE") as String?
            if (keystoreFile != null && file(keystoreFile).exists()) {
                storeFile = file(keystoreFile)
                storePassword = project.findProperty("KEYSTORE_PASSWORD") as String? ?: ""
                keyAlias = project.findProperty("KEY_ALIAS") as String? ?: "ava-unified"
                keyPassword = project.findProperty("KEY_PASSWORD") as String? ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseConfig = signingConfigs.findByName("release")
            signingConfig = if (releaseConfig?.storeFile?.exists() == true) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            pickFirsts.add("**/libc++_shared.so")
        }
    }
}

dependencies {
    // =========================================================================
    // KMP Modules - Core Functionality
    // =========================================================================

    // VoiceAvanue - UNIFIED module (combines VoiceOSCore + WebAvanue + shared resources)
    implementation(project(":Modules:VoiceAvanue"))

    // Foundation - Shared utilities (StateFlow, ViewModel, etc.)
    implementation(project(":Modules:Foundation"))

    // Gaze - Eye tracking module
    implementation(project(":Modules:Gaze"))

    // VoiceOSCore - Voice commands, accessibility, screen scraping
    implementation(project(":Modules:VoiceOSCore"))

    // WebAvanue - Voice-controlled browser
    implementation(project(":Modules:WebAvanue"))

    // VoiceCursor - Eye tracking, gaze control, dwell click
    implementation(project(":Modules:VoiceCursor"))

    // AVID - Avanues Voice ID system
    implementation(project(":Modules:AVID"))

    // AVU - Universal codec for ACD parsing
    implementation(project(":Modules:AVU"))

    // SpeechRecognition - KMP speech module
    implementation(project(":Modules:SpeechRecognition"))

    // Unified Database
    implementation(project(":Modules:Database"))

    // =========================================================================
    // AVA Core (Theme, Utils)
    // =========================================================================

    implementation(project(":Modules:AVA:core:Utils"))
    implementation(project(":Modules:AvanueUI:Themes"))

    // =========================================================================
    // AI Modules (Optional - for advanced features)
    // =========================================================================

    implementation(project(":Modules:AI:NLU"))
    implementation(project(":Modules:AI:LLM"))

    // =========================================================================
    // AndroidX Core
    // =========================================================================

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.splashscreen)

    // =========================================================================
    // Jetpack Compose
    // =========================================================================

    implementation(platform(libs.compose.bom.get()))
    implementation(libs.compose.ui.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    // Compose Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // =========================================================================
    // Coroutines
    // =========================================================================

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // =========================================================================
    // Serialization
    // =========================================================================

    implementation(libs.kotlinx.serialization.json)

    // =========================================================================
    // Hilt Dependency Injection
    // =========================================================================

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // =========================================================================
    // WebView (for WebAvanue browser)
    // =========================================================================

    implementation(libs.androidx.webkit)

    // =========================================================================
    // Debug & Testing
    // =========================================================================

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.compose.bom.get()))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
