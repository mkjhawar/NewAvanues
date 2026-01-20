/**
 * XRState.kt - Platform-agnostic XR data models
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Migrated to AvaUI/XR)
 *
 * Originally from: Avanues/Web/common/webavanue/universal
 *
 * These models are shared across all platforms (Android, iOS, Web) and contain
 * no platform-specific dependencies. They represent the pure data layer of the
 * XR system.
 *
 * Models extracted from:
 * - XRManager.XRState
 * - XRPerformanceMonitor.PerformanceMetrics
 * - XRPerformanceMonitor.PerformanceWarning
 * - XRPerformanceMonitor.WarningSeverity
 * - XRPerformanceMonitor.WarningType
 * - XRSessionManager.SessionState
 * - XRSessionManager.SessionMode
 * - XRSessionManager.SessionInfo
 */
package com.augmentalis.avamagic.xr

/**
 * WebXR session state.
 */
enum class SessionState {
    /** No active XR session */
    INACTIVE,

    /** XR session requested, waiting for permission/initialization */
    REQUESTING,

    /** XR session active and running */
    ACTIVE,

    /** XR session paused (app backgrounded) */
    PAUSED,

    /** XR session ended (user exited or error occurred) */
    ENDED
}

/**
 * Type of XR session.
 */
enum class SessionMode {
    /** Augmented Reality - camera-based world tracking */
    IMMERSIVE_AR,

    /** Virtual Reality - 360° immersive content */
    IMMERSIVE_VR,

    /** Inline XR - non-immersive XR content in webpage */
    INLINE,

    /** Unknown or not yet determined */
    UNKNOWN
}

/**
 * Session information.
 */
data class SessionInfo(
    val mode: SessionMode = SessionMode.UNKNOWN,
    val startTime: Long = 0L,
    val durationMillis: Long = 0L,
    val frameRate: Float = 0f
)

/**
 * Performance warning severity levels.
 */
enum class WarningSeverity {
    NONE,       // No warnings
    LOW,        // Minor performance degradation
    MEDIUM,     // Noticeable performance issues
    HIGH,       // Significant issues, consider pausing
    CRITICAL    // Critical issues, must pause
}

/**
 * Performance warning types.
 */
enum class WarningType {
    LOW_FPS,            // Frame rate below 45fps
    BATTERY_LOW,        // Battery below 20%
    BATTERY_CRITICAL,   // Battery below 10%
    THERMAL_WARNING,    // Device getting hot
    THERMAL_CRITICAL,   // Device overheating
    HIGH_DRAIN          // Battery draining too fast
}

/**
 * Performance metrics snapshot.
 */
data class PerformanceMetrics(
    val fps: Float = 0f,
    val averageFps: Float = 0f,
    val minFps: Float = 0f,
    val maxFps: Float = 0f,
    val frameDrops: Int = 0,
    val batteryLevel: Int = 100,
    val batteryTemperature: Float = 0f,
    val thermalStatus: Int = 0, // 0=None, 1=Light, 2=Moderate, 3=Severe, 4=Critical
    val isDraining: Boolean = false,
    val drainRatePerHour: Float = 0f,
    val uptime: Long = 0L // Session uptime in milliseconds
)

/**
 * Performance warning with severity and recommendations.
 */
data class PerformanceWarning(
    val type: WarningType,
    val severity: WarningSeverity,
    val message: String,
    val recommendation: String,
    val timestamp: Long = 0L  // Timestamp should be provided by platform-specific code
)

/**
 * XR State exposed to UI components.
 *
 * This is the main state model that aggregates all XR-related state
 * for consumption by UI layers across all platforms.
 */
data class XRState(
    val isSessionActive: Boolean = false,
    val sessionMode: String? = null,  // "AR", "VR", null
    val sessionState: String = "inactive",  // "active", "paused", "requesting", "inactive"
    val performanceMetrics: PerformanceMetrics = PerformanceMetrics(),
    val warnings: List<PerformanceWarning> = emptyList(),
    val permissionState: String = "UNKNOWN",  // String version of permission state
    val isXREnabled: Boolean = true,
    val isAREnabled: Boolean = true,
    val isVREnabled: Boolean = true
)
