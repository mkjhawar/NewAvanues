/**
 * build.gradle.kts - voiceos-json-utils
 *
 * Kotlin Multiplatform library for JSON manipulation.
 * Provides basic JSON utilities without external dependencies.
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

        // iOS and JVM source sets - REMOVED (Android-only library)
    }
}

android {
    namespace = "com.augmentalis.voiceos.json"
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
            artifactId = "json-utils"
            version = "1.0.0"
        }
    }
}