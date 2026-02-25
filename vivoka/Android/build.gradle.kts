/**
 * build.gradle.kts - Vivoka VSDK Wrapper Module
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-23
 *
 * Purpose: Wrapper module for Vivoka VSDK AAR dependencies
 * This allows VoiceOSCore (library module) to depend on Vivoka SDK
 * without violating Gradle's restriction on local AAR dependencies in libraries.
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.augmentalis.vivokasdk"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        // targetSdk removed - deprecated for libraries

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Publish all variants for KMP module compatibility
    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Vivoka VSDK
    // Primary: Maven AARs from GitLab Package Registry (includes JNI native libs)
    // Fallback: Local JARs + jniLibs/ (for offline builds, run scripts/setup-sdk.sh)
    if (file("libs/vsdk-6.0.0.jar").exists()) {
        api(files("libs/vsdk-6.0.0.jar"))
        api(files("libs/vsdk-csdk-asr-2.0.0.jar"))
        api(files("libs/vsdk-csdk-core-1.0.1.jar"))
        // Native libraries from src/main/jniLibs/ (automatically included)
    } else {
        api("com.augmentalis.sdk:vsdk:6.0.0")
        api("com.augmentalis.sdk:vsdk-csdk-asr:2.0.0")
        api("com.augmentalis.sdk:vsdk-csdk-core:1.0.1")
        // Native libraries are inside the AARs — no separate jniLibs needed
    }

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
}
