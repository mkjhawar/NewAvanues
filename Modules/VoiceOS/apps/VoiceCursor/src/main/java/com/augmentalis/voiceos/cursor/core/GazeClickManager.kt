/**
 * GazeClickManager.kt
 * Path: /apps/VoiceCursor/src/main/java/com/augmentalis/voiceos/voicecursor/core/GazeClickManager.kt
 * 
 * Created: 2025-01-23 00:15 PST
 * Last Modified: 2025-01-23 00:33 PDT
 * Author: VOS4 Development Team
 * Version: 2.1.0
 * 
 * Purpose: Thread-safe gaze detection and auto-click functionality
 * Module: Cursor System
 * 
 * Changelog:
 * - v1.0.0 (2025-01-23 00:15 PST): Initial port from VoiceOS
 * - v2.0.0 (2025-01-23 00:26 PDT): Fixed thread safety, state management, resource cleanup
 * - v2.1.0 (2025-01-23 00:33 PDT): Updated package name to com.augmentalis.voiceos.cursor
 * - v3.0.0 (2025-01-26): Integrated into VoiceCursor module
 */

package com.augmentalis.voiceos.cursor.core

import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Manages gaze detection and auto-click functionality
 * Single Responsibility: Handle gaze-based interactions
 * Thread-safe implementation with proper state management
 */
class GazeClickManager(
    private val config: GazeConfig = GazeConfig()
) {
    companion object {
        private const val GAZE_TIME_TOLERANCE = 200_000_000L // .2 seconds
    }
    
    // FIX: Thread-safe state with @Volatile
    @Volatile private var isGazeActive = false
    @Volatile private var isGazeClick = false
    @Volatile private var autoClickStartMs: Long = 0
    @Volatile private var lastGazeClick = false
    
    // FIX: Thread-safe position tracking
    @Volatile private var lastGazeX = 0f
    @Volatile private var lastGazeY = 0f
    @Volatile private var centerX = 0f
    @Volatile private var centerY = 0f
    @Volatile private var centerTs: Long = 0
    
    // Animation state
    @Volatile private var currentFrameIndex = 0
    @Volatile private var lastFrameTime = 0L
    @Volatile private var frameStartMs: Long = 0
    
    // FIX: Synchronization lock for complex operations
    private val stateLock = Any()
    
    /**
     * Check if gaze should trigger auto-click
     * FIX: Thread-safe with synchronization
     */
    fun checkGazeClick(
        currentX: Float,
        currentY: Float,
        timestamp: Long,
        isOverlayShown: Boolean
    ): GazeResult {
        synchronized(stateLock) {
            val ghostX = lastGazeX
            val ghostY = lastGazeY
            
            // Calculate distance from last gaze position
            val distance = hypot((currentX - ghostX).toDouble(), (currentY - ghostY).toDouble())
            
            // Set cancel distance based on overlay state
            val cancelDistance = if (isOverlayShown) {
                config.cancelDistance
            } else {
                config.lockCancelDistance
            }
            
            // Reset if moved too far
            if (distance > cancelDistance) {
                resetGazeClickInternal()
                return GazeResult(false, false)
            }
            
            // Check if enough time has passed for auto-click
            val elapsedGazeTime = System.currentTimeMillis() - autoClickStartMs
            
            if (isGazeClick && 
                elapsedGazeTime >= config.autoClickTimeMs && 
                isOverlayShown) {
                
                resetGazeClickInternal()
                autoClickStartMs = System.currentTimeMillis()
                return GazeResult(true, true) // Trigger click
            }
            
            // Continue tracking gaze
            if (isOverlayShown) {
                handleGazeInternal(currentX, currentY, timestamp)
            }
            
            return GazeResult(false, isGazeClick)
        }
    }
    
    /**
     * Handle gaze tracking for steady focus detection
     * FIX: Now called within synchronized block
     */
    private fun handleGazeInternal(x: Float, y: Float, timestamp: Long) {
        // FIX: Better state change detection
        val stateChanged = (lastGazeClick != isGazeClick)
        
        // Only reset if transitioning from non-click to potential click
        if (stateChanged && !lastGazeClick && isGazeClick) {
            resetGazeCenterInternal(x, y, timestamp)
        }
        
        lastGazeClick = isGazeClick
        
        // Calculate distance from gaze center
        val centerDistance = sqrt(
            ((x - centerX) * (x - centerX) + (y - centerY) * (y - centerY)).toDouble()
        )
        
        // Reset center if moved too far
        if (centerDistance > config.centerDistanceTolerance) {
            resetGazeCenterInternal(x, y, timestamp)
        } else {
            // Check how long gaze has been focused on center
            val timeFocused = timestamp - centerTs
            if (timeFocused > GAZE_TIME_TOLERANCE) {
                resetGazeCenterInternal(x, y, timestamp)
                if (!isGazeClick) {
                    setGazeClickInternal(true) // Enable auto-click
                }
            }
        }
        
        // Update last gaze position
        lastGazeX = x
        lastGazeY = y
    }
    
    /**
     * Enable gaze tracking
     * FIX: Thread-safe state modification
     */
    fun enableGaze() {
        synchronized(stateLock) {
            isGazeActive = true
            frameStartMs = System.currentTimeMillis()
        }
    }
    
    /**
     * Disable gaze tracking
     * FIX: Thread-safe state modification
     */
    fun disableGaze() {
        synchronized(stateLock) {
            isGazeActive = false
            resetGazeClickInternal()
        }
    }
    
    /**
     * Get current animation frame for gaze visualization
     * FIX: Thread-safe frame calculation
     */
    fun getAnimationFrame(frameCount: Int, frameDuration: Long): Int {
        synchronized(stateLock) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastFrameTime >= frameDuration) {
                currentFrameIndex = (currentFrameIndex + 1) % frameCount
                lastFrameTime = currentTime
            }
            return currentFrameIndex
        }
    }
    
    fun isGazeActive(): Boolean = isGazeActive
    
    fun isGazeClickActive(): Boolean = isGazeClick
    
    /**
     * FIX: Internal methods for use within synchronized blocks
     */
    private fun resetGazeCenterInternal(x: Float, y: Float, timestamp: Long) {
        centerTs = timestamp
        centerX = x
        centerY = y
    }
    
    private fun setGazeClickInternal(active: Boolean) {
        isGazeClick = active
        if (active) {
            autoClickStartMs = System.currentTimeMillis()
        }
    }
    
    private fun resetGazeClickInternal() {
        isGazeClick = false
    }
    
    /**
     * FIX: Added cleanup method for resource management
     */
    fun dispose() {
        synchronized(stateLock) {
            isGazeActive = false
            isGazeClick = false
            lastGazeClick = false
            
            // Reset all tracking values
            lastGazeX = 0f
            lastGazeY = 0f
            centerX = 0f
            centerY = 0f
            centerTs = 0
            autoClickStartMs = 0
            
            // Reset animation state
            currentFrameIndex = 0
            lastFrameTime = 0
            frameStartMs = 0
        }
    }
    
    data class GazeResult(
        val shouldClick: Boolean,
        val isTracking: Boolean
    )
}