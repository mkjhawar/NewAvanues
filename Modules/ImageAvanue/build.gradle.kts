/**
 * ImageAvanue — Cross-Platform Image Viewer Module
 *
 * Standalone KMP image viewer with pinch-to-zoom, pan, rotation, gallery navigation,
 * and filter presets. Uses Coil for Android image loading.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions { jvmTarget = "17" }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":Modules:Foundation"))
                api(project(":Modules:Logging"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":Modules:AvanueUI"))
                implementation(project(":Modules:VoiceOSCore"))
                implementation(libs.androidx.core.ktx)
                implementation(platform(libs.compose.bom.get()))
                implementation(libs.compose.ui.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.coil.compose)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
            }
        }

        val desktopMain by getting { dependsOn(commonMain) }
        val desktopTest by getting { dependsOn(commonTest) }
    }
}

android {
    namespace = "com.augmentalis.imageavanue"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
