plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.augmentalis.voicekeyboard"
    compileSdk = 35  // Match project standard

    defaultConfig {
        minSdk = 28  // Android 9 (Pie) - Project minimum
        // Note: targetSdk and versionCode/versionName removed for library modules

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Version info for library
        buildConfigField("String", "MODULE_VERSION", "\"1.0.0\"")
        buildConfigField("String", "MODULE_NAME", "\"VoiceKeyboard\"")
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
        buildConfig = true
        compose = true
        viewBinding = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

dependencies {
    // Android Core - Aligned with project standards
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    
    // Annotations - Project standard
    implementation("androidx.annotation:annotation:1.7.1")
    
    // Material Design
    implementation("com.google.android.material:material:1.12.0")
    
    // Compose - Using project's forced BOM version
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // VOS4 Modules
    implementation(project(":libraries:SpeechRecognition"))
    implementation(project(":libraries:VoiceUIElements"))
    implementation(project(":managers:VoiceDataManager"))
    implementation(project(":managers:LocalizationManager"))
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}