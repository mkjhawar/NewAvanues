plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.android.library")
}

kotlin {
    // JVM Target (TODO: Implement JVM actual classes - currently Android-only)
    // jvm {
    //     compilations.all {
    //         kotlinOptions {
    //             jvmTarget = "17"
    //         }
    //     }
    // }

    // Android Target
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS Targets (TODO: Implement iOS actual classes)
    // listOf(
    //     iosX64(),
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "AssetManager"
    //         isStatic = true
    //     }
    // }

    // macOS Target (TODO: Implement macOS actual classes)
    // macosX64()
    // macosArm64()

    // JS/WASM Targets (future)
    // js(IR) { browser() }
    // wasmJs { browser() }

    sourceSets {
        // Common Source Set
        val commonMain by getting {
            dependencies {
                // Kotlin Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

                // Kotlin Serialization for JSON
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

                // DateTime utilities (optional, for metadata)
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")

                // Kotlin IO for file operations
                implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.1")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
            }
        }

        // JVM Source Set (TODO: Implement after JVM actual classes are created)
        // val jvmMain by getting {
        //     dependencies {
        //         // Java Image IO for image processing
        //         implementation("org.imgscalr:imgscalr-lib:4.2")
        //
        //         // Apache Commons for file utilities
        //         implementation("commons-io:commons-io:2.15.1")
        //     }
        // }
        //
        // val jvmTest by getting {
        //     dependencies {
        //         implementation(kotlin("test-junit"))
        //         implementation("junit:junit:4.13.2")
        //     }
        // }

        // Android Source Set
        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }

        // iOS Source Set (TODO: Implement after iOS actual classes are created)
        // val iosX64Main by getting
        // val iosArm64Main by getting
        // val iosSimulatorArm64Main by getting
        // val iosMain by creating {
        //     dependsOn(commonMain)
        //     iosX64Main.dependsOn(this)
        //     iosArm64Main.dependsOn(this)
        //     iosSimulatorArm64Main.dependsOn(this)
        // }

        // macOS Source Set (TODO: Implement after macOS actual classes are created)
        // val macosX64Main by getting
        // val macosArm64Main by getting
        // val macosMain by creating {
        //     dependsOn(commonMain)
        //     macosX64Main.dependsOn(this)
        //     macosArm64Main.dependsOn(this)
        // }
    }
}

android {
    namespace = "com.augmentalis.universal.assetmanager"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// Task to copy example assets
tasks.register<Copy>("copyExampleAssets") {
    description = "Copy example asset files to the Assets directory"
    group = "setup"

    from("src/commonMain/resources/examples")
    into("${rootProject.projectDir}/Universal/Assets/Examples")
}

// Task to validate manifest files
tasks.register("validateManifests") {
    description = "Validate all manifest.json files"
    group = "verification"

    doLast {
        val assetsDir = file("${rootProject.projectDir}/Universal/Assets")
        if (!assetsDir.exists()) {
            logger.warn("Assets directory does not exist: ${assetsDir.absolutePath}")
            return@doLast
        }

        val manifestFiles = fileTree(assetsDir).matching {
            include("**/manifest.json")
        }

        manifestFiles.forEach { manifestFile ->
            logger.lifecycle("Validating: ${manifestFile.relativeTo(assetsDir).path}")
            try {
                // Basic JSON validation
                val content = manifestFile.readText()
                if (!content.trim().startsWith("{") || !content.trim().endsWith("}")) {
                    logger.error("Invalid JSON structure in ${manifestFile.name}")
                }
            } catch (e: Exception) {
                logger.error("Error validating ${manifestFile.name}: ${e.message}")
            }
        }
    }
}

// Clean task for asset directories
tasks.register<Delete>("cleanAssets") {
    description = "Clean generated asset files (keeps manifests)"
    group = "cleanup"

    delete(fileTree("${rootProject.projectDir}/Universal/Assets/Icons") {
        exclude("**/manifest.json")
    })
    delete(fileTree("${rootProject.projectDir}/Universal/Assets/Images") {
        exclude("**/manifest.json")
    })
}
