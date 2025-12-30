plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

group = "com.augmentalis.avanues.avamagic.avacode"
version = "1.0.0"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // jvm() // TODO: Re-enable for multiplatform

    // val iosX64Target = iosX64() // TODO: Re-enable for multiplatform
    // val iosArm64Target = iosArm64() // TODO: Re-enable for multiplatform
    // val iosSimulatorArm64Target = iosSimulatorArm64() // TODO: Re-enable for multiplatform

    sourceSets {
        val commonMain by getting {
            dependencies {
                // No external dependencies - pure Kotlin
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avanues.avamagic.avacode.forms"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
