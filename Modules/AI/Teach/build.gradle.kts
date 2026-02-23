plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Note: Hilt plugin removed - annotation processing happens in consuming Android app module

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Desktop JVM target (macOS, Windows, Linux)
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                // Core modules - KMP compatible
                implementation(project(":Modules:AVA:core:Domain"))
                implementation(project(":Modules:AVA:core:Utils"))

                // Kotlin Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Kotlinx Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                // Kotlinx DateTime (KMP-safe alternative to System.currentTimeMillis)
                implementation(libs.kotlinx.datetime)
            }
        }

        androidMain {
            dependencies {
                // AvanueUI theming
                implementation(project(":Modules:AvanueUI"))

                // Hilt Dependency Injection
                implementation(libs.hilt.android)

                // Compose
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation("androidx.compose.runtime:runtime")
                implementation("androidx.compose.foundation:foundation")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.ui:ui")

                // Android-specific Compose dependencies
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
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
                implementation("io.mockk:mockk:1.13.8")
                implementation("junit:junit:4.13.2")
            }
        }

        // Desktop JVM source set
        val desktopMain by getting {
            dependencies {
                // Coroutines for Swing/desktop
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.teach"
    compileSdk = 35

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

// Note: Hilt KSP processor needs to be applied in the Android app module
// that consumes this library, not in the KMP library itself.
// See: https://dagger.dev/hilt/gradle-setup.html#kmp
