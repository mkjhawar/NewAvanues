/**
 * LearnApp - Standalone App Exploration Tool
 *
 * Standalone Android app for manual app exploration and learning.
 * Provides launcher icon for user-initiated exploration sessions.
 * Coordinates with JIT service via AIDL to avoid duplicate captures.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11 (JIT-LearnApp Separation)
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")  // For @Parcelize support
    // Note: Room removed in favor of SQLDelight via core:database module (2025-12-18)
}

android {
    namespace = "com.augmentalis.learnapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.augmentalis.learnapp"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        aidl = true  // Enable AIDL for JIT service binding
        compose = true  // Modern UI with Jetpack Compose
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
}

dependencies {
    // LearnAppCore (shared business logic)
    implementation(project(":Modules:VoiceOS:libraries:LearnAppCore"))

    // JITLearning (AIDL interface for service binding)
    implementation(project(":Modules:VoiceOS:libraries:JITLearning"))

    // UUIDCreator (element identification and tracking)
    implementation(project(":Modules:VoiceOS:libraries:UUIDCreator"))

    // Database module (SQLDelight - replaces Room as of 2025-12-18)
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

    // Testing - Unit tests
    testImplementation(libs.bundles.testing.unit)

    // Testing - Android instrumentation tests (Phase 2 E2E)
    androidTestImplementation(libs.bundles.testing.android)
    androidTestImplementation(libs.androidx.test.uiautomator)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Compose testing (E2E UI tests)
    androidTestImplementation(libs.bundles.testing.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Activity testing for E2E flows
    androidTestImplementation(libs.androidx.test.espresso.intents)
    androidTestImplementation(libs.androidx.test.junit.ktx)

    // LeakCanary - Memory Leak Detection (Phase 7: Reliability Polish)
    debugImplementation(libs.leakcanary)
}
