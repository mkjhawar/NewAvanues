// Author: Manoj Jhawar
// Purpose: Kotlin Multiplatform DeviceManager module build configuration
// Targets: Android, iOS, JVM (Desktop)

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    // Compiler options for all targets
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // ===================
    // Target Configuration
    // ===================

    // Android Target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xskip-metadata-version-check")
        }
    }

    // JVM Desktop Target
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    // iOS Targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()


    // ===================
    // Source Sets
    // ===================

    sourceSets {
        // Common Main - Shared across all platforms
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.atomicfu)
            }
        }

        // Common Test
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // Android Main
        val androidMain by getting {
            // Include legacy Java source directory
            kotlin.srcDir("src/androidMain/main/java")

            dependencies {
                // AvanueUI theme system (required for dashboard UI)
                implementation(project(":Modules:AvanueUI"))

                // Coroutines Android
                implementation(libs.kotlinx.coroutines.android)

                // AndroidX Core
                implementation(libs.androidx.core.ktx)
                implementation("androidx.appcompat:appcompat:1.6.1")
                implementation("androidx.fragment:fragment-ktx:1.6.2")
                implementation("androidx.viewpager2:viewpager2:1.0.0")
                implementation("androidx.activity:activity-ktx:1.8.2")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
                implementation("androidx.activity:activity-compose:1.8.2")

                // Compose BOM — aligned with JB Compose 1.7.3 (Foundation 1.7.x)
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-tooling-preview")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.compose.runtime:runtime-livedata")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
                implementation("androidx.compose.foundation:foundation")
                implementation("androidx.compose.animation:animation")
                implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

                // Material Design
                implementation("com.google.android.material:material:1.11.0")

                // Window Manager for foldable support
                implementation("androidx.window:window:1.2.0")

                // UWB Support
                implementation("androidx.core.uwb:uwb:1.0.0-alpha08")

                // Biometric
                implementation("androidx.biometric:biometric:1.2.0-alpha05")

                // Google Play Services
                implementation("com.google.android.gms:play-services-nearby:19.1.0")
                implementation("com.google.android.gms:play-services-base:18.3.0")
                implementation("com.google.android.gms:play-services-location:21.0.1")
                implementation("com.google.android.gms:play-services-fitness:21.1.0")

                // Compatibility and Fallback Support
                implementation("androidx.core:core-splashscreen:1.0.1")
                implementation("androidx.multidex:multidex:2.0.1")
                implementation("androidx.annotation:annotation:1.7.1")
                implementation("androidx.collection:collection-ktx:1.4.0")
                implementation("androidx.concurrent:concurrent-futures:1.1.0")
                implementation(libs.androidx.datastore.preferences)

                // Backward Compatibility
                implementation("androidx.legacy:legacy-support-v4:1.0.0")
                implementation("androidx.vectordrawable:vectordrawable:1.1.0")
                implementation("androidx.vectordrawable:vectordrawable-animated:1.1.0")

                // Camera2 API for all versions
                implementation("androidx.camera:camera-core:1.3.1")
                implementation("androidx.camera:camera-camera2:1.3.1")
                implementation("androidx.camera:camera-lifecycle:1.3.1")
                implementation("androidx.camera:camera-view:1.3.1")

                // WorkManager for background tasks
                implementation("androidx.work:work-runtime-ktx:2.9.0")
            }
        }

        // Android Unit Test
        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlin.test)
                implementation("org.mockito:mockito-core:4.11.0")
                implementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
                implementation("androidx.arch.core:core-testing:2.2.0")
                implementation(libs.kotlinx.coroutines.test)

                // MockK for better Kotlin testing
                implementation(libs.mockk)
                implementation(libs.mockk.android)

                // Robolectric for Android unit tests
                implementation(libs.robolectric)
            }
        }

        // Android Instrumented Test
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.test.espresso.core)
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation("androidx.compose.ui:ui-test-junit4")
            }
        }

        // Desktop (JVM) Main
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation("org.jetbrains.compose.runtime:runtime:1.7.3")
            }
        }

        // Desktop (JVM) Test
        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        // iOS Intermediate Source Set
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting

        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "com.augmentalis.devicemanager"
    compileSdk = 35

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        minSdk = 28  // Must match AvanueUI dependency (minSdk 28)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        multiDexEnabled = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        dataBinding = true
        compose = true
    }
}

dependencies {
    // Debug dependencies for Compose tooling (Android-specific, outside KMP sourceSets)
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
