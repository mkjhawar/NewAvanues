// Author: Manoj Jhawar
// Code-Reviewed-By: CCA

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("maven-publish")
    kotlin("plugin.serialization") version "1.9.24"
}

android {
    namespace = "com.augmentalis.voiceui"
    compileSdk = 35

    defaultConfig {
        minSdk = 28
        targetSdk = 34
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Library configuration
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
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // Required managers
    implementation(project(":managers:CommandManager"))
    implementation(project(":managers:LocalizationManager"))
    
    // Required libraries
    implementation(project(":libraries:UUIDManager"))
    
    // DeviceManager library for device info and DPI calculations
    implementation(project(":libraries:DeviceManager"))
    
    // Kotlin - Latest stable
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // AndroidX Core - Latest stable
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    
    // Compose BOM - Latest stable
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    
    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")
    
    // Navigation - Latest stable
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Window management - Latest stable
    implementation("androidx.window:window:1.3.0")
    
    // DataStore - Latest stable
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    
    // Google Material Design Components
    implementation("com.google.android.material:material:1.12.0")
    
    // Testing - Latest stable
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug tools - Using BOM versions
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// Publishing configuration for AAR/JAR export
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.augmentalis.voiceui"
            artifactId = "voiceui-library"
            version = "1.0.0"
            
            afterEvaluate {
                from(components["release"])
            }
            
            pom {
                name.set("VoiceUI Library")
                description.set("Advanced voice command system with UUID-based targeting")
                url.set("https://github.com/augmentalis/voiceos")
                
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                
                developers {
                    developer {
                        id.set("manoj")
                        name.set("Manoj Jhawar")
                        email.set("manoj@augmentalis.com")
                    }
                }
            }
        }
    }
}

// JAR task for pure Kotlin components
tasks.register<Jar>("kotlinJar") {
    from(tasks.named("compileReleaseKotlin"))
    archiveClassifier.set("kotlin")
    archiveBaseName.set("voiceui-kotlin")
}