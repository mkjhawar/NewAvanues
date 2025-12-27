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
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    // iOS targets - only compiled when explicitly requested
    // To build iOS: ./gradlew :Modules:VoiceOS:core:accessibility-types:linkDebugFrameworkIosArm64
    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach {
            it.binaries.framework {
                baseName = "database"
                isStatic = true
            }
        }
    }
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
        // iOS targets - only compiled when explicitly requested
        // To build iOS: ./gradlew :Modules:VoiceOS:core:accessibility-types:linkDebugFrameworkIosArm64
        if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
            gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
        ) {
            val iosX64Main by getting
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
            val iosMain by creating {
                dependsOn(commonMain)
                iosX64Main.dependsOn(this)
                iosArm64Main.dependsOn(this)
                iosSimulatorArm64Main.dependsOn(this)
                dependencies {
                    implementation("app.cash.sqldelight:native-driver:2.0.1")
                }
            }
        }
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
    }
}

android {
    namespace = "com.augmentalis.database"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9 (Pie) - Aligned with project-wide standard
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
