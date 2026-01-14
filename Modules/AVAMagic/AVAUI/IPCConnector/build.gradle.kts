plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.avanues.avamagic.components"
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

    // TODO: Add iOS, macOS, Windows targets when platform-specific implementations are ready
    // jvm()  // Desktop support (Windows, macOS, Linux)
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin standard library
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                // ARG Scanner for service/provider discovery
                implementation(project(":Modules:AvaMagic:AvaUI:ARGScanner"))

                // Components Core for base types
                implementation(project(":Modules:AvaMagic:AvaUI:Core"))
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avanues.avamagic.components.ipc"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
