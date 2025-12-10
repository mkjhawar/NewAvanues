// filename: apps/ava-standalone/build.gradle.kts
// created: 2025-11-02 15:28:00 -0800
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// AVA AI - Standalone Android Application

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)

    // Firebase - only apply if google-services.json exists
    // Check is done at configuration time using rootProject.file()
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

// Apply Firebase plugins conditionally
val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}

android {
    namespace = "com.augmentalis.ava"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.augmentalis.ava"
        minSdk = 28  // Android 9+ (Pie and above)
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-alpha01"

        testInstrumentationRunner = "com.augmentalis.ava.CustomTestRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Only support arm64-v8a (modern 64-bit ARM devices)
        // This removes x86, x86_64, and armeabi-v7a native libraries
        // Saves ~40 MB by excluding ONNX and TensorFlow Lite libs for other ABIs
        ndk {
            abiFilters.clear()
            abiFilters += "arm64-v8a"
        }
    }

    // Release signing configuration
    // Keys stored in local.properties (gitignored)
    signingConfigs {
        create("release") {
            val keystoreFile = project.findProperty("KEYSTORE_FILE") as String?
            if (keystoreFile != null && file(keystoreFile).exists()) {
                storeFile = file(keystoreFile)
                storePassword = project.findProperty("KEYSTORE_PASSWORD") as String? ?: ""
                keyAlias = project.findProperty("KEY_ALIAS") as String? ?: "ava-release"
                keyPassword = project.findProperty("KEY_PASSWORD") as String? ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release signing if configured, otherwise fall back to debug
            val releaseConfig = signingConfigs.findByName("release")
            signingConfig = if (releaseConfig?.storeFile?.exists() == true) {
                releaseConfig
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"  // Compatible with Kotlin 1.9.21
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }

        // Handle duplicate native libraries from dependencies
        jniLibs {
            pickFirsts.add("**/libc++_shared.so")
            pickFirsts.add("**/libtvm4j_runtime_packed.so")  // TVM v0.22.0 packed runtime
            pickFirsts.add("**/libllama-android.so")
            pickFirsts.add("**/libllama-jni.so")
        }
    }
}

dependencies {
    // Firebase (conditional on google-services.json)
    if (googleServicesFile.exists()) {
        implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
        implementation("com.google.firebase:firebase-crashlytics-ktx")
        implementation("com.google.firebase:firebase-analytics-ktx")
    }
    // AVA Core modules (from Modules/AVA/core/)
    implementation(project(":core:Utils"))
    implementation(project(":core:Domain"))
    implementation(project(":core:Data"))
    implementation(project(":core:Theme"))

    // AVA Feature modules (from Modules/AVA/)
    implementation(project(":Chat"))
    implementation(project(":NLU"))
    implementation(project(":Actions"))
    implementation(project(":Teach"))
    implementation(project(":Overlay"))
    implementation(project(":WakeWord"))

    // LLM module - ENABLED (TVM JAR rebuilt with Java 17 on 2025-11-07)
    // Provides on-device AI via MLC-LLM with TVM runtime
    // Includes HuggingFaceModelDownloader for model management
    implementation(project(":LLM"))

    // RAG module - Retrieval-Augmented Generation
    // Provides document ingestion, semantic search, and chat engine
    implementation(project(":RAG"))

    // Room Database (for DatabaseProvider)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("androidx.activity:activity-compose:1.8.1")

    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Compose Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // DataStore (for settings persistence)
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Splash Screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Accompanist (system UI controller)
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.32.0")

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // WorkManager + Hilt integration (ADR-013: Background embedding computation)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    ksp("androidx.hilt:hilt-compiler:1.1.0")

    // Logging (provided by Common module)
    // Timber is now provided via Common module

    // Debug tooling
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)

    // Robolectric for Android unit tests with Context
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
