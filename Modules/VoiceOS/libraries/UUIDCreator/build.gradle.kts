plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    // KSP removed - no longer needed without Room
    id("maven-publish")
}

android {
    namespace = "com.augmentalis.uuidcreator"
    compileSdk = 34  // Android 14 (API 34) - Latest stable
    // compileSdkPreview = "VanillaIceCream"  // Android 15 - will enable when available

    defaultConfig {
        minSdk = 29  // Android 10 (Q) - Minimum supported
        // Note: targetSdk is deprecated for libraries
        multiDexEnabled = true  // Multi-version APK support
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    testOptions {
        unitTests {
            all {
                it.jvmArgs("-Dnet.bytebuddy.experimental=true")
            }
        }
    }
    
    // testOptions {
        // targetSdk = 34  // Android 14 - Production target for testing - REMOVED: deprecated for libraries
    // }
    
    lint {
        // targetSdk = 34  // Android 14 - Production target for lint - REMOVED: deprecated for libraries
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
        aidl = true  // Enable AIDL for IPC service binding
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"  // Compatible with Kotlin 1.9.24
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    // SQLDelight KMP Database (replaces Room)
    implementation(project(":Modules:VoiceOS:core:database"))

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Compatibility Support
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.annotation:annotation:1.7.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Android XR Support (when available)
    // NOTE: These libraries are not yet published. Uncomment when available.
    // compileOnly("androidx.xr:xr-core:1.0.0-alpha01")
    
    // JSON Serialization (still needed for metadata)
    implementation("com.google.code.gson:gson:2.10.1")

    // Compose dependencies
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.runtime:runtime-livedata")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Testing dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.25")
    testImplementation("org.mockito:mockito-core:4.11.0")
    testImplementation("org.mockito:mockito-inline:4.11.0") // Enables mocking of final classes
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // Android Test dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Debug implementations
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.augmentalis"
            artifactId = "uuidcreator"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}