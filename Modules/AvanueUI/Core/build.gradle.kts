plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.avanues.avamagic.ui"
version = "1.0.0"

kotlin {
    // Target platforms
    // NOTE: Temporarily Android-only until platform-specific APIs (Math, etc.)
    // are refactored for multiplatform support
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // TODO: Re-enable after refactoring platform-specific code
    // jvm()  // Desktop support (Windows, macOS, Linux)
    // val iosX64Target = iosX64()
    // val iosArm64Target = iosArm64()
    // val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin standard library
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation(libs.kotlinx.datetime)
            }
        }

        val androidMain by getting {
            dependencies {
                // Android dependencies
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("com.google.android.material:material:1.10.0")

                // Jetpack Compose
                implementation("androidx.compose.ui:ui:1.6.8")
                implementation("androidx.compose.material3:material3:1.2.1")
                implementation("androidx.compose.foundation:foundation:1.6.8")
            }
        }

        // TODO: Re-enable when targets are re-enabled
        // val jvmMain by getting {
        //     dependencies {
        //         // Desktop Compose
        //         implementation("org.jetbrains.compose.ui:ui-desktop:1.5.10")
        //     }
        // }
        // val iosMain by creating {
        //     dependsOn(commonMain)
        // }
        // val iosX64Main by getting {
        //     dependsOn(iosMain)
        // }
        // val iosArm64Main by getting {
        //     dependsOn(iosMain)
        // }
        // val iosSimulatorArm64Main by getting {
        //     dependsOn(iosMain)
        // }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.avanueui.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
