/**
 * build.gradle.kts - voiceos-text-utils
 *
 * Kotlin Multiplatform library for text manipulation and sanitization.
 * Provides security-focused text utilities for all platforms.
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
            baseName = "text-utils"
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
                // No external dependencies - pure Kotlin
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
    namespace = "com.augmentalis.voiceos.text"
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
            artifactId = "text-utils"
            version = "1.0.0"
        }
    }
}