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

    // iOS targets for native rendering
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "IDEAMagicAdapters"
            isStatic = true
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Core components (data models)
                implementation(project(":Modules:AVAMagic:AVAUI:Core"))

                // Foundation components (Compose implementations)
                implementation(project(":Modules:AVAMagic:AVAUI:Foundation"))

                // Design system
                implementation(project(":Modules:AVAMagic:AVAUI:DesignSystem"))
                implementation(project(":Modules:AVAMagic:AVAUI:CoreTypes"))
                implementation(project(":Modules:AVAMagic:AVAUI:StateManagement"))

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
            }
        }

        val androidMain by getting
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
    namespace = "com.augmentalis.avanues.avamagic.components.adapters"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
