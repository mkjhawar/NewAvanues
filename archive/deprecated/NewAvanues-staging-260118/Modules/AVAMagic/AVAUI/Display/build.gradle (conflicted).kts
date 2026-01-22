plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.avamagic.avaui.components"
version = "1.0.0"

kotlin {
    // Android-only to match Components:Core module
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // TODO: Re-enable after Components:Core becomes multiplatform
    // jvm()
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":Modules:AVAMagic:AVAUI:Core"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.compose.material3:material3:1.2.0")
                implementation("androidx.compose.ui:ui:1.5.4")
                implementation("androidx.compose.foundation:foundation:1.5.4")
            }
        }

        // TODO: Re-enable after Components:Core becomes multiplatform
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
    namespace = "com.augmentalis.avamagic.avaui.components.display"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
