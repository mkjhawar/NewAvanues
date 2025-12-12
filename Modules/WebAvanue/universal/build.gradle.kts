// WebAvanue Universal Module
// 95% shared code across Android, iOS, and Desktop

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.dokka)
    // SQLDelight plugin removed - using BrowserCoreData for database access
}

// Dokka Configuration
tasks.withType<org.jetbrains.dokka.gradle.DokkaTask>().configureEach {
    moduleName.set("WebAvanue Universal")
    moduleVersion.set("4.0.0-alpha")

    dokkaSourceSets {
        configureEach {
            includeNonPublic.set(false)
            skipEmptyPackages.set(true)
            skipDeprecated.set(false)
            reportUndocumented.set(true)
        }
    }
}

kotlin {
    // Android Target (Phase 1)
    androidTarget {
        @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
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
    //         baseName = "universal"
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
                // BrowserCoreData - Shared data layer with LRU caching
                implementation(project(":coredata"))

                // Kotlin
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)

                // Logging - Napier (KMP structured logging)
                implementation(libs.napier)

                // SQLDelight - Cross-platform database (inherited from BrowserCoreData)
                implementation(libs.sqldelight.runtime)
                implementation(libs.sqldelight.coroutines.extensions)

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended) // For Mic, Laptop, FileDownload, TouchApp icons

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

                // Android specific
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.lifecycle.viewmodel.compose)

                // Activity Compose - For rememberLauncherForActivityResult (file upload support)
                implementation(libs.androidx.activity.compose)

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
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                // Kotlin test
                implementation(kotlin("test"))
                implementation(libs.kotlin.test.junit)

                // Android test core
                implementation(libs.androidx.test.core)
                implementation(libs.androidx.test.runner)
                implementation(libs.androidx.test.rules)
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.test.junit.ktx)

                // Compose test
                implementation(libs.compose.ui.test.junit4)
                implementation(libs.compose.ui.test.manifest)

                // SQLDelight Android Driver for tests
                implementation(libs.sqldelight.android.driver)

                // Coroutines test
                implementation(libs.kotlinx.coroutines.test)
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
        //
        // val iosX64Test by getting
        // val iosArm64Test by getting
        // val iosSimulatorArm64Test by getting
        // val iosTest by creating {
        //     dependsOn(commonTest)
        //     iosX64Test.dependsOn(this)
        //     iosArm64Test.dependsOn(this)
        //     iosSimulatorArm64Test.dependsOn(this)
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
        //
        // val desktopTest by getting {
        //     dependencies {
        //         implementation(kotlin("test-junit"))
        //     }
        // }
    }
}

// Android Configuration
android {
    namespace = "com.augmentalis.Avanues.web.universal"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
}

// SQLDelight Configuration
// REMOVED: Database schema migrated to BrowserCoreData module
// universal module now depends on BrowserCoreData for all data persistence

// Publishing Configuration
group = "com.augmentalis.Avanues.web"
version = "4.0.0-alpha"

// Maven Publishing
// Phase 3: BrowserCoreData integration complete via project dependency
