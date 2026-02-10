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
                baseName = "Chat"
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

    sourceSets {
        // Common code (shared across all platforms)
        val commonMain by getting {
            dependencies {
                implementation(project(":Modules:AVA:core:Utils"))
                implementation(project(":Modules:AVA:core:Domain"))
                implementation(project(":Modules:AI:NLU"))
                implementation(project(":Modules:AI:RAG"))
                implementation(project(":Modules:AI:LLM"))
                implementation(project(":Modules:Actions"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
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
                implementation(project(":Modules:AVA:core:Data"))
                implementation(project(":Modules:AvanueUI"))
                implementation(libs.kotlinx.coroutines.android)

                // Hilt Dependency Injection
                implementation(libs.hilt.android)
                implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

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
                implementation("junit:junit:4.13.2")
                implementation("io.mockk:mockk-android:1.13.8")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("app.cash.turbine:turbine:1.0.0")
                implementation("androidx.test:core:1.5.0")
                implementation("org.robolectric:robolectric:4.11.1")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.compose.ui:ui-test-junit4:1.5.4")
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test.espresso:espresso-core:3.5.1")
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test:runner:1.5.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("io.mockk:mockk:1.13.8")
                implementation("org.mockito:mockito-android:5.7.0")
                implementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
                implementation("app.cash.turbine:turbine:1.0.0")
                implementation("com.google.dagger:hilt-android-testing:2.48.1")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.chat"
    compileSdk = 34

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
    "kspAndroidTest"("com.google.dagger:hilt-compiler:2.48.1")
}
