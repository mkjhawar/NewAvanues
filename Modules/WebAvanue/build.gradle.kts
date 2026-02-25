// WebAvanue Module - Unified KMP Browser Library
// Merged from coredata + universal into single flat structure
// 95% shared code across Android, iOS, and Desktop

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.dokka)
}

// Dokka v2 Configuration
dokka {
    moduleName.set("WebAvanue")
    moduleVersion.set("4.0.0-alpha")
    dokkaSourceSets.configureEach {
        suppressGeneratedFiles.set(true)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // Android Target
    androidTarget {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    // iOS Targets (Phase 2 - Disabled for now)
    // listOf(
    //     iosX64(),
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "WebAvanue"
    //         isStatic = true
    //     }
    // }

    // Desktop (JVM) Target (Phase 2 - Disabled for now)
    // jvm("desktop") {
    //     compilations.all {
    //         kotlinOptions {
    //             jvmTarget = "17"
    //         }
    //     }
    // }

    sourceSets {
        // Common Main - 95% shared code
        val commonMain by getting {
            dependencies {
                // Foundation - StateFlow utilities, ViewModels, NumberToWords
                api(project(":Modules:Foundation"))

                // AvaTheme - Canonical theme system (OceanTheme, GlassmorphicComponents)
                api(project(":Modules:AvanueUI"))

                // AvanueUI now includes all tokens, theme, and components (consolidated)

                // Logging - Canonical KMP logging infrastructure
                implementation(project(":Modules:Logging"))

                // Unified Database - Web command persistence (IScrapedWebCommandRepository, etc.)
                implementation(project(":Modules:Database"))

                // AVID for unified identifier generation
                implementation(project(":Modules:AVID"))

                // VoiceOSCore - IWebCommandExecutor interface, WebAction types, QuantizedCommand
                implementation(project(":Modules:VoiceOSCore"))

                // Kotlin
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Atomic operations for KMP thread safety
                implementation(libs.kotlinx.atomicfu)

                // Logging - Napier (KMP structured logging)
                implementation(libs.napier)

                // SQLDelight - Cross-platform database
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)

                // Voyager - KMP Navigation
                implementation(libs.voyager.navigator)
                implementation(libs.voyager.screenmodel)
                implementation(libs.voyager.tab.navigator)
                implementation(libs.voyager.transitions)

            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        // Android Main
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
                implementation(libs.androidx.lifecycle.runtime.compose)

                // Activity Compose - For rememberLauncherForActivityResult (file upload support)
                implementation(libs.androidx.activity.compose)

                // DocumentFile - For custom download paths
                implementation("androidx.documentfile:documentfile:1.0.1")

                // Security - EncryptedSharedPreferences
                implementation("androidx.security:security-crypto:1.1.0-alpha06")

                // Crash reporting - Sentry
                implementation("io.sentry:sentry-android:7.0.0")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.robolectric)
                implementation(libs.sqldelight.sqlite.driver)
                implementation("androidx.test:core:1.5.0")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                // Kotlin test
                implementation(kotlin("test"))
                implementation(libs.kotlin.test.junit)

                // Compose test
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.compose.ui.test.manifest)

                // SQLDelight Android Driver for tests
                implementation(libs.sqldelight.android.driver)

                // Coroutines test
                implementation(libs.kotlinx.coroutines.test)

                // MockK for integration tests
                implementation("io.mockk:mockk-android:1.13.8")
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
        //
        //     dependencies {
        //         // SQLDelight iOS Driver
        //         implementation("app.cash.sqldelight:native-driver:2.0.1")
        //     }
        // }

        // Desktop Main (Phase 2 - Disabled for now)
        // val desktopMain by getting {
        //     dependencies {
        //         // SQLDelight Desktop Driver
        //         implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
        //
        //         // Desktop specific
        //         implementation(compose.desktop.currentOs)
        //     }
        // }
    }
}

// Android Configuration
android {
    namespace = "com.augmentalis.webavanue"
    compileSdk = 35

    defaultConfig {
        minSdk = 29  // Raised from 26 to match VoiceOSCore dependency (API 29+ features)
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
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
