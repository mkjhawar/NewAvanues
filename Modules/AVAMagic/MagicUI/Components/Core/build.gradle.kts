plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.universal.libraries.avaelements"
version = "1.0.0"

kotlin {
    // Target platforms
    // NOTE: Temporarily Android-only to match UI/Core module
    // Will be expanded to multiplatform after UI/Core is refactored
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // TODO: Re-enable after UI/Core becomes multiplatform
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

                // NOTE: No dependency on UI:Core to avoid circular dependency
                // UI:Core depends on Components:Core for base types
                // DSL and YAML parsers that need UI components will be moved to separate module
            }
        }

        val androidMain by getting {
            dependencies {
                // Android dependencies
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("com.google.android.material:material:1.10.0")

                // Jetpack Compose
                implementation("androidx.compose.ui:ui:1.5.4")
                implementation("androidx.compose.material3:material3:1.2.0")
                implementation("androidx.compose.foundation:foundation:1.5.4")
            }
        }

        // TODO: Re-enable JVM and iOS source sets after UI/Core becomes multiplatform
        // val jvmMain by getting {
        //     dependencies {
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
    namespace = "com.augmentalis.avaelements.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
