plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
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
    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
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
                implementation(project(":Modules:AVA:core:Utils"))
                implementation(project(":Modules:AVA:core:Domain"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // Android-specific code
        val androidMain by getting {
            dependencies {
                implementation(project(":Modules:AVA:core:Data"))
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.onnxruntime.android)
                implementation(libs.tensorflow.lite.support)
                // Timber provided by Common module

                // SQLDelight Android driver
                implementation(libs.sqldelight.android.driver)

                // ADR-013: WorkManager for background embedding computation
                implementation(libs.androidx.work.runtime.ktx)
                implementation(libs.hilt.work)
                implementation(libs.hilt.android)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.robolectric)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.test.espresso.core)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockk.android)
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
                // SQLDelight JDBC driver for desktop database
                implementation(libs.sqldelight.sqlite.driver)
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

sqldelight {
    databases {
        create("SharedNluDatabase") {
            packageName.set("com.augmentalis.shared.nlu.db")
        }
    }
}
