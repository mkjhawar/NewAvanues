/**
 * build.gradle.kts - VoiceOS Exceptions KMP Library
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 *
 * Exception hierarchy for VoiceOS platform
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

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "exceptions"
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
                // No dependencies - pure exception classes
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

        val jvmMain by getting {
            dependsOn(commonMain)
        }
    }
}

android {
    namespace = "com.augmentalis.voiceos.exceptions"
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
                name.set("VoiceOS Exceptions")
                description.set("Exception hierarchy for VoiceOS platform")
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
