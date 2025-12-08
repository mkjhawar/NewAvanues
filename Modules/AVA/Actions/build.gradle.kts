plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
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

    // iOS targets
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Actions"
            isStatic = true
        }
    }

    // Desktop JVM target (macOS, Windows, Linux)
    jvm("desktop") {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
    }

    sourceSets {
        // Common code (shared across all platforms)
        val commonMain by getting {
            dependencies {
                implementation(project(":core:Utils"))
                implementation(project(":core:Domain"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        // Android-specific code
        val androidMain by getting {
            dependencies {
                implementation(project(":core:Data"))
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.hilt.android)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("io.mockk:mockk:1.13.8")
                implementation("org.robolectric:robolectric:4.11")
                implementation("com.google.truth:truth:1.1.5")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test.espresso:espresso-core:3.5.1")
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test:runner:1.5.2")
                implementation("com.google.truth:truth:1.1.5")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        // iOS-specific code (stubs for now)
        // Note: iOS source set hierarchy is auto-configured by kotlin.mpp.applyDefaultHierarchyTemplate

        // Desktop-specific code (stubs for now)
        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.ava.features.actions"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isReturnDefaultValues = true
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
}

dependencies {
    add("kspAndroid", libs.hilt.compiler)
}
