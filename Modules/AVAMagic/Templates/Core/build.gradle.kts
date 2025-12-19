plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // TODO: Re-enable for multiplatform
    // jvm("desktop")

    // TODO: Re-enable for multiplatform
    // listOf(
    //     iosX64(),
    //     iosArm64(),
    //     iosSimulatorArm64()
    // ).forEach { iosTarget ->
    //     iosTarget.binaries.framework {
    //         baseName = "TemplatesCore"
    //         isStatic = true
    //     }
    // }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // IDEAMagic dependencies
                implementation(project(":modules:AVAMagic:UI:Core"))
                implementation(project(":modules:AVAMagic:Code:Forms"))
                implementation(project(":modules:AVAMagic:Code:Workflows"))
                implementation(project(":modules:AVAMagic:Data"))

                // Kotlin standard library
                implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")

                // Kotlinx libraries
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
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
                implementation("androidx.core:core-ktx:1.12.0")
            }
        }

        // TODO: Re-enable desktop source set when target is re-enabled
        // val desktopMain by getting {
            // dependencies {
                // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
            // }
        // }

        // TODO: Re-enable iOS source sets when targets are re-enabled
        // val iosX64Main by getting
        // val iosArm64Main by getting
        // val iosSimulatorArm64Main by getting
        // val iosMain by creating { // TODO: Re-enable
            // dependsOn(commonMain) // TODO: Re-enable
            // iosX64Main.dependsOn(this)
            // iosArm64Main.dependsOn(this)
            // iosSimulatorArm64Main.dependsOn(this)
        // } // TODO: Re-enable
    }
}

android {
    namespace = "com.augmentalis.avanues.avamagic.templates.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}