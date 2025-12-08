// BrowserCoreData Module
// Data management library with 407+ tests
// Migrated from browser-plugin

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app.cash.sqldelight")
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
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                // UUID
                implementation("com.benasher44:uuid:0.8.1")

                // SQLDelight - Cross-platform database
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)

                // Koin for Dependency Injection
                implementation("io.insert-koin:koin-core:3.5.0")
                implementation("io.insert-koin:koin-compose:1.1.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                // Android WebView
                implementation("androidx.webkit:webkit:1.9.0")

                // SQLDelight Android Driver
                implementation("app.cash.sqldelight:android-driver:2.0.1")

                // Android specific
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
                implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.robolectric:robolectric:4.11.1")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
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
    namespace = "com.augmentalis.Avanues.web.data"
    compileSdk = 34

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
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:2.0.1")
            deriveSchemaFromMigrations.set(false)
            verifyMigrations.set(false)
        }
    }
}

// Publishing Configuration
group = "com.augmentalis.Avanues.web"
version = "4.0.0-alpha"
