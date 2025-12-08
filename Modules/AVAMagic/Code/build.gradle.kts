plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.universal.core"
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
    // jvm()

    // val iosX64Target = iosX64()
    // val iosArm64Target = iosArm64()
    // val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Common dependencies
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("net.mamoe.yamlkt:yamlkt:0.12.0")

                // MagicIdea UI dependencies
                implementation(project(":modules:AVAMagic:UI:Core"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }

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
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avanues.avacode"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
