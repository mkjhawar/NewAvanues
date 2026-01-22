plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
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
    // iOS targets - only compiled when explicitly requested
    // To build iOS: ./gradlew :Modules:VoiceOS:core:accessibility-types:linkDebugFrameworkIosArm64
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
                baseName = "Memory"
                isStatic = true
            }
        }
    }
    // Desktop JVM target
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        // Common code (pure Kotlin, no platform dependencies)
        val commonMain by getting {
            dependencies {
                // Kotlinx coroutines for async memory operations
                implementation(libs.kotlinx.coroutines.core)

                // DateTime for timestamps
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // Android-specific
        val androidMain by getting {
            dependencies {
                // Room database for Android persistence
                // Will be added when implementing persistence layer
            }
        }

        // iOS-specific (currently none needed)
        // Note: iOS source set hierarchy is auto-configured by kotlin.mpp.applyDefaultHierarchyTemplate

        // Desktop-specific (currently none needed)
        val desktopMain by getting
    }
}

android {
    namespace = "com.augmentalis.memory"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}
