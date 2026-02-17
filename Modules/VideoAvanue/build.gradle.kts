/**
 * VideoAvanue — Cross-Platform Video Player Module
 *
 * Standalone KMP video player using AndroidX Media3 (ExoPlayer), with playback
 * controls, seek, speed, fullscreen, and subtitle rendering.
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
                implementation(libs.androidx.core.ktx)
                implementation(platform(libs.compose.bom.get()))
                implementation(libs.compose.ui.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation(libs.media3.exoplayer)
                implementation(libs.media3.ui)
                implementation(libs.media3.session)
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
    namespace = "com.augmentalis.videoavanue"
    compileSdk = 34
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
