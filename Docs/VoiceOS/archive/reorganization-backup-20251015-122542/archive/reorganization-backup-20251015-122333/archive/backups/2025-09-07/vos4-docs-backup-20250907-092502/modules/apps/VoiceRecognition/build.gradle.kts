/**
 * VoiceRecognition App Module Build Configuration
 * 
 * Voice Recognition test app with VoiceCursor-style UI
 * Tests the SpeechRecognition library with ARVision theming
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.augmentalis.voicerecognition"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.augmentalis.voicerecognition"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
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
        debug {
            isMinifyEnabled = false
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
        buildConfig = true
        aidl = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"  // Compatible with Kotlin 1.9.25
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/INDEX.LIST",
                "META-INF/*.SF",
                "META-INF/*.DSA",
                "META-INF/*.RSA"
            )
        }
    }
}

dependencies {
    // Speech Recognition Library
    implementation(project(":libraries:SpeechRecognition"))
    
    // VOSK Speech Recognition (required by VoskEngine)
    implementation("com.alphacephei:vosk-android:0.3.47")
    
    // Vivoka VSDK - Local AAR files (can be included in app modules)
    implementation(files("../../vivoka/vsdk-6.0.0.aar"))
    implementation(files("../../vivoka/vsdk-csdk-asr-2.0.0.aar"))
    implementation(files("../../vivoka/vsdk-csdk-core-1.0.1.aar"))
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Android Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    
    // Compose with explicit versions (aligned with BOM 2024.06.00)
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    
    // Explicitly declare versions to avoid resolution warnings
    implementation("androidx.compose.ui:ui:1.6.8")
    implementation("androidx.compose.ui:ui-graphics:1.6.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
    implementation("androidx.compose.ui:ui-text:1.6.8")  // Explicit for ui-text
    implementation("androidx.compose.ui:ui-util:1.6.8")  // Explicit for ui-util
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("androidx.compose.runtime:runtime:1.6.8")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    
    // Permissions
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.robolectric:robolectric:4.11.1")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}