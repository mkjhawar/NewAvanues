/**
 * AvaUI XR Module
 *
 * Platform-agnostic XR abstractions for AR/VR session management.
 * Contains base classes and interfaces that are implemented by platform-specific code.
 *
 * Components:
 * - CommonXRManager: Main XR session interface
 * - CommonCameraManager: Camera lifecycle management
 * - CommonSessionManager: Session state machine
 * - CommonPerformanceMonitor: Performance/battery/thermal monitoring
 * - CommonPermissionManager: Permission handling
 * - XRState: Data models for XR state
 */
plugins {
    id("com.augmentalis.kmp.library")
}

kotlin {
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.augmentalis.avanueui.xr"
}
