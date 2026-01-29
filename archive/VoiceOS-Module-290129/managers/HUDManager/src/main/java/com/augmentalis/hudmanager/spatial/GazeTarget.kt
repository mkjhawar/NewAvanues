/**
 * GazeTarget.kt
 * Path: /managers/HUDManager/src/main/java/com/augmentalis/hudmanager/spatial/GazeTarget.kt
 * 
 * Created: 2025-09-06
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Simple gaze target for VoiceAccessibility integration with x, y coordinates
 * Module: HUDManager - Spatial
 */

package com.augmentalis.hudmanager.spatial

/**
 * Simple gaze target with normalized coordinates (-1 to 1)
 * Used by VoiceAccessibility for gaze tracking
 */
data class GazeTarget(
    val x: Float,
    val y: Float,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Convert to screen coordinates
     */
    fun toScreenCoordinates(screenWidth: Float, screenHeight: Float): Pair<Float, Float> {
        val screenX = (x + 1f) * 0.5f * screenWidth
        val screenY = (y + 1f) * 0.5f * screenHeight
        return Pair(screenX, screenY)
    }
    
    /**
     * Check if this target is stable compared to another target
     */
    fun isStable(other: GazeTarget, threshold: Float = 0.05f): Boolean {
        val deltaX = x - other.x
        val deltaY = y - other.y
        val distance = kotlin.math.sqrt(deltaX * deltaX + deltaY * deltaY)
        return distance < threshold
    }
}