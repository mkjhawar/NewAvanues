plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
}

group = "com.augmentalis.universal.libraries.avaelements"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

// Configure compose compiler - explicitly set version compatible with Kotlin 1.9.25
compose {
    kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.5.14")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "17"
        }
        withJava()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // MagicElements Core dependency
                implementation(project(":Modules:AVAMagic:MagicUI:Components:Core"))

                // Kotlin standard library
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
            }
        }

        val jvmMain by getting {
            dependencies {
                // Compose Desktop
                implementation(compose.desktop.currentOs)
                implementation(compose.desktop.common)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.materialIconsExtended)
                implementation(compose.ui)
                implementation(compose.runtime)

                // Coroutines for desktop
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

                // File I/O
                implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
            }
        }
    }
}

// Configure Compose
compose.desktop {
    application {
        mainClass = "com.augmentalis.avanues.avamagic.components.themebuilder.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )

            packageName = "MagicElements Theme Builder"
            packageVersion = "1.0.0"
            description = "Visual theme editor for MagicElements UI library"
            vendor = "Augmentalis"

            macOS {
                bundleID = "com.augmentalis.avaelements.themebuilder"
                iconFile.set(project.file("src/jvmMain/resources/icon.icns"))
            }

            windows {
                iconFile.set(project.file("src/jvmMain/resources/icon.ico"))
                menuGroup = "MagicElements"
                upgradeUuid = "A1B2C3D4-E5F6-7890-ABCD-EF1234567890"
            }

            linux {
                iconFile.set(project.file("src/jvmMain/resources/icon.png"))
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.9.25"
        )
    }
}
