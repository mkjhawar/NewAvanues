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
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
        publishLibraryVariants("release")
    }

    // iOS targets - REMOVED (implicit ivy repository conflicts)
    // JVM target - REMOVED (Android-only library)

    sourceSets {
        val commonMain by getting {
            dependencies {
                // No dependencies - pure Kotlin!
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific dependencies (none needed)
            }
        }

        // iOS and JVM source sets - REMOVED (Android-only library)
    }
}

android {
    namespace = "com.augmentalis.voiceos.hash"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

publishing {
    repositories {
        maven {
            name = "Local"
            url = uri("${rootProject.projectDir}/../../../.m2/repository")
        }
    }
}
