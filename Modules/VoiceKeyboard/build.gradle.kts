plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.augmentalis.voicekeyboard"
    compileSdk = 35  // Match project standard

    defaultConfig {
        minSdk = 29  // Android 10 (Q) - Project minimum
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
    
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
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
    implementation(platform(libs.compose.bom.get()))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // VOS4 Modules
    implementation(project(":Modules:SpeechRecognition"))
    // implementation(project(":Modules:AvanueUI:Voice"))  // DISABLED: Module path mapping issue
    // implementation(project(":Modules:VoiceOS:managers:VoiceDataManager"))  // DISABLED: Depends on SQLDelight
    implementation(project(":Modules:Localization"))  // KMP LocalizationManager module
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    
    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}