/**
 * MagicVoiceHandlers - AVAMagic Voice Command Handlers
 *
 * This module contains voice command handlers for AVAMagic UI components.
 * These handlers integrate with VoiceOSCore to provide voice-first interaction
 * for all AVAMagic UI elements.
 */
plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    namespace = "com.augmentalis.avamagic.voice.handlers"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
    // VoiceOSCore for BaseHandler, HandlerResult, etc.
    implementation(project(":Modules:VoiceOSCore"))

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
