plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("app.cash.sqldelight") version "2.0.1"
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

    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    js(IR) {
        browser()
        nodejs()
    }

    val iosX64Target = iosX64()
    val iosArm64Target = iosArm64()
    val iosSimulatorArm64Target = iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Asset management dependencies
                implementation(project(":Universal:Libraries:AvaElements:Core"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                // SQLDelight for cross-platform SQLite
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
                // Android SQLite driver
                implementation("app.cash.sqldelight:android-driver:2.0.1")
            }
        }

        val iosMain by creating {
            dependsOn(commonMain)
            dependencies {
                // iOS SQLite driver (native)
                implementation("app.cash.sqldelight:native-driver:2.0.1")
            }
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

        val desktopMain by getting {
            dependencies {
                // Desktop (JVM) SQLite driver
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }

        val jsMain by getting {
            dependencies {
                // Web SQLite driver (sql.js wrapper)
                implementation("app.cash.sqldelight:web-worker-driver:2.0.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avaelements.assets"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

sqldelight {
    databases {
        create("AssetDatabase") {
            packageName.set("com.augmentalis.avaelements.assets.db")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
