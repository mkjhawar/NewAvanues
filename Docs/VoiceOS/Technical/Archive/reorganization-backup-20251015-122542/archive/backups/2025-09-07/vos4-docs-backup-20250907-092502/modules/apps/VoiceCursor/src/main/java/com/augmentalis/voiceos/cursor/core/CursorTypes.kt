/**
 * Types.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/core/Types.kt
 * 
 * Created: 2025-01-23 00:15 PST
 * Last Modified: 2025-01-26 00:00 PST
 * Author: VOS4 Development Team
 * Version: 1.2.0
 * 
 * Purpose: Core cursor data types with zero-overhead design
 * Module: VoiceCursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-23 00:15 PST): Initial creation
 * - v1.1.0 (2025-01-23 00:33 PDT): Updated package name to com.augmentalis.voiceos.cursor
 * - v1.2.0 (2025-01-26 00:00 PST): Migrated to VoiceCursor module with namespace com.augmentalis.voiceos.voicecursor
 */

package com.augmentalis.voiceos.cursor.core

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Core cursor types and data classes
 * Zero-overhead using sealed classes and data classes
 */

@Parcelize
sealed class CursorType : Parcelable {
    @Parcelize
    object Hand : CursorType()
    @Parcelize
    object Normal : CursorType()
    @Parcelize
    object Custom : CursorType()
    
    val name: String
        get() = when (this) {
            is Hand -> "Hand"
            is Normal -> "Normal"
            is Custom -> "Custom"
        }
    
    fun toggle(): CursorType = when (this) {
        is Hand -> Normal
        is Normal -> Hand
        is Custom -> Normal
    }
}

/**
 * Cursor position with zero allocation overhead
 */
@JvmInline
value class CursorOffset(private val packed: Long) {
    constructor(x: Float, y: Float) : this(
        (x.toBits().toLong() shl 32) or (y.toBits().toLong() and 0xFFFFFFFFL)
    )
    
    val x: Float get() = Float.fromBits((packed shr 32).toInt())
    val y: Float get() = Float.fromBits(packed.toInt())
    
    fun copy(x: Float = this.x, y: Float = this.y): CursorOffset {
        return CursorOffset(x, y)
    }
}

/**
 * Cursor configuration with ARVision theme defaults
 */
@Parcelize
data class CursorConfig(
    val type: CursorType = CursorType.Normal,
    val color: Int = 0xFF007AFF.toInt(), // ARVision systemBlue
    val size: Int = 48, // ARVision standard touch target
    val handCursorSize: Int = 48,
    val speed: Int = 8,
    val strokeWidth: Float = 2.0f, // ARVision thin border
    val cornerRadius: Float = 20.0f, // ARVision rounded corners
    val glassOpacity: Float = 0.8f, // ARVision glass morphism
    val gazeClickDelay: Long = 1500L, // Gaze dwell time in milliseconds
    val showCoordinates: Boolean = false, // Display cursor X,Y coordinates
    
    // CursorFilter configuration
    val jitterFilterEnabled: Boolean = true, // Enable jitter filtering
    val filterStrength: FilterStrength = FilterStrength.Medium, // Filter strength level
    val motionSensitivity: Float = 0.7f // Motion sensitivity (0.1-1.0, higher = more sensitive)
) : Parcelable

/**
 * Filter strength levels for user-friendly selection
 */
@Parcelize
enum class FilterStrength : Parcelable {
    Low,    // 30% filtering strength
    Medium, // 60% filtering strength  
    High;   // 90% filtering strength
    
    /**
     * Convert to numeric strength value (0-100)
     */
    val numericValue: Int
        get() = when (this) {
            Low -> 30
            Medium -> 60
            High -> 90
        }
}

/**
 * Gaze configuration
 */
data class GazeConfig(
    val autoClickTimeMs: Long = 1500L,
    val cancelDistance: Double = 50.0,
    val lockCancelDistance: Double = 420.0,
    val centerDistanceTolerance: Double = 6.0,
    val timeTolerance: Long = 200_000_000L
)

/**
 * Cursor state container
 */
data class CursorState(
    val position: CursorOffset = CursorOffset(0f, 0f),
    val isLocked: Boolean = false,
    val lockedPosition: CursorOffset = CursorOffset(0f, 0f),
    val isVisible: Boolean = true,
    val isGazeActive: Boolean = false
)