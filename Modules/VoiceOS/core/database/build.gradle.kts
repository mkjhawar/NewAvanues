/**
 * build.gradle.kts - VoiceOS KMP Database Module
 *
 * SQLDelight-based cross-platform database for Android/iOS.
 * Migrated from Room for KMP compatibility.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.1"
    kotlin("plugin.serialization") version "1.9.25"
}

kotlin {
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets - REMOVED (implicit ivy repository conflicts)

    // JVM for desktop/testing - RE-ENABLED for Phase 3 (test suite)
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }

        // iOS source sets - REMOVED (implicit ivy repository conflicts)

        // RE-ENABLED for Phase 3 - test suite execution
        val jvmMain by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }

        // Android instrumentation test dependencies (Phase 2)
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test.espresso:espresso-core:3.5.1")
                implementation("androidx.test:runner:1.5.2")
                implementation("androidx.test:rules:1.5.0")
                implementation("app.cash.sqldelight:android-driver:2.0.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.database"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9 (Pie) - Aligned with project-wide standard
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("VoiceOSDatabase") {
            packageName.set("com.augmentalis.database")
            generateAsync.set(false)
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(false)  // Disabled - new database, no migrations to verify yet
        }
    }
}

// Re-enable tests for this module (overrides root project's disable)
tasks.withType<Test> {
    enabled = true
}
