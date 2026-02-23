plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.augmentalis.voiceui"
    compileSdk = 35

    defaultConfig {
        minSdk = 29  // Android 10 (Q) - Compatible with main app
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.ui.ExperimentalComposeUiApi"
        )
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose dependencies
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.animation:animation")
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    
    // GPU Acceleration
    implementation("androidx.graphics:graphics-core:1.0.0-alpha05")
    
    // Natural Language Processing - DISABLED to reduce APK size (~20MB)
    // implementation("com.google.mlkit:language-id:17.0.4")
    // implementation("com.google.mlkit:translate:17.0.1")
    // implementation("com.google.mlkit:smart-reply:17.0.2")
    
    // State Management
    implementation(libs.androidx.datastore.preferences)
    implementation("androidx.compose.runtime:runtime-livedata")
    
    // VOS4 Dependencies
    // implementation(project(":Modules:VoiceOS:managers:LocalizationManager"))  // DISABLED: Migrated to KMP Localization
    implementation(project(":Modules:AVID"))
    implementation(project(":Modules:DeviceManager")) // GPU capabilities
    implementation(project(":Modules:AvidCreator")) // AVID element management (moved from VoiceOS/libraries)

    // Performance Monitoring
    implementation("androidx.metrics:metrics-performance:1.0.0-alpha04")
    
    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}