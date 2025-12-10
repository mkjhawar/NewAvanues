plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.augmentalis.voiceos"
    compileSdk = 34  // Android 14 (API 34) - Latest stable
    // compileSdkPreview = "VanillaIceCream"  // Android 15 - will enable when available

    defaultConfig {
        applicationId = "com.augmentalis.voiceos"
        minSdk = 28  // Android 9 (Pie) - Minimum supported
        targetSdk = 34  // Android 14 - Production target
        versionCode = 1
        versionName = "3.0.0"
        
        // Enable multi-version APK for better compatibility
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
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
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += "-Xsuppress-version-warnings"
    }
    
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"  // Compatible with Kotlin 1.9.25
    }
    
}

dependencies {
    // Standalone Apps
    implementation(project(":apps:VoiceUI"))
    implementation(project(":apps:VoiceAccessibility"))
    
    // Input Method Libraries
    implementation(project(":libraries:VoiceKeyboard"))
    
    // System Managers
    implementation(project(":managers:CommandManager"))
    implementation(project(":managers:VoiceDataManager"))
    implementation(project(":managers:LocalizationManager"))
    implementation(project(":managers:LicenseManager"))
    
    // Shared Libraries
    implementation(project(":libraries:VoiceUIElements"))
    implementation(project(":libraries:DeviceManager"))
    implementation(project(":libraries:SpeechRecognition"))
    
    // Android core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    
    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.navigation:navigation-compose:2.7.6")
    
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // Dependency injection
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    
    // Android XR Support (when available)
    // NOTE: These libraries are not yet published. Uncomment when available.
    // compileOnly("androidx.xr:xr-core:1.0.0-alpha01")
    // compileOnly("androidx.xr:xr-foundation:1.0.0-alpha01")
    
    // Compatibility and Fallback Support
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Backward Compatibility
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    
    // Window Management (foldables, multi-window)
    implementation("androidx.window:window:1.2.0")
    implementation("androidx.window:window-java:1.2.0")

    // Testing - Updated versions for compatibility
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.0")
    testImplementation("org.mockito:mockito-core:5.5.0")
    testImplementation("org.mockito:mockito-android:5.5.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.1.5")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    
    // Android Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.51.1")
    kspAndroidTest("com.google.dagger:hilt-android-compiler:2.51.1")
    
    // Debug implementations for testing
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
