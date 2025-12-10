// Author: Manoj Jhawar
// Purpose: Ultra-efficient adaptive jitter elimination for cursor movement

package com.augmentalis.voiceos.cursor.filter

import kotlin.math.abs

/**
 * Adaptive cursor filter that eliminates jitter while maintaining responsiveness
 * Uses integer math and minimal memory for <0.1ms processing overhead
 */
class CursorFilter {
    
    companion object {
        // Motion thresholds (pixels per second)
        private const val STATIONARY_THRESHOLD = 20  // <20 px/s = stationary (more sensitive)
        private const val SLOW_THRESHOLD = 100       // <100 px/s = slow movement (smoother transition)
        
        // Filter strengths (0-100 scale for integer math)
        private const val STATIONARY_STRENGTH = 75   // 75% filtering when still (less aggressive)
        private const val SLOW_STRENGTH = 30         // 30% filtering when slow (much smoother)
        private const val FAST_STRENGTH = 5          // 5% filtering when fast (more responsive)
        
        // Timing
        private const val MIN_FRAME_TIME_NS = 8_000_000L  // 8ms (125fps for smoother motion)
    }
    
    // Minimal state - only 3 variables for <1KB memory
    private var lastX = 0f
    private var lastY = 0f
    private var lastTime = 0L
    private var motionLevel = 0f  // Smoothed motion estimate
    
    // Performance flag and configurable settings
    private var isEnabled = true
    private var stationaryThreshold = STATIONARY_THRESHOLD
    private var slowThreshold = SLOW_THRESHOLD
    private var stationaryStrength = STATIONARY_STRENGTH
    private var slowStrength = SLOW_STRENGTH
    private var fastStrength = FAST_STRENGTH
    private var motionSensitivity = 1.0f
    
    /**
     * Filter cursor position with adaptive smoothing
     * @param x Current X position
     * @param y Current Y position  
     * @param timestamp Current timestamp in nanoseconds
     * @return Filtered position
     */
    fun filter(x: Float, y: Float, timestamp: Long): Pair<Float, Float> {
        // Pass through if disabled
        if (!isEnabled) return Pair(x, y)
        
        // Initialize on first call
        if (lastTime == 0L) {
            lastX = x
            lastY = y
            lastTime = timestamp
            return Pair(x, y)
        }
        
        // Skip if called too frequently
        val deltaTime = timestamp - lastTime
        if (deltaTime < MIN_FRAME_TIME_NS) {
            return Pair(lastX, lastY)
        }
        
        // Calculate instantaneous motion (pixels per second)
        val deltaTimeSeconds = deltaTime / 1_000_000_000f
        val dx = abs(x - lastX)
        val dy = abs(y - lastY)
        val instantMotion = ((dx + dy) / deltaTimeSeconds) * motionSensitivity
        
        // Update smoothed motion estimate (90% old, 10% new)
        motionLevel = (motionLevel * 90 + instantMotion * 10) / 100
        
        // Select filter strength based on motion level
        val strength = when {
            motionLevel < stationaryThreshold -> stationaryStrength
            motionLevel < slowThreshold -> slowStrength
            else -> fastStrength
        }
        
        // Apply filter using integer math for efficiency
        val filteredX = ((x * (100 - strength) + lastX * strength) / 100).toInt().toFloat()
        val filteredY = ((y * (100 - strength) + lastY * strength) / 100).toInt().toFloat()
        
        // Update state
        lastX = filteredX
        lastY = filteredY
        lastTime = timestamp
        
        return Pair(filteredX, filteredY)
    }
    
    /**
     * Reset filter state (e.g., when cursor is recentered)
     */
    fun reset() {
        lastX = 0f
        lastY = 0f
        lastTime = 0L
        motionLevel = 0f
    }
    
    /**
     * Enable or disable filtering
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            reset()
        }
    }
    
    /**
     * Get current motion level for debugging/UI
     * @return Motion level in pixels per second
     */
    fun getMotionLevel(): Float = motionLevel
    
    /**
     * Get current filter strength for debugging/UI
     * @return Filter strength 0-100
     */
    fun getCurrentStrength(): Int {
        return when {
            motionLevel < stationaryThreshold -> stationaryStrength
            motionLevel < slowThreshold -> slowStrength
            else -> fastStrength
        }
    }
    
    /**
     * Update filter configuration
     */
    fun updateConfig(
        stationaryThreshold: Int = this.stationaryThreshold,
        slowThreshold: Int = this.slowThreshold,
        stationaryStrength: Int = this.stationaryStrength,
        slowStrength: Int = this.slowStrength,
        fastStrength: Int = this.fastStrength,
        motionSensitivity: Float = this.motionSensitivity
    ) {
        this.stationaryThreshold = stationaryThreshold
        this.slowThreshold = slowThreshold
        this.stationaryStrength = stationaryStrength
        this.slowStrength = slowStrength
        this.fastStrength = fastStrength
        this.motionSensitivity = motionSensitivity
    }
}

/**
 * Configuration for CursorFilter
 */
data class CursorFilterConfig(
    val enabled: Boolean = true,
    val stationaryThreshold: Int = 50,
    val slowThreshold: Int = 200,
    val stationaryStrength: Int = 90,
    val slowStrength: Int = 50,
    val fastStrength: Int = 10
)