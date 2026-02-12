/**
 * Unified KMP Database Module
 *
 * SQLDelight-based cross-platform database for all Avanues applications.
 * Consolidates VoiceOS, WebAvanue, and shared persistence into a single module.
 *
 * Supported Platforms:
 * - Android
 * - iOS (arm64, x64, simulatorArm64)
 * - JVM (Desktop)
 *
 * Key Features:
 * - AVID (Avanues Voice ID) system integration
 * - VoiceOS command and element persistence
 * - WebAvanue browser data (tabs, history, favorites)
 * - Web command persistence for whitelisted domains
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
            gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
        // iOS targets
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }
    // JVM target (Desktop)
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // SQLDelight
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)

                // Kotlin
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // AVID module for identifier generation
                implementation(project(":Modules:AVID"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                // SQLDelight Android driver
                implementation(libs.sqldelight.android.driver)

                // SQLCipher for database encryption (browser data)
                implementation(libs.sqlcipher.android)
                implementation(libs.androidx.sqlite.ktx)
                implementation(libs.androidx.sqlite.framework)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
            gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
        ) {
            // iOS source sets
            val iosX64Main by getting
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
            val iosMain by creating {
                dependsOn(commonMain)
                iosX64Main.dependsOn(this)
                iosArm64Main.dependsOn(this)
                iosSimulatorArm64Main.dependsOn(this)
                dependencies {
                    implementation(libs.sqldelight.native.driver)
                }
            }

            val iosX64Test by getting
            val iosArm64Test by getting
            val iosSimulatorArm64Test by getting
            val iosTest by creating {
                dependsOn(commonTest)
                iosX64Test.dependsOn(this)
                iosArm64Test.dependsOn(this)
                iosSimulatorArm64Test.dependsOn(this)
            }
        }
        // Desktop source sets
        val desktopMain by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.database"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
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
            dialect(libs.sqldelight.sqlite.dialect)
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(false)
        }
    }
}

// Enable tests for this module
tasks.withType<Test> {
    enabled = true
}
