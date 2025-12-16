plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.magiccode"
version = "1.0.0"

kotlin {
    // Android target only (VOS4 is Android-only, iOS/JVM support removed for now)
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
                // YAML parsing
                implementation("net.mamoe.yamlkt:yamlkt:0.13.0")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Semver for dependency resolution
                implementation("io.github.z4kn4fein:semver:2.0.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                // Room removed - using InMemoryPluginPersistence until SQLDelight migration
                // See: libraries/core/database/ for SQLDelight patterns when ready
                implementation("androidx.appcompat:appcompat:1.6.1")  // For AlertDialog
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")  // For Android coroutines
                implementation("androidx.security:security-crypto:1.1.0-alpha06")  // Encrypted SharedPreferences
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
                implementation("junit:junit:4.13.2")
                implementation("io.mockk:mockk:1.13.8")  // For mocking Android Keystore
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("androidx.test:core:1.5.0")  // For Android test utilities
                implementation("org.robolectric:robolectric:4.11.1")  // For unit testing Android components
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test:runner:1.5.2")
                implementation("androidx.test:rules:1.5.0")
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk-android:1.13.8")  // Android version of MockK
            }
        }
    }
}

android {
    namespace = "com.augmentalis.magiccode.plugins"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.useJUnit()
                it.testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = true
                }
            }
        }
    }
}

// Force test execution
tasks.withType<Test> {
    useJUnit()
    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
