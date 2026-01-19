/**
 * PositionModel.kt - Legacy position model
 *
 * Cross-platform KMP model for legacy position information.
 * Consider using AvidPosition for new code.
 */
package com.augmentalis.avidcreator

/**
 * Legacy position information
 * @deprecated Use AvidPosition instead
 */
@Deprecated("Use AvidPosition instead", ReplaceWith("AvidPosition"))
data class Position(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
    val depth: Float = 0f,
    val index: Int = 0,
    val row: Int = 0,
    val column: Int = 0
)
