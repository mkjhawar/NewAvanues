/**
 * CommonPermissionManager.kt - Platform-agnostic XR permission management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Migrated to AvaUI/XR)
 *
 * Originally from: Avanues/Web/common/webavanue/universal
 *
 * Contains shared permission state models and rationale text generation.
 * Platform implementations handle actual permission requests.
 */
package com.augmentalis.avamagic.xr

/**
 * CommonPermissionManager - Platform-agnostic permission management
 *
 * Contains shared permission state models and rationale text generation.
 * Platform implementations handle actual permission requests.
 */
abstract class CommonPermissionManager {

    /**
     * Permission state for XR features
     */
    enum class PermissionState {
        GRANTED,
        DENIED,
        DENIED_PERMANENTLY,
        NOT_REQUESTED
    }

    /**
     * XR capability check result
     */
    data class XRCapabilityResult(
        val isSupported: Boolean,
        val missingCapabilities: List<String>
    )

    /**
     * Permission result with detailed state
     */
    data class PermissionResult(
        val permissionName: String,
        val state: PermissionState,
        val shouldShowRationale: Boolean = false
    )

    // ========== Shared Rationale Text (100% reusable) ==========

    /**
     * Get user-friendly explanation for camera permission request
     *
     * @param sessionMode "immersive-ar" or "immersive-vr"
     * @return Localized permission explanation
     */
    fun getCameraPermissionRationale(sessionMode: String): String {
        return when (sessionMode) {
            "immersive-ar", SessionMode.IMMERSIVE_AR.name ->
                "Camera access is required to display augmented reality content. " +
                "This allows the website to overlay virtual objects on your real-world view."

            "immersive-vr", SessionMode.IMMERSIVE_VR.name ->
                "This VR experience requires camera access for enhanced tracking features."

            else ->
                "Camera access is required for this immersive web experience."
        }
    }

    /**
     * Get user-friendly error message when permission is denied
     *
     * @param isPermanentlyDenied true if user selected "Don't ask again"
     * @param appName Application name for settings instructions
     * @return User-friendly error message
     */
    fun getPermissionDeniedMessage(isPermanentlyDenied: Boolean, appName: String = "WebAvanue"): String {
        return if (isPermanentlyDenied) {
            "Camera permission is required for AR experiences. " +
            "Please enable it in Settings > Apps > $appName > Permissions."
        } else {
            "Camera permission is required for AR experiences. " +
            "Please grant permission to continue."
        }
    }

    /**
     * Get capability error messages
     */
    fun getCapabilityErrorMessages(): CapabilityMessages = CapabilityMessages

    /**
     * Standard capability error messages
     */
    object CapabilityMessages {
        const val MISSING_SENSORS = "Required sensors (accelerometer and gyroscope) not available"
        const val MISSING_OPENGL = "OpenGL ES 3.0+ not supported (required for WebGL 2.0)"
        const val MISSING_CAMERA = "No camera available for AR sessions"
        const val MISSING_ARCORE = "ARCore/ARKit not available on this device"
        const val MISSING_DEPTH = "Depth sensing not supported on this device"
    }

    // ========== Abstract Methods (Platform-Specific) ==========

    /**
     * Check if camera permission is granted
     */
    abstract fun isCameraPermissionGranted(): Boolean

    /**
     * Check if required sensors (accelerometer, gyroscope) are available
     */
    abstract fun areRequiredSensorsAvailable(): Boolean

    /**
     * Check if OpenGL ES 3.0+ is supported
     */
    abstract fun isOpenGLES3Supported(): Boolean

    /**
     * Check if device has a camera
     */
    abstract fun hasCamera(): Boolean

    /**
     * Check all XR capabilities
     *
     * @return XRCapabilityResult with supported flag and missing capabilities
     */
    fun checkXRCapabilities(): XRCapabilityResult {
        val missing = mutableListOf<String>()

        if (!areRequiredSensorsAvailable()) {
            missing.add(CapabilityMessages.MISSING_SENSORS)
        }
        if (!isOpenGLES3Supported()) {
            missing.add(CapabilityMessages.MISSING_OPENGL)
        }
        if (!hasCamera()) {
            missing.add(CapabilityMessages.MISSING_CAMERA)
        }

        return XRCapabilityResult(
            isSupported = missing.isEmpty(),
            missingCapabilities = missing
        )
    }
}
