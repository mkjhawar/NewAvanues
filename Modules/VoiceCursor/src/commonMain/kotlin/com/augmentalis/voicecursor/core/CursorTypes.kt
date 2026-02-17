/**
 * CursorTypes.kt - Core cursor data types (KMP)
 *
 * Platform-agnostic cursor types with zero-overhead design.
 * Android-specific serialization (Parcelable) is in androidMain.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voicecursor.core

import kotlinx.serialization.Serializable

/**
 * Cursor type enumeration
 */
@Serializable
enum class CursorType {
    Hand,
    Normal,
    Custom;

    fun toggle(): CursorType = when (this) {
        Hand -> Normal
        Normal -> Hand
        Custom -> Normal
    }
}

/**
 * 2D position with float coordinates
 */
@Serializable
data class CursorPosition(
    val x: Float = 0f,
    val y: Float = 0f
) {
    companion object {
        val Zero = CursorPosition(0f, 0f)
    }

    operator fun plus(other: CursorPosition): CursorPosition =
        CursorPosition(x + other.x, y + other.y)

    operator fun minus(other: CursorPosition): CursorPosition =
        CursorPosition(x - other.x, y - other.y)

    operator fun times(scalar: Float): CursorPosition =
        CursorPosition(x * scalar, y * scalar)

    fun distanceTo(other: CursorPosition): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}

/**
 * High-performance cursor offset using packed long (zero allocation)
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

    fun toPosition(): CursorPosition = CursorPosition(x, y)

    companion object {
        val Zero = CursorOffset(0f, 0f)
    }
}

/**
 * Filter strength levels for cursor smoothing
 */
@Serializable
enum class FilterStrength {
    Low,    // 30% filtering strength
    Medium, // 60% filtering strength
    High;   // 90% filtering strength

    val numericValue: Int
        get() = when (this) {
            Low -> 30
            Medium -> 60
            High -> 90
        }

    val smoothingFactor: Float
        get() = when (this) {
            Low -> 0.3f
            Medium -> 0.6f
            High -> 0.9f
        }
}

/**
 * Cursor configuration
 */
@Serializable
data class CursorConfig(
    val type: CursorType = CursorType.Normal,
    val color: Long = 0xFF007AFF, // ARVision systemBlue (cursor fill ARGB)
    val size: Int = 48, // ARVision standard touch target
    val handCursorSize: Int = 48,
    val speed: Int = 8,
    val strokeWidth: Float = 2.0f,
    val cornerRadius: Float = 20.0f,
    val glassOpacity: Float = 0.8f,
    val showCoordinates: Boolean = false,

    // Appearance — overlay rendering (defaults match original hardcoded values)
    val borderColor: Long = 0xFFFFFFFF,      // Cursor border (was hardcoded White)
    val dwellRingColor: Long = 0xFF007AFF,   // Dwell progress ring
    val cursorAlpha: Int = 200,              // Fill opacity 0-255
    val borderStrokeWidth: Float = 3f,       // Border width
    val dwellRingStrokeWidth: Float = 4f,    // Dwell ring width
    val cursorRadius: Float = 12f,           // Cursor dot radius

    // Dwell click settings
    val dwellClickEnabled: Boolean = true,
    val dwellClickDelayMs: Long = 1500L,

    // Filter settings
    val jitterFilterEnabled: Boolean = true,
    val filterStrength: FilterStrength = FilterStrength.Medium,
    val motionSensitivity: Float = 0.7f // 0.1-1.0
)

/**
 * Gaze/dwell click configuration
 */
@Serializable
data class GazeConfig(
    val autoClickTimeMs: Long = 1500L,
    val cancelDistance: Float = 50f,
    val lockCancelDistance: Float = 420f,
    val centerDistanceTolerance: Float = 6f,
    val timeTolerance: Long = 200L // milliseconds
)

/**
 * Cursor state container
 */
@Serializable
data class CursorState(
    val position: CursorPosition = CursorPosition.Zero,
    val isLocked: Boolean = false,
    val lockedPosition: CursorPosition = CursorPosition.Zero,
    val isVisible: Boolean = true,
    val isGazeActive: Boolean = false,
    val isDwellInProgress: Boolean = false,
    val dwellProgress: Float = 0f // 0.0 to 1.0
)

/**
 * Cursor movement input from various sources
 */
@Serializable
sealed class CursorInput {
    /**
     * IMU-based head movement
     */
    @Serializable
    data class HeadMovement(
        val pitch: Float, // Up/down
        val yaw: Float,   // Left/right
        val roll: Float   // Tilt
    ) : CursorInput()

    /**
     * Eye gaze position
     */
    @Serializable
    data class EyeGaze(
        val x: Float,
        val y: Float,
        val confidence: Float = 1f
    ) : CursorInput()

    /**
     * Direct position input (mouse, touch)
     */
    @Serializable
    data class DirectPosition(
        val x: Float,
        val y: Float
    ) : CursorInput()

    /**
     * Relative movement delta
     */
    @Serializable
    data class Delta(
        val dx: Float,
        val dy: Float
    ) : CursorInput()
}

/**
 * Overlay window specification — KMP-shared sizing and positioning.
 *
 * Platforms use this to create a correctly-sized overlay that covers only the
 * cursor dot + dwell ring. The overlay is repositioned as the cursor moves,
 * preventing full-screen overlays that block touch events.
 *
 * @param sizePx Overlay width & height in pixels (square)
 * @param centerOffsetPx Half-size; subtract from cursor position to get top-left origin
 */
data class CursorOverlaySpec(
    val sizePx: Int,
    val centerOffsetPx: Int
) {
    companion object {
        /**
         * Compute overlay dimensions from cursor config and display density.
         *
         * @param config Cursor configuration (radius, stroke widths)
         * @param displayDensity Platform display density (1.0 = mdpi, 2.0 = xhdpi, 3.0 = xxhdpi)
         * @return Spec with pixel-scaled overlay size
         */
        fun fromConfig(config: CursorConfig, displayDensity: Float = 1f): CursorOverlaySpec {
            val scaledRadius = config.cursorRadius * displayDensity
            val dwellRingExtra = (8f + config.dwellRingStrokeWidth) * displayDensity
            val borderExtra = config.borderStrokeWidth * displayDensity
            // Total radius = cursor dot + dwell ring gap + dwell ring stroke + border
            val totalRadius = scaledRadius + dwellRingExtra + borderExtra
            // Add margin for anti-aliasing
            val margin = 4f * displayDensity
            val sizePx = ((totalRadius + margin) * 2f).toInt().coerceAtLeast(1)
            return CursorOverlaySpec(
                sizePx = sizePx,
                centerOffsetPx = sizePx / 2
            )
        }
    }

    /**
     * Compute overlay top-left position for a given cursor position.
     */
    fun overlayOrigin(cursorX: Float, cursorY: Float): CursorPosition {
        return CursorPosition(
            x = cursorX - centerOffsetPx,
            y = cursorY - centerOffsetPx
        )
    }
}

/**
 * Cursor action result
 */
@Serializable
sealed class CursorAction {
    @Serializable
    data object Click : CursorAction()

    @Serializable
    data object DoubleClick : CursorAction()

    @Serializable
    data object LongPress : CursorAction()

    @Serializable
    data class Drag(
        val startPosition: CursorPosition,
        val endPosition: CursorPosition
    ) : CursorAction()

    @Serializable
    data object DwellClick : CursorAction()

    @Serializable
    data object Cancel : CursorAction()
}
