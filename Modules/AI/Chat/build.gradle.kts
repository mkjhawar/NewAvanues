plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

dependencies {
    // Core dependencies
    implementation(project(":Modules:AVA:core:Utils"))
    implementation(project(":Modules:AVA:core:Domain"))
    implementation(project(":Modules:AVA:core:Data"))
    implementation(project(":Modules:AVA:core:Theme"))  // Ocean Glass Design System
    //implementation(project(":Modules:AVA:SharedNLU"))  // TODO: SharedNLU not found in current structure
    implementation(project(":Modules:Actions"))
    implementation(project(":Modules:AI:LLM"))
    implementation(project(":Modules:AI:RAG"))  // RAG Phase 2: Chat UI Integration

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")  // For hiltViewModel() in Compose

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Kotlin DateTime (KMP-compatible)
    implementation(libs.kotlinx.datetime)

    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.1")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Logging (provided by Common module)
    // Timber is now provided via Common module

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation(kotlin("test"))
    testImplementation("io.mockk:mockk-android:1.13.8")  // mockk-android for final class mocking in Android JVM tests
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("androidx.test:core:1.5.0")  // For ApplicationProvider in unit tests
    testImplementation("org.robolectric:robolectric:4.11.1")  // Android environment for unit tests

    // Android instrumented tests
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation(kotlin("test"))

    // MockK for instrumented tests - using standard mockk for Android instrumented tests
    androidTestImplementation("io.mockk:mockk:1.13.8")

    // Mockito for instrumented tests
    androidTestImplementation("org.mockito:mockito-android:5.7.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")

    // Coroutines test support for instrumented tests
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // Turbine for flow testing in instrumented tests
    androidTestImplementation("app.cash.turbine:turbine:1.0.0")

    // Hilt testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48.1")
    kaptAndroidTest("com.google.dagger:hilt-compiler:2.48.1")
}

android {
    namespace = "com.augmentalis.chat"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Configure source sets for KMP-style structure
    sourceSets {
        getByName("main") {
            kotlin.srcDirs("src/main/kotlin", "src/commonMain/kotlin", "src/androidMain/kotlin")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true  // Required for Robolectric to work in all build variants
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.7"  // Compatible with Kotlin 1.9.21
    }

    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md"
            )
        }
    }
}
