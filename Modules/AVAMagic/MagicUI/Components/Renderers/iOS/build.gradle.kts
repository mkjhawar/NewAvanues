plugins {
    kotlin("multiplatform")
    kotlin("native.cocoapods")
}

kotlin {
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "iOS SwiftUI renderer for MagicElements"
        homepage = "https://github.com/augmentalis/avaelements"
        version = "1.0.0"
        ios.deploymentTarget = "15.0"
        podfile = project.file("../../../../../../../ios/Podfile")

        framework {
            baseName = "MagicElementsRendererIOS"
            isStatic = true
        }
    }

    sourceSets {
        val iosMain by creating {
            dependencies {
                // MagicElements Core
                implementation(project(":modules:AVAMagic:UI:Core"))
            }
        }

        val iosX64Main by getting {
            dependsOn(iosMain)
        }

        val iosArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosSimulatorArm64Main by getting {
            dependsOn(iosMain)
        }

        val iosTest by creating {
            dependencies {
                implementation(kotlin("test"))
            }
        }

        val iosX64Test by getting {
            dependsOn(iosTest)
        }

        val iosArm64Test by getting {
            dependsOn(iosTest)
        }

        val iosSimulatorArm64Test by getting {
            dependsOn(iosTest)
        }
    }
}
