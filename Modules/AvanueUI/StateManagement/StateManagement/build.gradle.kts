plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.universal.libraries.avaelements"
version = "1.0.0"

kotlin {
    // Target platforms
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    jvm()  // Desktop support (Windows, macOS, Linux)

    // iOS targets
    val iosX64Target = iosX64()
    val iosArm64Target = iosArm64()
    val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core MagicElements dependency
                implementation(project(":Modules:AvanueUI:Core"))

                // Kotlin standard library
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android dependencies
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")

                // Jetpack Compose State
                implementation("androidx.compose.runtime:runtime:1.5.4")

                // DataStore for persistence
                implementation("androidx.datastore:datastore-preferences:1.0.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                // Desktop Compose Runtime
                implementation("org.jetbrains.compose.runtime:runtime:1.5.10")

                // Java preferences for persistence
                implementation("java.prefs:java.prefs")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // iOS-specific coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("app.cash.turbine:turbine:1.0.0")
            }
        }
    }
}

android {
    namespace = "com.avanueui.state"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Task to build documentation
tasks.register("generateDocs") {
    group = "documentation"
    description = "Generate documentation for StateManagement module"

    doLast {
        println("Documentation generation would run here")
    }
}
