plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    kotlin("kapt")
}

android {
    namespace = "com.augmentalis.llm"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Native library configuration
        ndk {
            // Support 64-bit ARM (most modern Android devices)
            abiFilters += listOf("arm64-v8a")
            // Optionally support 32-bit ARM (older devices)
            // abiFilters += listOf("armeabi-v7a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // TVM v0.22.0 native libraries are managed at app level
    // Located in android/ava/src/main/jniLibs/arm64-v8a/
    // - libtvm4j_runtime_packed.so (104MB) - TVM v0.22.0 packed runtime
    //   Includes: TVM runtime, FFI, JNI bridge, tokenizers, MLC-LLM
    // - libc++_shared.so (1.7MB) - NDK C++ stdlib (required by packed runtime)
}

dependencies {
    // AVA Core modules
    implementation(project(":core:Domain"))
    implementation(project(":core:Utils"))
    implementation(project(":core:Data"))

    // AVA Features - needed for IntentClassification
    implementation(project(":SharedNLU"))

    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Kotlin Serialization (for OpenAI protocol)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    // TVM Runtime (for MLC LLM)
    // Built from MLC-LLM source (external/mlc-llm/3rdparty/tvm/jvm/core/)
    // Compiled with Java 17 (class file major version 61)
    // Original JAR was built with Java 24 (major version 68), causing DEX failures
    // RESOLVED: Rebuilt with JDK 17 on 2025-11-07, assembleDebug now succeeds ✅
    // INCLUDES: Native tokenizer support (HuggingFace tokenizers via Rust FFI)
    implementation(files("libs/tvm4j_core.jar"))

    // Android utilities
    implementation("androidx.core:core-ktx:1.12.0")

    // Security (EncryptedSharedPreferences)
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // HTTP client for cloud LLM APIs
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // WorkManager for background downloads
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Apache Commons Compress for .ALM (tar) extraction
    implementation("org.apache.commons:commons-compress:1.25.0")

    // Logging (provided by Common module)
    // Timber is now provided via Common module's androidMain

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // LiteRT (TFLite) for Gemma 3n support - using stable TFLite for now
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
}
