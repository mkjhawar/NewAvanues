package com.augmentalis.argscanner.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.augmentalis.argscanner.data.Converters
import kotlinx.serialization.Serializable

/**
 * ScannedObject - Represents a detected object from AR scanning
 *
 * Captures object information detected by ML Kit including:
 * - Object classification (label)
 * - Confidence score
 * - 3D position in space
 * - Bounding box dimensions
 * - Associated UUID for voice control
 *
 * Created by: Manoj Jhawar, manoj@ideahq.net
 */
@Entity(tableName = "scanned_objects")
@TypeConverters(Converters::class)
@Serializable
data class ScannedObject(
    @PrimaryKey
    val uuid: String,  // UUID from UUIDCreator for voice control

    val sessionId: String,  // Foreign key to ARScanSession

    // ML Kit Detection Data
    val label: String,  // Object classification (e.g., "chair", "table", "monitor")
    val confidence: Float,  // Detection confidence (0.0 - 1.0)

    // 3D Spatial Data (from ARCore)
    val position: Position3D,  // X, Y, Z coordinates in meters
    val rotation: Rotation3D,  // Pitch, yaw, roll in degrees
    val boundingBox: BoundingBox3D,  // Width, height, depth in meters

    // Metadata
    val timestamp: Long = System.currentTimeMillis(),
    val trackingQuality: TrackingQuality = TrackingQuality.NORMAL,

    // Voice Control
    val voiceName: String? = null,  // Optional custom voice name
    val voiceCommands: List<String> = emptyList(),  // Supported voice commands

    // UI DSL Generation
    val uiComponentType: String? = null,  // Suggested AVAMagic component type
    val uiProperties: Map<String, Any> = emptyMap()  // Component-specific properties
) {
    /**
     * 3D Position in space (meters from origin)
     */
    @Serializable
    data class Position3D(
        val x: Float,  // Left/right
        val y: Float,  // Up/down
        val z: Float   // Forward/backward (depth)
    )

    /**
     * 3D Rotation (degrees)
     */
    @Serializable
    data class Rotation3D(
        val pitch: Float,  // Tilt up/down
        val yaw: Float,    // Turn left/right
        val roll: Float    // Rotation on axis
    )

    /**
     * 3D Bounding Box (meters)
     */
    @Serializable
    data class BoundingBox3D(
        val width: Float,   // X dimension
        val height: Float,  // Y dimension
        val depth: Float    // Z dimension
    )

    /**
     * Tracking quality indicator
     */
    enum class TrackingQuality {
        POOR,      // Low confidence, unstable tracking
        NORMAL,    // Adequate tracking
        GOOD,      // High confidence, stable tracking
        EXCELLENT  // Very high confidence, very stable
    }

    /**
     * Calculate distance from origin (in meters)
     */
    fun distanceFromOrigin(): Float {
        return kotlin.math.sqrt(
            position.x * position.x +
            position.y * position.y +
            position.z * position.z
        )
    }

    /**
     * Calculate volume of bounding box (cubic meters)
     */
    fun volume(): Float {
        return boundingBox.width * boundingBox.height * boundingBox.depth
    }

    /**
     * Check if object is within detection confidence threshold
     */
    fun isConfident(threshold: Float = 0.7f): Boolean {
        return confidence >= threshold
    }

    /**
     * Generate voice-friendly name
     */
    fun getVoiceFriendlyName(): String {
        return voiceName ?: label.replace("_", " ").lowercase()
    }
}
