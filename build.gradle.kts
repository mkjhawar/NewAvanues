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
    // Kotlin (use catalog so only one version exists)
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    //alias(libs.plugins.kotlin.compose) apply false  // Only for Kotlin 2.0+

    // Android
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    // Dependency Injection
    alias(libs.plugins.hilt) apply false

    // Code Generation
    alias(libs.plugins.ksp) apply false

    // Database
    alias(libs.plugins.sqldelight) apply false

    // Compose Multiplatform
    alias(libs.plugins.compose) apply false

    // Documentation
    alias(libs.plugins.dokka) apply false
    // alias(libs.plugins.sentry) apply false  // Not defined in version catalog
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
