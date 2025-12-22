plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose)
    // alias(libs.plugins.kotlin.compose)  // Only for Kotlin 2.0+
    // alias(libs.plugins.sentry)  // Not defined in version catalog
}

android {
    namespace = "com.augmentalis.webavanue"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.augmentalis.webavanue"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    // Sentry Configuration
    configure<io.sentry.android.gradle.extensions.SentryPluginExtension> {
        autoUploadProguardMapping.set(true)
        autoUploadNativeSymbols.set(true)
        tracingInstrumentation {
            enabled.set(true)
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Project modules
    implementation(project(":Modules:WebAvanue:coredata"))
    implementation(project(":Modules:WebAvanue:universal"))

    // Voyager Navigation (KMP-compatible)
    implementation(libs.voyager.navigator)
    implementation(libs.voyager.transitions)

    // Compose Multiplatform
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(compose.uiTooling)

    // Material Icons Extended - provides additional icons (Tab, Bookmark, Mic, etc.)
    implementation(libs.compose.material.icons.extended)

    // Activity Compose
    implementation(libs.androidx.activity.compose)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Lifecycle
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // DateTime
    implementation(libs.kotlinx.datetime)

    // Core AndroidX
    implementation(libs.androidx.core.ktx)

    // WebView
    implementation(libs.androidx.webkit)

    // Security - Encrypted credential storage
    implementation(libs.androidx.security.crypto)

    // Logging - Napier (KMP structured logging)
    implementation(libs.napier)

    // Sentry - Crash Reporting & Performance Monitoring
    implementation(libs.sentry.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlin.test)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    // Debug tooling
    debugImplementation(libs.compose.ui.test.manifest)
}
