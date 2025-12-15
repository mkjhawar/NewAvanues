// BrowserCoreData Module
// Data management library with 407+ tests
// Migrated from browser-plugin

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sqldelight)
}

kotlin {
    // Android target
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iOS targets (Phase 2)
    // iosX64()
    // iosArm64()
    // iosSimulatorArm64()

    // Desktop JVM target (Phase 2)
    // jvm("desktop") {
    //     compilations.all {
    //         kotlinOptions {
    //             jvmTarget = "17"
    //         }
    //     }
    // }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // UUID
                implementation(libs.uuid)

                // Logging
                implementation(libs.napier)

                // SQLDelight - Cross-platform database
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                // Koin for Dependency Injection
                implementation(libs.koin.core)
                implementation(libs.koin.compose)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                // Android WebView
                implementation(libs.androidx.webkit)

                // SQLDelight Android Driver
                implementation(libs.sqldelight.android.driver)

                // SQLCipher for database encryption
                implementation(libs.sqlcipher.android)
                implementation(libs.androidx.sqlite.ktx)
                implementation(libs.androidx.sqlite.framework)

                // Android specific
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }

        // iOS Main (Phase 2 - Disabled for now)
        // val iosX64Main by getting
        // val iosArm64Main by getting
        // val iosSimulatorArm64Main by getting
        // val iosMain by creating {
        //     dependsOn(commonMain)
        //     iosX64Main.dependsOn(this)
        //     iosArm64Main.dependsOn(this)
        //     iosSimulatorArm64Main.dependsOn(this)
        //     dependencies {
        //         implementation("app.cash.sqldelight:native-driver:2.0.1")
        //     }
        // }

        // Desktop Main (Phase 2 - Disabled for now)
        // val desktopMain by getting {
        //     dependencies {
        //         implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
        //         implementation(compose.desktop.currentOs)
        //         implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
        //     }
        // }
    }
}

// Android Configuration
android {
    namespace = "com.augmentalis.webavanue"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
}

// SQLDelight Configuration
sqldelight {
    databases {
        create("BrowserDatabase") {
            packageName.set("com.augmentalis.webavanue.data.db")
            dialect(libs.sqldelight.sqlite.dialect)
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(false)
        }
    }
}

// Publishing Configuration
group = "com.augmentalis.webavanue"
version = "4.0.0-alpha"
