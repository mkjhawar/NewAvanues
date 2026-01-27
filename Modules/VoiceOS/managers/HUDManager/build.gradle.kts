/**
 * HUDManager build configuration
 * Author: VoiceOS Development Team
 * Purpose: Smart Glasses HUD System for VoiceOS
 * Created: 2025-01-23
 */

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.augmentalis.hudmanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        // targetSdk removed - deprecated for libraries
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    }

    buildFeatures {
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    
    // Jetpack Compose for HUD rendering
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Camera for eye tracking
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // Machine Learning for gaze detection - DISABLED to reduce APK size (~12MB)
    // implementation("com.google.mlkit:face-detection:16.1.5")
    
    // Google Common for ListenableFuture
    implementation("com.google.guava:guava:32.1.3-android")
    
    // VOS4 Dependencies - Direct integration with existing systems
    implementation(project(":Modules:DeviceManager"))       // For IMU integration
    implementation(project(":Modules:AVID"))                                  // For unique identifiers
    // implementation(project(":Modules:VoiceOS:managers:VoiceDataManager"))  // DISABLED: Depends on SQLDelight       // For data persistence
    implementation(project(":Modules:Localization"))  // KMP Localization module
    // implementation(project(":apps:SpeechRecognition"))        // For voice command data - TEMP: Has compilation errors
    // implementation(project(":Modules:VoiceOS:VoiceOSCore")) // VoiceOSCore is now application - cannot depend on it
    // implementation(project(":apps:VoiceUI"))                  // For HUD rendering - TEMP: Has compilation errors
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("io.mockk:mockk:1.13.7")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}