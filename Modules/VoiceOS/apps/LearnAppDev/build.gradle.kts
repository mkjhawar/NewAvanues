/**
 * LearnAppDev - Developer Edition App Exploration Tool
 *
 * Developer edition of LearnApp with additional debugging features:
 * - Neo4j graph visualization
 * - Full exploration logs
 * - Unencrypted AVU export
 * - Element inspection tools
 * - Real-time accessibility tree viewer
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11 (LearnApp Dual-Edition)
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")  // For @Parcelize support
}

android {
    namespace = "com.augmentalis.learnappdev"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.augmentalis.learnappdev"
        minSdk = 34
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Developer edition build config
        buildConfigField("Boolean", "IS_DEVELOPER_EDITION", "true")
        buildConfigField("Boolean", "ENABLE_NEO4J", "true")
        buildConfigField("Boolean", "ENABLE_LOGGING", "true")
        buildConfigField("int", "MAX_LOG_ENTRIES", "500")  // P3 Layer 6: No hardcoding (Java primitive)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    buildFeatures {
        aidl = true  // Enable AIDL for JIT service binding
        compose = true  // Modern UI with Jetpack Compose
        buildConfig = true  // Enable BuildConfig generation
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"  // Compatible with Kotlin 1.9.25
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/io.netty.versions.properties",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    // LearnAppCore (shared business logic)
    implementation(project(":Modules:VoiceOS:libraries:LearnAppCore"))

    // JITLearning (AIDL interface for service binding)
    implementation(project(":Modules:VoiceOS:libraries:JITLearning"))

    // Database module
    implementation(project(":Modules:VoiceOS:core:database"))

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Jetpack Compose UI
    implementation("androidx.activity:activity-compose:1.8.2")
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-graphics:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.material3:material3:1.2.1")

    // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("com.google.android.material:material:1.11.0")

    // Neo4j Driver (for graph visualization)
    // Note: Using embedded Neo4j for Android compatibility
    implementation("org.neo4j.driver:neo4j-java-driver:5.15.0")

    // JSON Processing (for debugging)
    implementation("com.google.code.gson:gson:2.10.1")

    // OkHttp (for Neo4j HTTP API fallback)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.6.8")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.6.8")
}
