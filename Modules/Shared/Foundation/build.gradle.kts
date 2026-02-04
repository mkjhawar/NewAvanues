// Shared Foundation Module - Common utilities across all apps
// Contains: StateFlow utilities, ViewModels, Number conversion
// Used by: VoiceOSCore, WebAvanue, AVA

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    // Android Target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS Targets (Phase 2)
    // listOf(
    //     iosX64(),
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "SharedFoundation"
    //         isStatic = true
    //     }
    // }

    // Desktop (JVM) Target (Phase 2)
    // jvm("desktop") {
    //     compilations.all {
    //         kotlinOptions {
    //             jvmTarget = "17"
    //         }
    //     }
    // }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin Coroutines
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                // Android Coroutines
                implementation(libs.kotlinx.coroutines.android)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.foundation"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

group = "com.augmentalis.foundation"
version = "1.0.0"
