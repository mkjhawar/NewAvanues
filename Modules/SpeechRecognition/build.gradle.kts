/**
 * SpeechRecognition KMP Module Build Configuration
 *
 * Kotlin Multiplatform module providing unified speech recognition capabilities
 * with platform-specific implementations for Android, iOS, Desktop, and JS.
 *
 * Structure:
 * - commonMain: Shared API, models, and logic
 * - androidMain: Android-specific engines (Vosk, Vivoka, Google, Whisper)
 * - iosMain: iOS-specific engines (placeholder)
 * - desktopMain: Desktop-specific engines (placeholder)
 * - jsMain: Web-specific engines (placeholder)
 */

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    alias(libs.plugins.kotlin.compose)
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
}

kotlin {
    // Android target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
        gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
    ) {
        // iOS targets
        listOf(
            iosX64(),
            iosArm64(),
            iosSimulatorArm64()
        ).forEach {
            it.binaries.framework {
                baseName = "SpeechRecognition"
                isStatic = true
            }
        }
    }
    // Desktop target (JVM)
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
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
                // Kotlin standard library
                implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // NLU module for CommandMatchingService
                implementation(project(":Modules:AI:NLU"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android Core
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.appcompat:appcompat:1.6.1")

                // Android Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

                // RecyclerView for help menu UI
                implementation("androidx.recyclerview:recyclerview:1.3.2")

                // Compose UI (for Model Download UI)
                implementation("androidx.compose.ui:ui:1.5.4")
                implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
                implementation("androidx.compose.material3:material3:1.1.2")
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
                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.google.code.gson:gson:2.10.1")

                // Firebase
                implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
                implementation("com.google.firebase:firebase-config")

                // Hilt
                implementation("com.google.dagger:hilt-android:2.51.1")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.24")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                implementation("org.robolectric:robolectric:4.11.1")
                implementation("io.mockk:mockk:1.13.8")
            }
        }

        if (project.findProperty("kotlin.mpp.enableNativeTargets") == "true" ||
            gradle.startParameter.taskNames.any { it.contains("ios", ignoreCase = true) || it.contains("Framework", ignoreCase = true) }
        ) {
            val iosX64Main by getting
            val iosArm64Main by getting
            val iosSimulatorArm64Main by getting
            val iosMain by creating {
                dependsOn(commonMain)
                iosX64Main.dependsOn(this)
                iosArm64Main.dependsOn(this)
                iosSimulatorArm64Main.dependsOn(this)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
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
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        multiDexEnabled = true

        buildConfigField("String", "MODULE_VERSION", "\"3.0.0\"")
        buildConfigField("String", "MODULE_NAME", "\"SpeechRecognition-KMP\"")
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
    add("kspAndroid", "com.google.dagger:hilt-compiler:2.51.1")
}
