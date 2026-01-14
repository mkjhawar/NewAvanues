plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.universal.core.thememanager"
version = "1.0.0"

kotlin {
    // Target platforms
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // JVM and iOS targets disabled - UI:Core dependency is Android-only
    // TODO: Re-enable when UI:Core supports these platforms
    // jvm()  // Desktop support (Windows, macOS, Linux)

    // iOS targets disabled - UI:Core dependency is Android-only
    // TODO: Re-enable when UI:Core supports iOS
    // val iosX64Target = iosX64()
    // val iosArm64Target = iosArm64()
    // val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin standard library
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                // MagicElements Components Core - for Theme data classes
                implementation(project(":Modules:AvaMagic:AvaUI:Core"))
            }
        }

        val androidMain by getting {
            dependencies {
                // Android dependencies
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }

        // JVM source set disabled - UI:Core dependency is Android-only
        // TODO: Re-enable when UI:Core supports JVM
        // val jvmMain by getting {
        //     dependencies {
        //         // Desktop-specific dependencies if needed
        //     }
        // }

        // iOS source sets disabled - UI:Core dependency is Android-only
        // TODO: Re-enable when UI:Core supports iOS
        // val iosMain by creating {
        //     dependsOn(commonMain)
        // }
        // val iosX64Main by getting {
        //     dependsOn(iosMain)
        // }
        // val iosArm64Main by getting {
        //     dependsOn(iosMain)
        // }
        // val iosSimulatorArm64Main by getting {
        //     dependsOn(iosMain)
        // }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avamagic.avaui.theme"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
