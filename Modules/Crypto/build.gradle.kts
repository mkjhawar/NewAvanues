/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iOS targets with CommonCrypto cinterop
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "Crypto"
            isStatic = true
        }

        it.compilations.getByName("main") {
            cinterops {
                val commoncrypto by creating {
                    defFile("src/nativeInterop/cinterop/commoncrypto.def")
                }
            }
        }
    }

    // macOS targets with CommonCrypto cinterop
    listOf(
        macosX64(),
        macosArm64()
    ).forEach {
        it.compilations.getByName("main") {
            cinterops {
                val commoncrypto by creating {
                    defFile("src/nativeInterop/cinterop/commoncrypto.def")
                }
            }
        }
    }

    // Desktop JVM target
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // JS target (browser + Node.js)
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // Shared JVM source set for Android + Desktop
        val jvmMain by creating {
            dependsOn(commonMain)
        }

        val androidMain by getting {
            dependsOn(jvmMain)
        }

        // Shared Apple/Darwin source set — CommonCrypto-based crypto
        // All iOS + macOS targets have commoncrypto cinterop, so darwinMain
        // gets access to the generated bindings (Kotlin 1.9+ feature)
        val darwinMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.kotlinx.atomicfu)
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
            dependsOn(jvmMain)
        }

        val jsMain by getting {
            dependencies {
                // No external deps — uses built-in crypto.subtle / Node crypto
            }
        }

        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

android {
    namespace = "com.augmentalis.crypto"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
