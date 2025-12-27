// Author: Manoj Jhawar
// Purpose: Unified DeviceMGR module build configuration

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    kotlin("plugin.serialization")
}

android {
    namespace = "com.augmentalis.devicemanager"
    compileSdk = 34  // Android 14 (API 34) - Latest stable for Android 15-17 preparation
    // compileSdkPreview = "VanillaIceCream"  // Android 15 - will enable when available

    defaultConfig {
        minSdk = 28  // Android 9 (Pie) - Minimum supported (aligned with project-wide standard)
        // Note: targetSdk is deprecated for libraries, using testOptions.targetSdk instead
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        // Enable multi-version APK for better compatibility
        multiDexEnabled = true
    }
    
    // testOptions {
        // targetSdk = 34  // Android 14 - Production target for testing - REMOVED: deprecated for libraries
    // }
    
    lint {
        // targetSdk = 34  // Android 14 - Production target for lint - REMOVED: deprecated for libraries
        abortOnError = false
        checkReleaseBuilds = false
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
        freeCompilerArgs += listOf("-Xskip-metadata-version-check")
    }
    
    buildFeatures {
        dataBinding = true
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}

dependencies {
    // Kotlin (let build system choose compatible version)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    
    // Material Design
    implementation("com.google.android.material:material:1.11.0")
    
    // Window Manager for foldable support
    implementation("androidx.window:window:1.2.0")
    
    // UWB Support
    implementation("androidx.core.uwb:uwb:1.0.0-alpha08")
    
    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    
    // Google Play Services
    implementation("com.google.android.gms:play-services-nearby:19.1.0")
    implementation("com.google.android.gms:play-services-base:18.3.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // Android XR Support (when available)
    // NOTE: These libraries are not yet published. Uncomment when available.
    // compileOnly("androidx.xr:xr-core:1.0.0-alpha01")
    // compileOnly("androidx.xr:xr-foundation:1.0.0-alpha01")
    // compileOnly("androidx.xr:xr-spatial:1.0.0-alpha01")
    
    // Compatibility and Fallback Support
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("androidx.collection:collection-ktx:1.4.0")
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Backward Compatibility
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.vectordrawable:vectordrawable:1.1.0")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.1.0")
    
    // Camera2 API for all versions
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    
    // Sensors and Location
    // DISABLED: core-location-altitude-1.0.0-alpha01 has broken proguard.txt (syntax error at line 19)
    // Brought in transitively by camera libraries anyway
    // implementation("androidx.core:core-location-altitude:1.0.0-alpha01")
    implementation("com.google.android.gms:play-services-fitness:21.1.0")
    
    // Media3 for advanced audio/video
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    
    // WorkManager for background tasks
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Compose BOM and libraries
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // MockK for better Kotlin testing
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")
    
    // Robolectric for Android unit tests
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
