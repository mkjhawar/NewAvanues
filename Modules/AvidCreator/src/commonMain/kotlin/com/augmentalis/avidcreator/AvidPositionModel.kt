/**
 * AvidPositionModel.kt - Spatial position information for AVID elements
 *
 * Cross-platform KMP models for element positioning.
 *
 * Updated: 2026-01-18 - Migrated to KMP
 */
package com.augmentalis.avidcreator

/**
 * Spatial position information for UI elements
 */
data class AvidPosition(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val depth: Float = 0f,
    val index: Int = 0,
    val row: Int = 0,
    val column: Int = 0,
    val bounds: AvidBounds? = null
)

/**
 * Bounding box for precise positioning
 */
data class AvidBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}
