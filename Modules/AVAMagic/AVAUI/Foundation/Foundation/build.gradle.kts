plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
}

group = "com.augmentalis.avanues.avamagic"
version = "1.0.0"

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":Modules:AvaMagic:AvaUI:DesignSystem"))
                implementation(project(":Modules:AvaMagic:AvaUI:CoreTypes"))
                implementation(project(":Modules:AvaMagic:AvaUI:StateManagement"))

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
            }
        }

        all {
            languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
                // Compose test removed - not available in this version
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.compose.material:material-icons-extended:1.6.8")
            }
        }

        val desktopMain by getting

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
}

android {
    namespace = "com.augmentalis.avanues.avamagic.components"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
