plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.universal.libraries.avaelements"
version = "1.0.0"

kotlin {
    // Target platforms
    // NOTE: Android-only until Core module supports multiplatform
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // TODO: Re-enable when Core module supports these targets
    // jvm()
    // val iosX64Target = iosX64()
    // val iosArm64Target = iosArm64()
    // val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Asset management dependencies
                implementation(project(":Modules:AvanueUI:Core"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }

        // TODO: Re-enable when Core module supports iOS
        // val iosMain by creating { dependsOn(commonMain) }
        // val iosX64Main by getting { dependsOn(iosMain) }
        // val iosArm64Main by getting { dependsOn(iosMain) }
        // val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.avanueui.assets"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
