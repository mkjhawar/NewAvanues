// filename: Universal/AVA/Core/Theme/build.gradle.kts
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "AvaTheme"
            isStatic = true
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Compose libraries
                implementation(platform("androidx.compose:compose-bom:2024.06.00"))
                implementation("androidx.compose.runtime:runtime")
                implementation("androidx.compose.foundation:foundation")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.ui:ui")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android-specific dependencies if needed
            }
        }
    }
}

android {
    namespace = "com.augmentalis.ava.core.theme"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
