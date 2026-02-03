/**
 * build.gradle.kts - voiceos-logging
 *
 * Kotlin Multiplatform library for VoiceOS logging infrastructure.
 * Provides PII-safe logging with platform-specific implementations.
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
    // iOS targets - only compiled when explicitly requested
    // To build iOS: ./gradlew :Modules:VoiceOS:core:accessibility-types:linkDebugFrameworkIosArm64
    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
        // iOS targets
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach {
            it.binaries.framework {
                baseName = "voiceos-logging"
            }
        }
    }
    // JVM target
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    // Source sets
    sourceSets {
        val commonMain by getting {
            dependencies {
                // No external dependencies - self-contained
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

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        // iOS targets - only compiled when explicitly requested
        // To build iOS: ./gradlew :Modules:VoiceOS:core:accessibility-types:linkDebugFrameworkIosArm64
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
        val jvmMain by getting {
            dependsOn(commonMain)
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
    }
}

android {
    namespace = "com.augmentalis.voiceos.logging"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    publications {
        publications.withType<MavenPublication> {
            groupId = "com.augmentalis.voiceos"
            artifactId = "voiceos-logging"
            version = "1.0.0"
        }
    }
}
