plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt) apply false  // Only applied to Android
}

kotlin {
    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Chat"
            isStatic = true
        }
    }
    // Desktop JVM target (macOS, Windows, Linux)
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        // Common code (shared across all platforms)
        val commonMain by getting {
            dependencies {
                implementation(project(":Modules:AVA:core:Utils"))
                implementation(project(":Modules:AVA:core:Domain"))
                implementation(project(":Modules:AI:NLU"))
                implementation(project(":Modules:AI:RAG"))
                implementation(project(":Modules:AI:LLM"))
                implementation(project(":Modules:IntentActions"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
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
                implementation(project(":Modules:AvanueUI"))
                implementation(libs.kotlinx.coroutines.android)

                // Hilt Dependency Injection
                implementation(libs.hilt.android)
                implementation(libs.androidx.hilt.navigation.compose)

                // Jetpack Compose
                implementation("androidx.compose.ui:ui:1.5.4")
                implementation("androidx.compose.material3:material3:1.1.2")
                implementation("androidx.compose.material:material-icons-extended:1.5.4")
                implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")

                // ViewModel
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
                implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
                implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

                // Navigation
                implementation("androidx.navigation:navigation-compose:2.7.5")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk.android)
                implementation(libs.kotlinx.coroutines.test)
                implementation("app.cash.turbine:turbine:1.0.0")
                implementation(libs.androidx.test.core)
                implementation(libs.robolectric)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.compose.ui:ui-test-junit4:1.5.4")
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.test.espresso.core)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.mockk)
                implementation("org.mockito:mockito-android:5.7.0")
                implementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
                implementation("app.cash.turbine:turbine:1.0.0")
                implementation(libs.hilt.android.testing)
            }
        }

        // Desktop-specific code (JVM for macOS, Windows, Linux)
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                implementation(libs.kotlinx.serialization.json)
                // ONNX Runtime JVM for NLU inference (uses NLU module's implementation)
                implementation("com.microsoft.onnxruntime:onnxruntime:${libs.versions.onnxruntime.get()}")
            }
        }

        val desktopTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.chat"
    compileSdk = 35

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
}

// Apply Hilt plugin only to Android source set
apply(plugin = "com.google.dagger.hilt.android")
apply(plugin = "com.google.devtools.ksp")

dependencies {
    // Hilt compiler (Android-only)
    "kspAndroid"(libs.hilt.compiler)
    "kspAndroidTest"(libs.hilt.compiler)
}
