plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    // Android target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Common"
            isStatic = true
        }
    }
    // macOS targets
    macosX64()
    macosArm64()

    // Desktop JVM target
    jvm("desktop") {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // Web/JS target
    js(IR) {
        browser()
        nodejs()
    }

    sourceSets {
        // Common code (pure Kotlin, no platform dependencies)
        val commonMain by getting {
            dependencies {
                // Kotlinx coroutines for Mutex, withLock, CoroutineExceptionHandler
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        // Android-specific
        val androidMain by getting {
            dependencies {
                // Timber logging library (Android-only)
                api("com.jakewharton.timber:timber:5.0.1")

                // Firebase Crashlytics (optional - graceful degradation if missing)
                compileOnly(project.dependencies.platform("com.google.firebase:firebase-bom:32.7.0"))
                compileOnly("com.google.firebase:firebase-crashlytics-ktx")
            }
        }

        // iOS-specific (currently none needed)
        // Note: iOS source set hierarchy is auto-configured by kotlin.mpp.applyDefaultHierarchyTemplate

        // Desktop-specific (currently none needed)
        val desktopMain by getting
    }
}

android {
    namespace = "com.augmentalis.ava.core.common"
    compileSdk = 35

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
