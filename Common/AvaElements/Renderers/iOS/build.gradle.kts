plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

version = "1.0.0"
group = "com.augmentalis.avaelements"

kotlin {
    // iOS targets for Kotlin/Native
    ios()
    iosSimulatorArm64()

    // Optional: Support for Mac Catalyst
    // iosX64() // For older simulators

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Kotlin standard library
                implementation(kotlin("stdlib-common"))

                // Kotlinx serialization for data transfer
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

                // Kotlinx coroutines for async operations
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val iosMain by getting {
            dependencies {
                // Dependency on MagicElements Core
                implementation(project(":Universal:Libraries:AvaElements:Core"))
                implementation(project(":Universal:Libraries:AvaElements:components:phase1"))
                implementation(project(":Universal:Libraries:AvaElements:components:phase3"))
                implementation(project(":Universal:Libraries:AvaElements:components:flutter-parity"))

                // iOS-specific dependencies can be added here
            }
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }
    }
}

android {
    namespace = "com.augmentalis.avaelements.renderer.ios"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Kotlin/Native configuration for iOS interop
kotlin {
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        binaries {
            framework {
                baseName = "MagicElementsiOS"

                // Export the main API classes for Swift consumption
                export(project(":Universal:Libraries:AvaElements:Core"))
                export(project(":Universal:Libraries:AvaElements:components:phase1"))
                export(project(":Universal:Libraries:AvaElements:components:phase3"))
                export(project(":Universal:Libraries:AvaElements:components:flutter-parity"))

                // Ensure all API is exposed to Objective-C/Swift
                isStatic = false

                // Optimization settings
                freeCompilerArgs += listOf(
                    "-Xexport-kdoc",  // Export KDoc comments to Swift
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.cinterop.ExperimentalForeignApi"
                )
            }
        }

        compilations.getByName("main") {
            cinterops {
                // Add C interop definitions here if needed for iOS APIs
            }
        }
    }
}

// Task to generate iOS framework
tasks.register("buildIOSFramework") {
    group = "build"
    description = "Build iOS framework for Xcode integration"

    dependsOn("linkDebugFrameworkIos")
    dependsOn("linkReleaseFrameworkIos")

    doLast {
        println("iOS Framework built successfully!")
        println("Debug framework: build/bin/ios/debugFramework/")
        println("Release framework: build/bin/ios/releaseFramework/")
    }
}

// Task to generate XCFramework for distribution
tasks.register("buildXCFramework") {
    group = "build"
    description = "Build XCFramework for iOS app distribution"

    dependsOn(
        "linkReleaseFrameworkIos",
        "linkReleaseFrameworkIosSimulatorArm64"
    )

    doLast {
        exec {
            commandLine(
                "xcodebuild", "-create-xcframework",
                "-framework", "build/bin/ios/releaseFramework/MagicElementsiOS.framework",
                "-framework", "build/bin/iosSimulatorArm64/releaseFramework/MagicElementsiOS.framework",
                "-output", "build/xcframework/MagicElementsiOS.xcframework"
            )
        }
        println("XCFramework created: build/xcframework/MagicElementsiOS.xcframework")
    }
}

// Documentation generation
tasks.register("generateiOSDocs") {
    group = "documentation"
    description = "Generate documentation for iOS renderer"

    doLast {
        println("Generating iOS renderer documentation...")
        println("Documentation available in: docs/ios/")
    }
}

// Test configuration
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
