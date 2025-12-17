/**
 * build.gradle.kts - VoiceOS Constants KMP Library
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-16
 *
 * Pure Kotlin constants library - no platform-specific code needed
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

    // iOS targets - REMOVED (implicit ivy repository conflicts)
    // JVM target - REMOVED (Android-only library)

    // Source sets
    sourceSets {
        val commonMain by getting {
            dependencies {
                // No dependencies - pure constants
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

        // iOS and JVM source sets - REMOVED (Android-only library)
    }
}

android {
    namespace = "com.augmentalis.voiceos.constants"
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
            pom {
                name.set("VoiceOS Constants")
                description.set("Centralized constants for VoiceOS platform")
                url.set("https://github.com/augmentalis/voiceos")

                licenses {
                    license {
                        name.set("Proprietary")
                        url.set("https://augmentalis.com/license")
                    }
                }

                developers {
                    developer {
                        id.set("manoj")
                        name.set("Manoj Jhawar")
                        email.set("manoj@ideahq.net")
                    }
                }
            }
        }
    }
}
