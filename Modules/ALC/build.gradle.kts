plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

group = "com.augmentalis.alc"
version = "1.0.0"

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Desktop targets (JVM-based)
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // Linux native (for ARM devices, Raspberry Pi, etc.)
    linuxX64()
    linuxArm64()

    // Windows native
    mingwX64()

    // macOS native
    macosX64()
    macosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin standard
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                // Ktor for HTTP (cloud providers)
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
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
                // Ktor Android engine
                implementation("io.ktor:ktor-client-okhttp:2.3.7")

                // Hilt
                implementation("com.google.dagger:hilt-android:2.48")

                // TVM Runtime (local)
                implementation(files("libs/tvm4j_core.jar"))
            }
        }

        // iOS shared source set
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.7")
            }
        }

        // Desktop (JVM) source set
        val desktopMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-cio:2.3.7")
                // ONNX Runtime for desktop inference
                implementation("com.microsoft.onnxruntime:onnxruntime:1.16.3")
            }
        }

        // macOS native shared
        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependsOn(commonMain)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
        }

        // Linux native shared
        val linuxX64Main by getting
        val linuxArm64Main by getting
        val linuxMain by creating {
            dependsOn(commonMain)
            linuxX64Main.dependsOn(this)
            linuxArm64Main.dependsOn(this)
        }

        // Windows native
        val mingwX64Main by getting {
            dependsOn(commonMain)
        }
    }
}

android {
    namespace = "com.augmentalis.alc"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Hilt KSP configuration for Android
dependencies {
    add("kspAndroid", "com.google.dagger:hilt-compiler:2.48")
}
