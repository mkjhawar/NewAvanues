plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.augmentalis.avamagic.avacode"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            kotlin.srcDirs("commonMain/kotlin", "src/commonMain/kotlin")
            dependencies {
                implementation(project(":Modules:AvaMagic:AvaUI:Core"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation("net.mamoe.yamlkt:yamlkt:0.13.0")
            }
        }

        val androidMain by getting {
            kotlin.srcDirs("src/androidMain/kotlin")
        }
    }
}
