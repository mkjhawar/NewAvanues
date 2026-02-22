/**
 * CommonXRManager.kt - Platform-agnostic interface for XR management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Migrated to AvaUI/XR)
 *
 * Originally from: Avanues/Web/common/webavanue/universal
 *
 * Defines the contract for XR session management that can be implemented
 * differently on each platform (ARCore on Android, ARKit on iOS, WebXR on Web).
 */
package com.augmentalis.avamagic.xr

import kotlinx.coroutines.flow.StateFlow

/**
 * XRManagerCapabilities - Platform XR capabilities
 *
 * Describes what XR features are available on the current platform.
 */
data class XRManagerCapabilities(
    val supportsAR: Boolean = false,
    val supportsVR: Boolean = false,
    val supportsWebXR: Boolean = false,
    val supportsHandTracking: Boolean = false,
    val supportsPlaneDetection: Boolean = false,
    val supportsLightEstimation: Boolean = false,
    val supportsDepthSensing: Boolean = false
)

/**
 * XRSessionConfig - Configuration for starting an XR session
 */
data class XRSessionConfig(
    val mode: SessionMode,
    val requiredFeatures: Set<String> = emptySet(),
    val optionalFeatures: Set<String> = emptySet()
)

/**
 * CommonXRManager - Platform-agnostic interface for XR management
 *
 * Defines the contract for XR session management that can be implemented
 * differently on each platform (ARCore on Android, ARKit on iOS, WebXR on Web).
 */
interface CommonXRManager {

    /**
     * Current XR state (reactive)
     */
    val xrState: StateFlow<XRState>

    /**
     * Platform capabilities
     */
    val capabilities: XRManagerCapabilities

    /**
     * Check if platform supports the requested XR mode
     *
     * @param mode XR mode to check
     * @return true if supported
     */
    fun isSupported(mode: SessionMode): Boolean

    /**
     * Request XR session with configuration
     *
     * @param config Session configuration
     * @return true if session request was initiated
     */
    suspend fun requestSession(config: XRSessionConfig): Boolean

    /**
     * End current XR session
     */
    suspend fun endSession()

    /**
     * Pause current XR session
     */
    fun pauseSession()

    /**
     * Resume paused XR session
     */
    fun resumeSession()

    /**
     * Check if auto-pause should occur (based on performance)
     *
     * @return true if session should be paused
     */
    fun shouldAutoPause(): Boolean

    /**
     * Clean up resources
     */
    fun dispose()
}

/**
 * XRPermissionState - Current permission status
 */
enum class XRPermissionState {
    /** Permission not yet requested */
    NOT_REQUESTED,

    /** Permission request in progress */
    REQUESTING,

    /** Permission granted */
    GRANTED,

    /** Permission denied */
    DENIED,

    /** Permission denied permanently (user selected "Don't ask again") */
    DENIED_PERMANENTLY
}

/**
 * XRFeature - Standard XR features that can be requested
 */
object XRFeature {
    // Core features
    const val LOCAL = "local"
    const val LOCAL_FLOOR = "local-floor"
    const val BOUNDED_FLOOR = "bounded-floor"
    const val UNBOUNDED = "unbounded"
    const val VIEWER = "viewer"

    // AR features
    const val HIT_TEST = "hit-test"
    const val ANCHORS = "anchors"
    const val PLANE_DETECTION = "plane-detection"
    const val MESH_DETECTION = "mesh-detection"
    const val LIGHT_ESTIMATION = "light-estimation"
    const val DEPTH_SENSING = "depth-sensing"
    const val DOM_OVERLAY = "dom-overlay"
    const val CAMERA_ACCESS = "camera-access"

    // Hand tracking
    const val HAND_TRACKING = "hand-tracking"

    // Layers
    const val LAYERS = "layers"
}

/**
 * Factory function for creating platform-specific XR manager
 *
 * Usage:
 * ```
 * val xrManager = createXRManager(context, lifecycle)
 * ```
 */
expect fun createXRManager(platformContext: Any): CommonXRManager
