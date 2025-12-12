/**
 * LearnAppCore - Shared Business Logic Library
 *
 * Provides unified element processing for both JIT and Exploration modes.
 * Zero dependencies between JIT and LearnApp - only shared core logic.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11 (JIT-LearnApp Separation)
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.augmentalis.learnappcore"
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Database module (for GeneratedCommandDTO and VoiceOSDatabaseManager)
    implementation(project(":Modules:VoiceOS:core:database"))

    // UUID Creator module (for ThirdPartyUuidGenerator)
    implementation(project(":Modules:VoiceOS:libraries:UUIDCreator"))

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Android dependencies
    implementation("androidx.core:core-ktx:1.12.0")

    // DataStore for configuration persistence
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // WorkManager for offline-first sync (Phase 7: Reliability Polish)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
