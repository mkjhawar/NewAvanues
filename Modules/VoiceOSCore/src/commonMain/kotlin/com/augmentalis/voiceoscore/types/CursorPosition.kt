package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.currentTimeMillis
import kotlin.math.sqrt

/**
 * Represents a cursor position in screen coordinates.
 *
 * This data class is used throughout the VoiceOSCoreNG cursor system (Phase 11)
 * to track cursor location, calculate distances, and manage bounds constraints.
 *
 * @property x The horizontal coordinate in pixels
 * @property y The vertical coordinate in pixels
 * @property timestamp The time when this position was recorded (milliseconds since epoch)
 */
data class CursorPosition(
    val x: Int,
    val y: Int,
    val timestamp: Long = currentTimeMillis()
) {
    companion object {
        /**
         * Origin position at (0, 0) with timestamp 0.
         * Useful as a default or sentinel value.
         */
        val ORIGIN = CursorPosition(0, 0, 0L)
    }

    /**
     * Calculates the Euclidean distance from this position to another position.
     *
     * Uses the Pythagorean theorem: distance = sqrt(dx^2 + dy^2)
     *
     * @param other The target position to measure distance to
     * @return The distance in pixels as a Double
     */
    fun distanceTo(other: CursorPosition): Double {
        val dx = (other.x - x).toDouble()
        val dy = (other.y - y).toDouble()
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Creates a new CursorPosition offset from this position by the given deltas.
     *
     * The resulting position will have a new timestamp (current time).
     *
     * @param dx The horizontal offset (positive = right, negative = left)
     * @param dy The vertical offset (positive = down, negative = up)
     * @return A new CursorPosition with the offset applied
     */
    fun offset(dx: Int, dy: Int): CursorPosition {
        return CursorPosition(x + dx, y + dy)
    }

    /**
     * Creates a new CursorPosition clamped to the given bounds.
     *
     * Preserves the original timestamp.
     *
     * @param minX The minimum allowed X coordinate (inclusive)
     * @param minY The minimum allowed Y coordinate (inclusive)
     * @param maxX The maximum allowed X coordinate (inclusive)
     * @param maxY The maximum allowed Y coordinate (inclusive)
     * @return A new CursorPosition with coordinates constrained to bounds
     */
    fun clamp(minX: Int, minY: Int, maxX: Int, maxY: Int): CursorPosition {
        return CursorPosition(
            x = x.coerceIn(minX, maxX),
            y = y.coerceIn(minY, maxY),
            timestamp = timestamp
        )
    }

    /**
     * Checks if this position is within a rectangular area starting at origin.
     *
     * The valid range is [0, width) for x and [0, height) for y.
     * This matches typical screen coordinate systems where (0,0) is top-left.
     *
     * @param width The width of the bounding area (exclusive upper bound for x)
     * @param height The height of the bounding area (exclusive upper bound for y)
     * @return true if the position is within bounds, false otherwise
     */
    fun isWithinBounds(width: Int, height: Int): Boolean {
        return x in 0 until width && y in 0 until height
    }
}
