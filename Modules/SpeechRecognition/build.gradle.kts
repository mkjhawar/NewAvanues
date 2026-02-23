/**
 * SpeechRecognition KMP Module Build Configuration
 *
 * Kotlin Multiplatform module providing unified speech recognition capabilities
 * with platform-specific implementations for Android, iOS, Desktop, and JS.
 *
 * Structure:
 * - commonMain: Shared API, models, and logic
 * - jvmMain: Shared JVM code for Android + Desktop (VSM encryption via javax.crypto)
 * - androidMain: Android-specific engines (Vosk, Vivoka, Google, Whisper)
 * - iosMain: iOS-specific engines (Apple Speech + Whisper via cinterop)
 * - macosMain: macOS-specific engines (Apple Speech via SFSpeechRecognizer)
 * - desktopMain: Desktop-specific engines (Whisper via JNI)
 * - jsMain: Web-specific engines (placeholder)
 */

import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

kotlin {
    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iOS targets with whisper_bridge cinterop
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "SpeechRecognition"
            isStatic = true
            // Link against whisper_bridge static library when available
            // Build: cd Modules/Whisper && ./build-xcframework.sh
            // The library must be placed at:
            //   libs/ios-arm64/libwhisper_bridge.a (device)
            //   libs/ios-x64/libwhisper_bridge.a (simulator x64)
            //   libs/ios-sim-arm64/libwhisper_bridge.a (simulator arm64)
        }

        // Generate Kotlin bindings via cinterop
        it.compilations.getByName("main") {
            cinterops {
                // whisper.cpp bridge for offline speech recognition
                val whisper by creating {
                    defFile("src/nativeInterop/cinterop/whisper.def")
                    includeDirs("src/nativeInterop/cinterop")
                }
                // CommonCrypto for VSM model file encryption/decryption
                val commoncrypto by creating {
                    defFile("src/nativeInterop/cinterop/commoncrypto.def")
                }
            }
        }
    }

    // macOS targets — Apple Speech via SFSpeechRecognizer (macOS 10.15+)
    macosX64()
    macosArm64()
    // Desktop target (JVM)
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // JS target (disabled - conflicts with NodeJsRootPlugin in this project)
    // Uncomment when needed:
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // NLU module for CommandMatchingService
                implementation(project(":Modules:AI:NLU"))

                // Foundation for ModuleSettingsProvider
                implementation(project(":Modules:Foundation"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // Shared JVM source set for Android + Desktop
        // Contains: VSM encryption/decryption (javax.crypto), shared JVM utilities
        val jvmMain by creating {
            dependsOn(commonMain)
        }

        val androidMain by getting {
            dependsOn(jvmMain)
            dependencies {
                // Android Core
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)

                // Android Coroutines
                implementation(libs.kotlinx.coroutines.android)

                // RecyclerView for help menu UI
                implementation("androidx.recyclerview:recyclerview:1.3.2")

                // Compose UI (for Model Download UI)
                implementation("androidx.compose.ui:ui:1.5.4")
                implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
                implementation("androidx.compose.material3:material3:1.1.2")

                // AvanueUI Design System (for WhisperModelDownloadScreen theme)
                implementation(project(":Modules:AvanueUI"))
                // Using material-icons-core + vector drawables to reduce APK size
                implementation("androidx.compose.material:material-icons-core:1.5.4")
                implementation("androidx.activity:activity-compose:1.8.2")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

                // Android Audio (legacy media library for basic audio support)
                implementation("androidx.media:media:1.7.0")

                // Compatibility and Fallback Support
                implementation("androidx.multidex:multidex:2.0.1")
                implementation("androidx.annotation:annotation:1.7.1")
                implementation("androidx.concurrent:concurrent-futures:1.1.0")

                // JSON Processing
                implementation("org.json:json:20231013")

                // Speech Recognition Engines - compileOnly (not bundled)
                compileOnly("com.alphacephei:vosk-android:0.3.47") {
                    exclude(group = "com.google.guava", module = "listenablefuture")
                }

                // DeviceManager
                implementation(project(":Modules:DeviceManager"))

                // VoiceIsolation - Audio preprocessing (noise suppression, echo cancellation, AGC)
                implementation(project(":Modules:VoiceIsolation"))

                // Vivoka VSDK - compileOnly
                compileOnly(files("${rootDir}/vivoka/vsdk-6.0.0.aar"))
                compileOnly(files("${rootDir}/vivoka/vsdk-csdk-asr-2.0.0.aar"))
                compileOnly(files("${rootDir}/vivoka/vsdk-csdk-core-1.0.1.aar"))

                // OkHttp & Gson
                implementation(libs.okhttp)
                implementation(libs.gson)

                // Firebase
                implementation(project.dependencies.platform("com.google.firebase:firebase-bom:34.3.0"))
                implementation("com.google.firebase:firebase-config")
                implementation("com.google.firebase:firebase-auth")

                // Hilt
                implementation(libs.hilt.android)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlin.test.junit)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.robolectric)
                implementation(libs.mockk)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.kotlinx.atomicfu)
                // Compose runtime needed for kotlin.compose plugin on native targets
                implementation("org.jetbrains.compose.runtime:runtime:1.7.3")
            }
        }

        // macOS source set — Apple Speech via SFSpeechRecognizer
        val macosX64Main by getting
        val macosArm64Main by getting
        val macosMain by creating {
            dependsOn(commonMain)
            macosX64Main.dependsOn(this)
            macosArm64Main.dependsOn(this)
            dependencies {
                implementation(libs.kotlinx.atomicfu)
                implementation("org.jetbrains.compose.runtime:runtime:1.7.3")
            }
        }
        val desktopMain by getting {
            dependsOn(jvmMain)
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
                // Compose runtime needed for kotlin.compose plugin on JVM target
                // (same as iosMain/macosMain — compiler plugin applied to all targets)
                implementation("org.jetbrains.compose.runtime:runtime:1.7.3")
            }
        }

        // JS target disabled - uncomment when JS target is enabled
        // val jsMain by getting {
        //     dependencies {
        //         implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.7.3")
        //     }
        // }
    }
}

android {
    namespace = "com.augmentalis.speechrecognition"
    compileSdk = 35

    defaultConfig {
        minSdk = 28  // Must match NLU dependency (minSdk 28)
        multiDexEnabled = true

        buildConfigField("String", "MODULE_VERSION", "\"3.0.0\"")
        buildConfigField("String", "MODULE_NAME", "\"SpeechRecognition-KMP\"")

        // Vivoka download credentials — injected from local.properties or CI environment
        // NEVER hardcode credentials in source files
        val localProps = rootProject.file("local.properties")
        val props = Properties()
        if (localProps.exists()) {
            localProps.inputStream().use { props.load(it) }
        }
        val vivokaUser = props.getProperty("vivoka.download.username")
            ?: System.getenv("VIVOKA_DOWNLOAD_USERNAME") ?: ""
        val vivokaPwd = props.getProperty("vivoka.download.password")
            ?: System.getenv("VIVOKA_DOWNLOAD_PASSWORD") ?: ""
        buildConfigField("String", "VIVOKA_DOWNLOAD_USERNAME", "\"$vivokaUser\"")
        buildConfigField("String", "VIVOKA_DOWNLOAD_PASSWORD", "\"$vivokaPwd\"")

        // Google Cloud Speech-to-Text v2
        val gcpProjectId = props.getProperty("gcp.speech.project_id")
            ?: System.getenv("GCP_SPEECH_PROJECT_ID") ?: ""
        buildConfigField("String", "GCP_SPEECH_PROJECT_ID", "\"$gcpProjectId\"")
    }

    // Include legacy Android-only source set for Vivoka and other engines
    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
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
    }

    packaging {
        jniLibs {
            keepDebugSymbols += "**/*.so"
        }
        resources {
            excludes += listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt"
            )
        }
    }
}

dependencies {
    // KSP for Hilt
    add("kspAndroid", libs.hilt.compiler)
}

// VLM Tool — CLI for encrypting/decrypting Whisper model files
// Usage: ./gradlew :Modules:SpeechRecognition:runVlmTool --args="batch-encode VLMFiles/"
afterEvaluate {
    if (kotlin.targets.findByName("desktop") != null) {
        tasks.register<JavaExec>("runVlmTool") {
            group = "application"
            description = "Run the VLM Encryption Tool (encrypt/decrypt Whisper models)"
            mainClass.set("com.augmentalis.speechrecognition.cli.VLMToolKt")
            val desktopTarget = kotlin.targets.getByName<org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget>("desktop")
            classpath = desktopTarget.compilations.getByName("main").runtimeDependencyFiles +
                files(desktopTarget.compilations.getByName("main").output.allOutputs)
            // Resolve paths relative to project root (not module dir)
            workingDir = rootProject.projectDir
        }
    }
}
