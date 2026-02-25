/**
 * HUDModels.kt
 * 
 * Created: 2025-01-23
 * Version: 1.0.0
 * 
 * Purpose: Data models for HUD system testing and operations
 */

package com.augmentalis.voiceoscore.managers.hudmanager.models

import androidx.compose.ui.graphics.Color
import com.augmentalis.voiceoscore.managers.hudmanager.spatial.SpatialPosition
import com.augmentalis.voiceoscore.managers.hudmanager.spatial.NotificationPriority
import com.augmentalis.voiceoscore.managers.hudmanager.HUDMode

/**
 * HUD system state for testing and monitoring
 */
data class HUDState(
    val mode: HUDMode = HUDMode.STANDARD,
    val isTracking: Boolean = false,
    val activeElements: List<HUDElement> = emptyList(),
    val opacity: Float = 1.0f,
    val isInitialized: Boolean = false,
    val currentUser: String? = null
)

/**
 * Rendering performance statistics
 */
data class RenderingStats(
    val averageFPS: Float = 0f,
    val frameTime: Long = 0L,
    val renderedElements: Int = 0,
    val droppedFrames: Int = 0,
    val memoryUsage: Long = 0L,
    val renderingTime: Long = 0L
)

/**
 * Spatial tracking data
 */
data class SpatialData(
    val headPosition: Vector3D = Vector3D(0f, 0f, 0f),
    val headRotation: Vector3D = Vector3D(0f, 0f, 0f),
    val eyeGaze: Vector3D = Vector3D(0f, 0f, 0f),
    val isCalibrated: Boolean = false,
    val trackingQuality: Float = 0f,
    val lastUpdate: Long = System.currentTimeMillis()
)

/**
 * 3D Vector for positions and rotations
 */
data class Vector3D(
    val x: Float,
    val y: Float, 
    val z: Float
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    
    fun length(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
    fun normalized(): Vector3D {
        val len = length()
        return if (len > 0f) Vector3D(x / len, y / len, z / len) else this
    }
}

/**
 * HUD element for display
 */
data class HUDElement(
    val id: String,
    val type: HUDElementType,
    val position: Vector3D,
    val content: String,
    val priority: Int,
    val bounds: ElementBounds? = null,
    val isVisible: Boolean = true,
    val opacity: Float = 1.0f,
    val scale: Float = 1.0f,
    val rotation: Vector3D = Vector3D(0f, 0f, 0f),
    val color: Color = Color.White,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Types of HUD elements
 */
enum class HUDElementType {
    TEXT_OVERLAY,
    ICON,
    BUTTON,
    PROGRESS_BAR,
    IMAGE,
    VIDEO,
    NOTIFICATION,
    MENU,
    TOOLTIP,
    STATUS_INDICATOR
}

/**
 * Element bounds for collision detection
 */
data class ElementBounds(
    val width: Float,
    val height: Float,
    val depth: Float = 0.1f
)

/**
 * HUD configuration settings
 */
data class HUDConfig(
    val mode: HUDMode = HUDMode.STANDARD,
    val targetFPS: Int = 60,
    val maxElements: Int = 100,
    val trackingEnabled: Boolean = true,
    val optimizationEnabled: Boolean = false,
    val debugMode: Boolean = false
) {
    companion object {
        fun default() = HUDConfig()
    }
}

/**
 * Calibration point for spatial mapping
 */
data class CalibrationPoint(
    val position: Vector3D,
    val label: String,
    val isVerified: Boolean = false,
    val accuracy: Float = 0f
)

/**
 * Element collision data
 */
data class ElementCollision(
    val element1: HUDElement,
    val element2: HUDElement,
    val overlapArea: Float,
    val severity: CollisionSeverity
)

/**
 * Collision severity levels
 */
enum class CollisionSeverity {
    MINOR,
    MODERATE,
    MAJOR,
    CRITICAL
}

/**
 * Voice command structure
 */
data class VoiceCommand(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String = "",
    val label: String = text,  // Display label
    val confidence: Float = 0.9f,
    val category: String = "GENERAL",
    val targetElement: String? = null,
    val translationKey: String? = null  // For localization
)

/**
 * UI Context for voice commands
 */
enum class UIContext {
    BROWSER,
    SETTINGS, 
    MESSAGING,
    HOME_SCREEN,
    NAVIGATION,
    PERSISTENT,
    UNKNOWN
}

/**
 * Gaze tracking target
 */
data class GazeTarget(
    val elementId: String,
    val position: Vector3D,
    val confidence: Float,
    val dwellTime: Long = 0L
)