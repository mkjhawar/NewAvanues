#!/bin/bash

# Create build.gradle.kts for each module

MODULES=(
    "accessibility"
    "recognition" 
    "audio"
    "commands"
    "overlay"
    "localization"
    "licensing"
    "browser"
    "keyboard"
    "launcher"
    "filemanager"
    "smartglasses"
    "communication"
    "data"
    "uikit"
    "deviceinfo"
    "updatesystem"
)

for MODULE in "${MODULES[@]}"; do
    cat > "modules/$MODULE/build.gradle.kts" << EOF
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.augmentalis.voiceos.$MODULE"
    compileSdk = 34

    defaultConfig {
        minSdk = 28
        targetSdk = 33
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
}

dependencies {
    // Core module
    implementation(project(":modules:core"))
    
    // Android
    implementation("androidx.core:core-ktx:1.12.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
EOF

    # Create AndroidManifest.xml
    mkdir -p "modules/$MODULE/src/main"
    cat > "modules/$MODULE/src/main/AndroidManifest.xml" << EOF
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
</manifest>
EOF

    # Create consumer-rules.pro
    touch "modules/$MODULE/consumer-rules.pro"
    touch "modules/$MODULE/proguard-rules.pro"
    
    echo "Created module: $MODULE"
done

# Update main app build.gradle.kts to include all modules
cat > "app/build.gradle.kts" << 'EOF'
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
}

android {
    namespace = "com.augmentalis.voiceos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.augmentalis.voiceos"
        minSdk = 28
        targetSdk = 33
        versionCode = 1
        versionName = "3.0.0"

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
    }
    
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }
}

dependencies {
    // All modules for monolithic app
    implementation(project(":modules:core"))
    implementation(project(":modules:accessibility"))
    implementation(project(":modules:recognition"))
    implementation(project(":modules:audio"))
    implementation(project(":modules:commands"))
    implementation(project(":modules:overlay"))
    implementation(project(":modules:localization"))
    implementation(project(":modules:licensing"))
    implementation(project(":modules:browser"))
    implementation(project(":modules:keyboard"))
    implementation(project(":modules:launcher"))
    implementation(project(":modules:filemanager"))
    implementation(project(":modules:smartglasses"))
    implementation(project(":modules:communication"))
    implementation(project(":modules:data"))
    implementation(project(":modules:uikit"))
    implementation(project(":modules:deviceinfo"))
    implementation(project(":modules:updatesystem"))
    
    // Android core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.0")
    
    // Compose (only for settings)
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    
    // Vosk/Vivoka libraries
    implementation("com.alphacephei:vosk-android:0.3.47")
    implementation(files("libs/vsdk-6.0.0.aar"))
    implementation(files("libs/vsdk-csdk-asr-2.0.0.aar"))
    implementation(files("libs/vsdk-csdk-core-1.0.1.aar"))
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
}
EOF

echo "Module setup complete!"