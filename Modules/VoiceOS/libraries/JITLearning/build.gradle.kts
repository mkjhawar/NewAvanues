/**
 * JITLearning - Just-In-Time Learning Service
 *
 * Foreground service that runs passive screen learning in VoiceOSCore process.
 * Provides AIDL interface for coordination with LearnApp standalone app.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11 (JIT-LearnApp Separation)
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")  // For @Parcelize support
}

android {
    namespace = "com.augmentalis.jitlearning"
    compileSdk = 35

    defaultConfig {
        minSdk = 34

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

    buildFeatures {
        aidl = true  // Enable AIDL support
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

    // Database module
    implementation(project(":Modules:VoiceOS:core:database"))

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")

    // Testing - Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // Testing - Android instrumentation tests (Phase 2)
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    // MockK for mocking (Phase 2 integration tests)
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("io.mockk:mockk-agent:1.13.8")
}
