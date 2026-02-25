plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.magiccode"
version = "1.0.0"

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // JVM target for desktop/server
    jvm("jvm") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        val commonMain by getting {
            dependencies {
                // VoiceOSCore - for QuantizedCommand, ActionResult, QuantizedElement types
                api(project(":Modules:VoiceOSCore"))

                // Rpc - for RPC service integration
                api(project(":Modules:Rpc"))

                // Unified Database - for repository interfaces
                api(project(":Modules:Database"))

                // AVU - for AvuDslLexer, AvuDslParser, AvuInterpreter, PluginLoader (.avp text plugin dispatch)
                implementation(project(":Modules:AVU"))

                // YAML parsing
                implementation("net.mamoe.yamlkt:yamlkt:0.13.0")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Atomic operations for KMP thread safety
                implementation(libs.kotlinx.atomicfu)

                // DateTime
                implementation(libs.kotlinx.datetime)

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

        // JVM source sets
        val jvmMain by getting {
            dependencies {
                // JVM-specific dependencies if needed
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))
            }
        }
        // iOS source sets - shared intermediate source set
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting

        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosX64Test by getting
        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting

        val iosTest by creating {
            dependsOn(commonTest)
            iosX64Test.dependsOn(this)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "com.augmentalis.magiccode.plugins"
    compileSdk = 35

    defaultConfig {
        minSdk = 29  // Match VoiceOSCore requirement
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
