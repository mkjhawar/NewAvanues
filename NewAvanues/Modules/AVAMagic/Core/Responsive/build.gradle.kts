plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.compose.ui:ui:1.6.0")
                implementation("androidx.compose.foundation:foundation:1.6.0")
                implementation("androidx.compose.runtime:runtime:1.6.0")

                // Use existing DeviceManager from VoiceOS
                implementation(project(":Modules:VoiceOS:libraries:DeviceManager"))
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avamagic.responsive"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}
