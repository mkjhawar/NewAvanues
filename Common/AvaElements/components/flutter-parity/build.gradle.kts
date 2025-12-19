plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("maven-publish")
    id("app.cash.paparazzi") version "1.3.1"
    id("org.jlleitschuh.gradle.ktlint")
    id("org.jetbrains.kotlinx.kover")
}

kotlin {
    android {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    // iOS targets for Kotlin/Native
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":Universal:Libraries:AvaElements:Core"))
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation("app.cash.paparazzi:paparazzi:1.3.1")
                implementation("junit:junit:4.13.2")
                implementation("androidx.compose.ui:ui-test-junit4:1.6.0")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.compose.foundation:foundation:1.6.0")
                implementation("androidx.compose.material3:material3:1.2.0")
                implementation("androidx.compose.animation:animation:1.6.0")
            }
        }

        // iOS source sets
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
        }

        val iosArm64Test by getting
        val iosSimulatorArm64Test by getting
        val iosTest by creating {
            dependsOn(commonTest)
            iosArm64Test.dependsOn(this)
            iosSimulatorArm64Test.dependsOn(this)
        }
    }
}

android {
    namespace = "com.augmentalis.avaelements.flutter"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

// Maven Publishing Configuration
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.augmentalis.avaelements"
            artifactId = "flutter-parity"
            version = "1.0.0"

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("AvaElements Flutter Parity Components")
                description.set("Flutter-like components for Jetpack Compose with 100% API parity")
                url.set("https://github.com/augmentalis/avanues")

                licenses {
                    license {
                        name.set("Proprietary")
                        url.set("https://augmentalis.com/license")
                    }
                }

                developers {
                    developer {
                        id.set("manoj")
                        name.set("Manoj Jhawar")
                        email.set("manoj@ideahq.net")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/augmentalis/avanues.git")
                    developerConnection.set("scm:git:ssh://github.com/augmentalis/avanues.git")
                    url.set("https://github.com/augmentalis/avanues")
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/augmentalis/avanues")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }

        maven {
            name = "Local"
            url = uri("${rootProject.buildDir}/repo")
        }
    }
}

// Ktlint Configuration
ktlint {
    version.set("1.0.1")
    android.set(true)
    outputToConsole.set(true)
    coloredOutput.set(true)
    verbose.set(true)
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}

// Kover (Code Coverage) Configuration
koverReport {
    filters {
        excludes {
            classes(
                "**/BuildConfig*",
                "**/*Test*",
                "**/*\$*"
            )
        }
    }

    defaults {
        xml {
            onCheck = true
        }
        html {
            onCheck = true
        }
        verify {
            onCheck = true
            rule {
                bound {
                    minValue = 90
                    metric = kotlinx.kover.gradle.plugin.dsl.MetricType.LINE
                    aggregation = kotlinx.kover.gradle.plugin.dsl.AggregationType.COVERED_PERCENTAGE
                }
            }
        }
    }
}

// Version management tasks
tasks.register("printVersion") {
    group = "versioning"
    description = "Print the current version"
    doLast {
        println("Version: 1.0.0")
    }
}

// Quality gates task
tasks.register("qualityGates") {
    group = "verification"
    description = "Run all quality gate checks"
    dependsOn(
        "ktlintCheck",
        "lintDebug",
        "testDebugUnitTest",
        "koverVerify"
    )
}
