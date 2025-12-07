// WebAvanue Universal Module
// 95% shared code across Android, iOS, and Desktop

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
    // SQLDelight plugin removed - using BrowserCoreData for database access
}

kotlin {
    // Android Target (Phase 1)
    androidTarget {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            }
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
                implementation(project(":common:webavanue:coredata"))

                // Kotlin
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                // SQLDelight - Cross-platform database (inherited from BrowserCoreData)
                implementation("app.cash.sqldelight:runtime:2.0.1")
                implementation("app.cash.sqldelight:coroutines-extensions:2.0.1")

                // Compose Multiplatform
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended) // For Mic, Laptop, FileDownload, TouchApp icons

                // Voyager - KMP Navigation
                implementation("cafe.adriel.voyager:voyager-navigator:1.0.0")
                implementation("cafe.adriel.voyager:voyager-screenmodel:1.0.0")
                implementation("cafe.adriel.voyager:voyager-tab-navigator:1.0.0")
                implementation("cafe.adriel.voyager:voyager-transitions:1.0.0")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        // Android Main
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

                // Activity Compose - For rememberLauncherForActivityResult (file upload support)
                implementation("androidx.activity:activity-compose:1.8.2")
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("junit:junit:4.13.2")
                implementation("org.robolectric:robolectric:4.11.1")
                implementation("app.cash.sqldelight:sqlite-driver:2.0.1")
            }
        }

        val androidInstrumentedTest by getting {
            dependencies {
                // Kotlin test
                implementation(kotlin("test"))
                implementation(kotlin("test-junit"))

                // Android test core
                implementation("androidx.test:core:1.5.0")
                implementation("androidx.test:runner:1.5.2")
                implementation("androidx.test:rules:1.5.0")
                implementation("androidx.test.ext:junit:1.1.5")
                implementation("androidx.test.ext:junit-ktx:1.1.5")

                // Compose test
                implementation("androidx.compose.ui:ui-test-junit4:1.5.4")
                implementation("androidx.compose.ui:ui-test-manifest:1.5.4")

                // SQLDelight Android Driver for tests
                implementation("app.cash.sqldelight:android-driver:2.0.1")

                // Coroutines test
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
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
