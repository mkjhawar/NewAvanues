/**
 * build.gradle.kts - VoiceOSCore Unit Tests Module
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-16
 * Updated: 2025-11-07 (Converted to Android Library for dependency compatibility)
 *
 * Android Library test module for VoiceOSCore refactoring tests.
 * Uses JUnit 5, MockK, and Robolectric for Android framework mocking.
 *
 * CoT: Converted from kotlin("jvm") to com.android.library because:
 * - Hilt dependencies are AAR (Android Archives), incompatible with JVM modules
 * - Room dependencies are AAR, incompatible with JVM modules
 * - Android test modules should be Android libraries to properly depend on Android libraries
 */

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}

android {
    namespace = "com.augmentalis.voiceoscore.tests"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    // Exclude refactoring tests from compilation
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        exclude("**/refactoring/**")
    }
}

dependencies {
    // Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.25")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // JUnit 4 (Android default - Robolectric requires JUnit 4)
    testImplementation("junit:junit:4.13.2")

    // Kotlin Test Support
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.25")

    // MockK - Kotlin mocking framework
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")

    // Robolectric - Android framework mocking
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Room Database (for database tests)
    testImplementation("androidx.room:room-runtime:2.6.1")
    testImplementation("androidx.room:room-ktx:2.6.1")
    testImplementation("androidx.room:room-testing:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // Hilt Testing
    testImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kaptTest("com.google.dagger:hilt-android-compiler:2.51.1")
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
        showCauses = true
        showExceptions = true
    }

    // Increase memory for tests
    maxHeapSize = "2g"

    // Enable parallel test execution
    maxParallelForks = Runtime.getRuntime().availableProcessors()

    // System properties for Robolectric
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.repo.url", "https://repo1.maven.org/maven2")
}

// Custom tasks for test reporting
tasks.register("testReport") {
    dependsOn("testDebugUnitTest")
    doLast {
        println("📊 Test Report:")
        println("Test results: build/reports/tests/testDebugUnitTest/index.html")
    }
}

// Quick test validation
tasks.register("quickTest") {
    dependsOn("testDebugUnitTest")
    doLast {
        println("🚀 Quick test validation completed")
    }
}
