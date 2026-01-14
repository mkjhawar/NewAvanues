plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs += listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.9.24"
                )
            }
        }
    }

    sourceSets {
        val androidMain by getting {
            dependencies {
                // MagicElements Core - base types and components
                implementation(project(":Modules:AVAMagic:AVAUI:Core"))
                implementation(project(":Modules:AVAMagic:AVAUI:Core"))

                // Jetpack Compose - Use explicit versions instead of BOM
                implementation("androidx.compose.ui:ui:1.6.8")
                implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
                implementation("androidx.compose.material3:material3:1.2.1")
                implementation("androidx.compose.material:material-icons-core:1.6.8")
                implementation("androidx.compose.material:material-icons-extended:1.6.8")

                // Compose Foundation
                implementation("androidx.compose.foundation:foundation:1.6.8")

                // Compose Runtime
                implementation("androidx.compose.runtime:runtime:1.6.8")

                // Activity Compose
                implementation("androidx.activity:activity-compose:1.8.2")

                // Image Loading (Coil for Compose)
                implementation("io.coil-kt:coil-compose:2.5.0")

                // Kotlin Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

                // Kotlin Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("androidx.compose.ui:ui-test-junit4")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avaelements.renderer.android"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}
