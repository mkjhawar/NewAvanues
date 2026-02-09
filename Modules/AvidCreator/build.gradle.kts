/**
 * KMP AvidCreator Module
 *
 * Cross-platform AVID element creation and management library.
 * Android extensions for AccessibilityService integration.
 *
 * Supported Platforms:
 * - Android (full implementation with UI)
 * - iOS (core models only)
 * - Desktop (core models only)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("kotlin-parcelize")
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // JVM target (Desktop)
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
        // iOS targets
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach {
            it.binaries.framework {
                baseName = "AvidCreator"
                isStatic = true
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)

                // Cross-platform AVID generation
                implementation(project(":Modules:AVID"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.androidx.core.ktx)

                // SQLDelight KMP Database
                implementation(project(":Modules:Database"))

                // AvanueUI (GlassMorphism, DepthLevel - consolidated)
                implementation(project(":Modules:AvanueUI"))

                // Compatibility Support
                implementation("androidx.multidex:multidex:2.0.1")
                implementation("androidx.annotation:annotation:1.7.1")
                implementation("androidx.datastore:datastore-preferences:1.0.0")

                // JSON Serialization
                implementation("com.google.code.gson:gson:2.10.1")

                // Compose dependencies
                implementation(platform("androidx.compose:compose-bom:2024.02.00"))
                implementation("androidx.compose.runtime:runtime")
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.foundation:foundation")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.compose.runtime:runtime-livedata")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
                implementation("androidx.activity:activity-compose:1.8.2")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation(libs.kotlin.test)
                implementation("org.mockito:mockito-core:4.11.0")
                implementation("org.mockito:mockito-inline:4.11.0")
                implementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
                implementation("androidx.arch.core:core-testing:2.2.0")
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test.espresso:espresso-core:3.5.1")
                implementation("androidx.compose.ui:ui-test-junit4")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
        if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
            gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
        ) {
            // iOS source sets
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
}

android {
    namespace = "com.augmentalis.avidcreator"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        multiDexEnabled = true
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            all {
                it.jvmArgs("-Dnet.bytebuddy.experimental=true")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}
