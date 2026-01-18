/**
 * Position.kt - Spatial position from legacy UIKitVoiceCommandSystem
 *
 * EXACT port of Position data class from working legacy implementation
 */

package com.augmentalis.avidcreator.models

/**
 * Spatial position - EXACT copy from legacy implementation
 *
 * @param x X coordinate
 * @param y Y coordinate
 * @param z Z coordinate (default 0)
 * @param index Element index in list
 * @param row Row position in grid
 * @param column Column position in grid
 */
data class Position(
    val x: Float,
    val y: Float,
    val z: Float = 0f,
    val index: Int = 0,
    val row: Int = 0,
    val column: Int = 0
)
