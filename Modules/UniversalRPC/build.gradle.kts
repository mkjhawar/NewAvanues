/*
 * UniversalRPC - Cross-Platform gRPC Module
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Root module for gRPC-based IPC across all platforms.
 * Replaces AIDL with cross-platform gRPC using Wire (Square).
 */

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    // Wire plugin disabled - proto files already generated in src/commonMain/kotlin
    // Re-enable when KotlinPoet compatibility issue is resolved
    // id("com.squareup.wire") version "5.1.0"
}

group = "com.augmentalis.universalrpc"
version = "1.0.0"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop") {
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
    ).forEach {
        it.binaries.framework {
            baseName = "UniversalRPC"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.wire.runtime)
                implementation(libs.wire.grpc.client)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.grpc.okhttp)
                implementation(libs.grpc.kotlin.stub)
                implementation(libs.grpc.protobuf.lite)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.grpc.netty.shaded)
                implementation(libs.grpc.kotlin.stub)
                implementation(libs.grpc.protobuf)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }
    }
}

android {
    namespace = "com.augmentalis.universalrpc"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Wire configuration disabled - proto files already generated
// Re-enable when KotlinPoet compatibility issue is resolved
// wire {
//     kotlin {
//         out = "src/commonMain/kotlin"
//         rpcRole = "client"
//         rpcCallStyle = "suspending"
//         singleMethodServices = false
//     }
//     sourcePath {
//         srcDir("Common/proto")
//     }
// }
