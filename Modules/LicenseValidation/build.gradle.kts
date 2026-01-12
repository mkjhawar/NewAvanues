plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "LicenseValidation"
            isStatic = true
        }
    }

    js(IR) {
        moduleName = "license-validation"
        browser {
            commonWebpackConfig {
                cssSupport { enabled.set(true) }
            }
        }
        nodejs()
        binaries.library()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
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
                // CameraX
                implementation("androidx.camera:camera-camera2:1.3.1")
                implementation("androidx.camera:camera-lifecycle:1.3.1")
                implementation("androidx.camera:camera-view:1.3.1")
                // ML Kit
                implementation("com.google.mlkit:barcode-scanning:17.2.0")
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
            }
        }

        val desktopMain by getting {
            dependencies {
                // ZXing for QR code scanning from images
                implementation("com.google.zxing:core:3.5.2")
                implementation("com.google.zxing:javase:3.5.2")
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

        val jsMain by getting {
            dependencies {
                // QR code scanning for web
                implementation(npm("jsqr", "1.4.0"))
            }
        }
    }
}

android {
    namespace = "com.newavanues.licensing"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
