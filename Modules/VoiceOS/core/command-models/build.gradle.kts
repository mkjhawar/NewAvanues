/**
 * build.gradle.kts - VoiceOS Command Models KMP Library
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 *
 * Command data structures for VoiceOS platform
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
                // No dependencies - pure data models
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
    namespace = "com.augmentalis.voiceos.command"
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
                name.set("VoiceOS Command Models")
                description.set("Command data structures for VoiceOS platform")
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
