/*
 * VoiceIsolation - KMP Audio Preprocessing Module
 *
 * Provides noise suppression, echo cancellation, and automatic gain control
 * for voice audio before speech recognition. Toggleable for A/B testing.
 */

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs += listOf("-Xskip-metadata-version-check")
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
    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
        // iOS targets (conditional - only when explicitly building for iOS)
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach {
            it.binaries.framework {
                baseName = "VoiceIsolation"
                isStatic = true
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.datastore:datastore-preferences:1.0.0")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("io.mockk:mockk:1.13.8")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            }
        }
        if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
            gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
        ) {
            // iOS source set (conditional)
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
    namespace = "com.augmentalis.voiceisolation"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "MODULE_VERSION", "\"1.0.0\"")
        buildConfigField("String", "MODULE_NAME", "\"VoiceIsolation-KMP\"")
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

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
