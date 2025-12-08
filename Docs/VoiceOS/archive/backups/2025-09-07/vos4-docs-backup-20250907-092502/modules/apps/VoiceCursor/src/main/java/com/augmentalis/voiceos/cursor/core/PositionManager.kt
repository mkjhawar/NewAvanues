/**
 * PositionManager.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/cursor/core/PositionManager.kt
 * 
 * Created: 2025-09-06
 * Author: VOS4 Development Team
 * Version: 1.0.0
 * 
 * Purpose: Legacy compatibility wrapper for position management
 * Module: VoiceCursor System
 */

package com.augmentalis.voiceos.cursor.core

import android.util.Log

/**
 * Legacy compatibility wrapper for position management
 * Provides backward compatibility with older VoiceCursor implementations
 */
class PositionManager(
    private var screenWidth: Int,
    private var screenHeight: Int
) {
    companion object {
        private const val TAG = "PositionManager"
    }
    
    // Current position state
    private var currentX = 0f
    private var currentY = 0f
    
    /**
     * Update screen dimensions
     */
    fun updateScreenDimensions(width: Int, height: Int) {
        screenWidth = width
        screenHeight = height
        Log.d(TAG, "Screen dimensions updated: ${width}x${height}")
    }
    
    /**
     * Center the cursor position
     */
    fun centerCursor() {
        currentX = (screenWidth / 2).toFloat()
        currentY = (screenHeight / 2).toFloat()
        Log.d(TAG, "Cursor centered at ($currentX, $currentY)")
    }
    
    /**
     * Get current cursor position
     */
    fun getCurrentPosition(): CursorOffset {
        return CursorOffset(currentX, currentY)
    }
    
    /**
     * Set cursor position
     */
    fun setPosition(x: Float, y: Float) {
        currentX = x.coerceIn(0f, screenWidth.toFloat())
        currentY = y.coerceIn(0f, screenHeight.toFloat())
    }
    
    /**
     * Update position with delta movement
     */
    fun updatePosition(deltaX: Float, deltaY: Float) {
        currentX = (currentX + deltaX).coerceIn(0f, screenWidth.toFloat())
        currentY = (currentY + deltaY).coerceIn(0f, screenHeight.toFloat())
    }
    
    /**
     * Check if position is within screen bounds
     */
    fun isWithinBounds(x: Float, y: Float): Boolean {
        return x >= 0f && x <= screenWidth && y >= 0f && y <= screenHeight
    }
    
    /**
     * Get screen dimensions
     */
    fun getScreenDimensions(): Pair<Int, Int> {
        return Pair(screenWidth, screenHeight)
    }
    
    /**
     * Dispose resources
     */
    fun dispose() {
        currentX = 0f
        currentY = 0f
        Log.d(TAG, "PositionManager disposed")
    }
}