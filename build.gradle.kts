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
    kotlin("multiplatform") version "1.9.22" apply false
    kotlin("android") version "1.9.22" apply false
    kotlin("plugin.serialization") version "1.9.22" apply false

    // Android
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false

    // Dependency Injection
    id("com.google.dagger.hilt.android") version "2.48" apply false

    // Code Generation
    id("com.google.devtools.ksp") version "1.9.22-1.0.16" apply false

    // Database
    id("app.cash.sqldelight") version "2.0.1" apply false

    // Compose Multiplatform
    id("org.jetbrains.compose") version "1.6.11" apply false

    // Documentation
    id("org.jetbrains.dokka") version "1.9.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
