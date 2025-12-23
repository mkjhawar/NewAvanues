/**
 * build.gradle.kts - voiceos-accessibility-types
 *
 * Kotlin Multiplatform library for VoiceOS accessibility type definitions.
 * Pure Kotlin with no platform dependencies - works on Android, iOS, JVM, JS.
 */
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("maven-publish")
}

group = "com.augmentalis.voiceos"
version = "1.0.0"

kotlin {
    // Android target
    androidTarget {
        publishLibraryVariants("release", "debug")
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // JVM target
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
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
                baseName = "accessibility-types"
            }
        }
    }

    // Source sets
    sourceSets {
        val commonMain by getting {
            dependencies {
                // No dependencies - pure type definitions
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependsOn(commonMain)
        }

        val jvmMain by getting {
            dependsOn(commonMain)
        }

        // iOS source sets - only when native targets are enabled
        if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
            gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
        ) {
            val iosMain by creating {
                dependsOn(commonMain)
            }

            val iosX64Main by getting {
                dependsOn(iosMain)
            }

            val iosArm64Main by getting {
                dependsOn(iosMain)
            }

            val iosSimulatorArm64Main by getting {
                dependsOn(iosMain)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.voiceos.accessibility"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
