plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
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
                // Core modules
                implementation(project(":core:Domain"))
                implementation(project(":core:Utils"))
                implementation(project(":SharedPlatform"))

                // Kotlin Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Kotlin Serialization
                implementation(libs.kotlinx.serialization.json)

                // SQLDelight - KMP database
                api(libs.sqldelight.runtime)
                api(libs.sqldelight.coroutines.extensions)
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
                // Android-specific SQLDelight driver
                api(libs.sqldelight.android.driver)

                // DataStore (Android-specific for developer preferences)
                implementation(libs.androidx.datastore.preferences)

                // Dagger Hilt for dependency injection (Android-specific)
                implementation(libs.hilt.android)
                implementation(libs.androidx.hilt.common)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.mockk)
                implementation(libs.robolectric)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.core.ktx)
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.androidx.test.ext.junit)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.core.ktx)
            }
        }

        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                // iOS-specific SQLDelight driver
                api(libs.sqldelight.native.driver)
            }
        }

        val iosTest by creating {
            dependsOn(commonTest)
        }
    }
}

android {
    namespace = "com.augmentalis.ava.core.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 28  // Android 9+ (Pie and above)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// SQLDelight Configuration
sqldelight {
    databases {
        create("AVADatabase") {
            packageName.set("com.augmentalis.ava.core.data.db")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            // Use CREATE TABLE statements in .sq files directly
            // Set to true only when migration files (.sqm) exist
            deriveSchemaFromMigrations.set(false)
        }
    }
}
