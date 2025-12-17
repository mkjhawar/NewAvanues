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

    // iOS targets - REMOVED (implicit ivy repository conflicts)
    // JVM target - REMOVED (Android-only library)

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

        // iOS and JVM source sets - REMOVED (Android-only library)
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
