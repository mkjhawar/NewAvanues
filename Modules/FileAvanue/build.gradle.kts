/**
 * FileAvanue -- Cross-Platform File Manager Module
 *
 * KMP file browser with provider-based storage abstraction (local, cloud, network).
 * Uses IStorageProvider polymorphism for genuinely different storage backends.
 * Android UI uses AvanueUI theme + AVID voice identifiers.
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

    // JS target (browser)
    js(IR) {
        browser()
    }

    // iOS Targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // macOS Targets
    macosX64()
    macosArm64()

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
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":Modules:AvanueUI"))
                implementation(project(":Modules:VoiceOSCore"))
                implementation(libs.androidx.core.ktx)
                implementation(project.dependencies.platform(libs.compose.bom))
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

        // Darwin shared source set (iOS + macOS)
        val darwinMain by creating {
            dependsOn(commonMain)
            dependencies {
                // Compose runtime needed for kotlin.compose plugin on native targets
                implementation("org.jetbrains.compose.runtime:runtime:1.7.3")
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(darwinMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependsOn(darwinMain)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
        }

        val desktopMain by getting {
            dependsOn(commonMain)
            dependencies {
                // Compose runtime needed for kotlin.compose plugin on JVM target
                implementation("org.jetbrains.compose.runtime:runtime:1.7.3")
            }
        }
        val desktopTest by getting { dependsOn(commonTest) }

        val jsMain by getting {
            dependsOn(commonMain)
            dependencies {
                // Compose runtime needed for kotlin.compose plugin on JS target
                implementation("org.jetbrains.compose.runtime:runtime:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.fileavanue"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}
