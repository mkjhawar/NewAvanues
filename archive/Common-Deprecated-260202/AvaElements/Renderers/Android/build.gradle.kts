plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

group = "com.augmentalis.avaelements.renderers"
version = "2.0.0"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs += listOf(
                    "-P",
                    "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.9.25"
                )
            }
        }
    }

    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":Universal:Libraries:AvaElements:Core"))
                implementation(project(":Universal:Libraries:AvaElements:components:phase1"))
                implementation(project(":Universal:Libraries:AvaElements:components:phase3"))
                implementation(project(":Universal:Libraries:AvaElements:components:flutter-parity"))

                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.compose.ui:ui:1.5.4")
                implementation("androidx.compose.material3:material3:1.2.0")
                implementation("androidx.compose.foundation:foundation:1.5.4")
                implementation("androidx.compose.runtime:runtime:1.5.4")

                // Scrolling components
                implementation("androidx.compose.foundation:foundation:1.6.0")

                // Pager for PageView
                implementation("com.google.accompanist:accompanist-pager:0.34.0")

                // Reorderable list for ReorderableListView
                implementation("org.burnoutcrew.composereorderable:reorderable:0.9.6")

                // Coil for async image loading (FadeInImage, CircleAvatar, Icons)
                implementation("io.coil-kt:coil-compose:2.5.0")
                implementation("io.coil-kt:coil-svg:2.5.0")

                // SwipeRefresh for RefreshIndicator
                implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")

                // ZXing for QR code generation
                implementation("com.google.zxing:core:3.5.2")

                // Vico for chart visualization (Compose-native)
                implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")
                implementation("com.patrykandpatrick.vico:core:1.13.1")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avaelements.renderers.android"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
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
