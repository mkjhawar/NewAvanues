plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

group = "com.augmentalis.universal.libraries.avaelements"
version = "1.0.0"

kotlin {
    // Target platforms
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm()

    val iosX64Target = iosX64()
    val iosArm64Target = iosArm64()
    val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // MagicUI Core
                implementation(project(":Modules:AVAMagic:AVAUI:Core"))
                // MagicUI Component Categories
                implementation(project(":Modules:AVAMagic:AVAUI:Input"))
                implementation(project(":Modules:AVAMagic:AVAUI:Feedback"))
                implementation(project(":Modules:AVAMagic:AVAUI:Display"))
                implementation(project(":Modules:AVAMagic:AVAUI:Layout"))
                implementation(project(":Modules:AVAMagic:AVAUI:Navigation"))
                implementation(project(":Modules:AVAMagic:AVAUI:Floating"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.compose.material3:material3:1.2.0")
                implementation("androidx.compose.ui:ui:1.5.4")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avamagic.avaui.templates"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
