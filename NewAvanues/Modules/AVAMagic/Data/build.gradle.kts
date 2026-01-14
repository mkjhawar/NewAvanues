plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("kotlin-parcelize")
}

group = "com.augmentalis.universal.core"
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

    // JVM target disabled - missing actual implementations for expect classes
    // TODO: Re-enable when JVM actual implementations are added
    // jvm()

    // iOS targets disabled - mockk test dependency doesn't support iOS
    // TODO: Re-enable when using iOS-compatible test framework
    // val iosX64Target = iosX64()
    // val iosArm64Target = iosArm64()
    // val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Common dependencies
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }

        // iOS source sets disabled - mockk test dependency doesn't support iOS
        // TODO: Re-enable when using iOS-compatible test framework
        // val iosMain by creating {
        //     dependsOn(commonMain)
        // }

        // val iosX64Main by getting {
        //     dependsOn(iosMain)
        // }

        // val iosArm64Main by getting {
        //     dependsOn(iosMain)
        // }

        // val iosSimulatorArm64Main by getting {
        //     dependsOn(iosMain)
        // }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk:1.13.8")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk-android:1.13.8")
                implementation("io.mockk:mockk-agent:1.13.8")
                implementation("org.robolectric:robolectric:4.11.1")
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test:runner:1.5.2")
                implementation("androidx.test.ext:junit:1.1.5")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test:runner:1.5.2")
                implementation("androidx.test:rules:1.5.0")
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test.ext:junit-ktx:1.1.5")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.voiceos.database"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildFeatures {
        aidl = true
    }

    sourceSets {
        getByName("main") {
            aidl.srcDirs("src/main/aidl")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
