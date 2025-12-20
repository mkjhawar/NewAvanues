/**
 * LearnAppDev - Developer Edition App Exploration Tool
 *
 * Developer edition of LearnApp with additional debugging features:
 * - Neo4j graph visualization
 * - Full exploration logs
 * - Unencrypted AVU export
 * - Element inspection tools
 * - Real-time accessibility tree viewer
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11 (LearnApp Dual-Edition)
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")  // For @Parcelize support
}

android {
    namespace = "com.augmentalis.learnappdev"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.augmentalis.learnappdev"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Developer edition build config
        buildConfigField("Boolean", "IS_DEVELOPER_EDITION", "true")
        buildConfigField("Boolean", "ENABLE_NEO4J", "true")
        buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        buildConfigField("int", "MAX_LOG_ENTRIES", "500")  // P3 Layer 6: No hardcoding (Java primitive)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        aidl = true  // Enable AIDL for JIT service binding
        compose = true  // Modern UI with Jetpack Compose
        buildConfig = true  // Enable BuildConfig generation
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"  // Compatible with Kotlin 1.9.24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    // LearnAppCore (shared business logic)
    implementation(project(":Modules:VoiceOS:libraries:LearnAppCore"))

    // JITLearning (AIDL interface for service binding)
    implementation(project(":Modules:VoiceOS:libraries:JITLearning"))

    // Database module
    implementation(project(":Modules:VoiceOS:core:database"))

    // Kotlin Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Jetpack Compose UI
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.material)

    // Neo4j Driver (for graph visualization)
    // Note: Using embedded Neo4j for Android compatibility
    implementation(libs.neo4j.java.driver)

    // JSON Processing (for debugging)
    implementation(libs.gson)

    // OkHttp (for Neo4j HTTP API fallback)
    implementation(libs.okhttp)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.bundles.testing.compose)
    debugImplementation(libs.bundles.compose.debug)
}
