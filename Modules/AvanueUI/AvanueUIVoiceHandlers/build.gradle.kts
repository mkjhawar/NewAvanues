/**
 * AvanueUIVoiceHandlers - Voice Command Handlers for AvanueUI Components (KMP)
 *
 * Voice command handlers that integrate with VoiceOSCore to provide voice-first
 * interaction for AvanueUI elements (Input, Display, Feedback, Navigation).
 *
 * Migrated to KMP: 2026-02-03
 * Renamed from MagicVoiceHandlers: 2026-02-07
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
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
    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach {
            it.binaries.framework {
                baseName = "AvanueUIVoiceHandlers"
            }
        }
    }

    // Desktop/JVM target
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
                // VoiceOSCore for BaseHandler, HandlerResult, etc.
                implementation(project(":Modules:VoiceOSCore"))

                // KMP Logging
                implementation(project(":Modules:Logging"))

                // Kotlin coroutines
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
            }
        }

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
            }
        }

        val desktopMain by getting {
            dependsOn(commonMain)
        }
    }
}

android {
    namespace = "com.avanueui.voice.handlers"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
