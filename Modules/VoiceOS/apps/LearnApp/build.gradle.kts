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
}

android {
    namespace = "com.augmentalis.learnapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.augmentalis.learnapp"
        minSdk = 34
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

    // Database module
    implementation(project(":Modules:VoiceOS:core:database"))

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Jetpack Compose UI
    implementation("androidx.activity:activity-compose:1.8.2")
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-graphics:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("com.google.android.material:material:1.11.0")

    // Testing - Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Testing - Android instrumentation tests (Phase 2 E2E)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Compose testing (E2E UI tests)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")

    // Activity testing for E2E flows
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")

    // LeakCanary - Memory Leak Detection (Phase 7: Reliability Polish)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
