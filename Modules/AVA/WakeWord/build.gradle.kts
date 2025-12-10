// filename: Universal/AVA/Features/WakeWord/build.gradle.kts
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// AVA AI - Wake Word Detection Feature Module

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.augmentalis.ava.features.wakeword"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Support 64-bit ARM (most modern Android devices)
        ndk {
            abiFilters += listOf("arm64-v8a")
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
    // AVA Core modules
    implementation(project(":core:Domain"))
    implementation(project(":core:Utils"))

    // AVA LLM module for ApiKeyManager (Porcupine API key)
    implementation(project(":LLM"))

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Android utilities
    implementation("androidx.core:core-ktx:1.12.0")

    // DataStore Preferences
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Lifecycle ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    // Porcupine Wake Word Engine
    // Free tier with access key: https://picovoice.ai/platform/porcupine/
    implementation("ai.picovoice:porcupine-android:3.0.2")

    // Logging (provided by Common module)
    // Timber is now provided via Common module's androidMain

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
