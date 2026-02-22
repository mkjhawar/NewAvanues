// filename: Universal/AVA/Features/RAG/build.gradle.kts
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

kotlin {
    // Suppress expect/actual class Beta warning (KT-61573)
    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }

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
        ).forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "AvaRAG"
                isStatic = true
            }
        }
    }
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // AVA Core modules (KMP-compatible only)
                implementation(project(":Modules:AVA:core:Utils"))
                implementation(project(":Modules:AVA:core:Domain"))
                // Note: Core:Data is Android-only, moved to androidMain

                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }

        val androidMain by getting {
            dependencies {
                // AvanueUI theming
                implementation(project(":Modules:AvanueUI"))

                // AVA Core modules (Android-only)
                implementation(project(":Modules:AVA:core:Data"))

                // Shared AI modules
                implementation(project(":Modules:AI:LLM"))

                // NLU module for real BERT WordPiece tokenization
                implementation(project(":Modules:AI:NLU"))

                // Compose dependencies
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.ui:ui-graphics")
                implementation("androidx.compose.foundation:foundation")
                implementation("androidx.compose.foundation:foundation-layout")
                implementation("androidx.compose.material3:material3")
                implementation("androidx.compose.material:material-icons-extended")
                implementation("androidx.compose.runtime:runtime")
                implementation("androidx.activity:activity-compose:1.8.2")

                // ONNX Runtime for embeddings
                implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.3")

                // PDF text extraction
                implementation("com.tom-roush:pdfbox-android:2.0.27.0") {
                    exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
                    exclude(group = "org.bouncycastle", module = "bcpkix-jdk15to18")
                    exclude(group = "org.bouncycastle", module = "bcutil-jdk15to18")
                }

                // HTML parsing (for HTML files and web documents)
                implementation("org.jsoup:jsoup:1.17.1")

                // DOCX parsing (Apache POI)
                implementation("org.apache.poi:poi-ooxml:5.2.5")
                implementation("org.apache.xmlbeans:xmlbeans:5.1.1")

                // Security (EncryptedSharedPreferences for embedding encryption)
                implementation("androidx.security:security-crypto:1.1.0-alpha06")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test:runner:1.5.2")
                implementation("androidx.test:core:1.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
    }
}

android {
    namespace = "com.augmentalis.rag"
    compileSdk = 35

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
