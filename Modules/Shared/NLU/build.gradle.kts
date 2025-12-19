plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

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
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "NLU"
            isStatic = true
        }
    }

    // Desktop JVM target (macOS, Windows, Linux)
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    // Web/JS target (Phase 2 - disabled for now due to task conflicts)
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    sourceSets {
        // Common code (shared across all platforms)
        val commonMain by getting {
            dependencies {
                implementation(project(":core:Utils"))
                implementation(project(":core:Domain"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        // Android-specific code
        val androidMain by getting {
            dependencies {
                implementation(project(":core:Data"))
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.onnxruntime.android)
                implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
                // Timber provided by Common module

                // ADR-013: WorkManager for background embedding computation
                implementation("androidx.work:work-runtime-ktx:2.9.0")
                implementation("androidx.hilt:hilt-work:1.1.0")
                implementation("com.google.dagger:hilt-android:2.48")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("io.mockk:mockk:1.13.8")
                implementation("org.robolectric:robolectric:4.11")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test.espresso:espresso-core:3.5.1")
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test:runner:1.5.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk-android:1.13.8")
            }
        }

        // iOS-specific code (stubs for now)
        // Note: iOS source set hierarchy is auto-configured by kotlin.mpp.applyDefaultHierarchyTemplate

        // Desktop-specific code (ONNX Runtime JVM)
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                // ONNX Runtime JVM for desktop inference
                implementation("com.microsoft.onnxruntime:onnxruntime:${libs.versions.onnxruntime.get()}")
            }
        }

        // JS-specific code (Phase 2 - disabled for now)
        // val jsMain by getting {
        //     dependencies {
        //         // TensorFlow.js for browser-based inference (future)
        //     }
        // }
    }
}

android {
    namespace = "com.augmentalis.nlu"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default NLU model: mALBERT (multilingual)
        buildConfigField("String", "DEFAULT_NLU_MODEL_TYPE", "\"MALBERT_MULTILINGUAL\"")
        buildConfigField("boolean", "ALLOW_MODEL_SWITCHING", "true")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
}
