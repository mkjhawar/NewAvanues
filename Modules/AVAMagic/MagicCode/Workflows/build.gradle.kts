plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

group = "com.augmentalis.avanues.avamagic.avacode"
version = "1.0.0"

kotlin {
    // NOTE: Temporarily Android-only until platform-specific APIs (System.currentTimeMillis, etc.)
    // are refactored for multiplatform support
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // TODO: Re-enable after refactoring platform-specific code
    // jvm()
    // val iosX64Target = iosX64()
    // val iosArm64Target = iosArm64()
    // val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Dependency on Forms module
                implementation(project(":Modules:AVAMagic:MagicCode:Forms"))
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
    namespace = "com.augmentalis.avanues.avamagic.avacode.workflows"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
