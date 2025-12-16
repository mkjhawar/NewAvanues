/*
 * NewAvanues-VoiceOS - Root Build Configuration
 *
 * Defines common plugins and configurations for all subprojects.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

buildscript {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    // Kotlin
    kotlin("multiplatform") version "1.9.25" apply false
    kotlin("android") version "1.9.25" apply false
    kotlin("plugin.serialization") version "1.9.25" apply false

    // Android
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false

    // Dependency Injection
    id("com.google.dagger.hilt.android") version "2.48" apply false

    // Code Generation
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false

    // Database
    id("app.cash.sqldelight") version "2.0.1" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
