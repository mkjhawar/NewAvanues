/**
 * PDFAvanue — Cross-Platform PDF Viewer Module
 *
 * Standalone KMP PDF renderer using Android PdfRenderer, iOS PDFKit (future),
 * and Desktop PDFBox (future). Provides Composable PdfViewer with zoom, pan,
 * page navigation, text selection, and search.
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
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation(libs.compose.ui.ui)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
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
    namespace = "com.augmentalis.pdfavanue"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
