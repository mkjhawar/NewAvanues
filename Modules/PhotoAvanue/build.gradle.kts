/**
 * PhotoAvanue — Cross-Platform Camera + Video Module
 *
 * Standalone KMP camera with photo capture, video recording, GPS EXIF,
 * flash control, lens switching, zoom, and exposure control.
 * Self-running via PhotoAvanueScreen or embeddable via CameraPreview.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":Modules:Foundation"))
                api(project(":Modules:Logging"))

                // Compose Multiplatform (shared across Android + Desktop)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)

                // AvanueUI v5.1 — Theme + Unified Components (KMP)
                implementation(project(":Modules:AvanueUI"))

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
                // Android Core
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.activity.compose)

                // CameraX — photo capture, video recording, preview, extensions
                implementation(libs.androidx.camera.core)
                implementation(libs.androidx.camera.camera2)
                implementation(libs.androidx.camera.lifecycle)
                implementation(libs.androidx.camera.view)
                implementation(libs.androidx.camera.video)
                implementation(libs.androidx.camera.extensions)

                // EXIF — GPS metadata tagging
                implementation(libs.androidx.exifinterface)
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
    namespace = "com.augmentalis.photoavanue"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
