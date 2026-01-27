plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(project(":Modules:Database"))

                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)

                // Compose (use .get() to resolve provider for platform())
                implementation(platform(libs.compose.bom.get()))
                implementation(libs.compose.ui.ui)
                implementation(libs.compose.ui.tooling.preview)
                implementation(libs.compose.material3)
                implementation(libs.compose.material.icons.extended)
                implementation("androidx.compose.runtime:runtime-livedata")
                implementation(libs.androidx.lifecycle.viewmodel.compose)

                implementation(libs.kotlinx.coroutines.android)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.swing)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.localization"
    compileSdk = 34

    defaultConfig {
        minSdk = 29
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
}
