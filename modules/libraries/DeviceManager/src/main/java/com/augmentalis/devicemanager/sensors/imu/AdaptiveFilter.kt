// Author: Manoj Jhawar
// Purpose: Adaptive filtering for IMU data with dynamic noise reduction

package com.augmentalis.devicemanager.sensors.imu

import kotlin.math.*

/**
 * Adaptive filter that adjusts filtering parameters based on motion characteristics
 */
class AdaptiveFilter {
    
    companion object {
        private const val MIN_FILTER_STRENGTH = 0.1f
        private const val MAX_FILTER_STRENGTH = 0.95f
        private const val MOTION_THRESHOLD = 0.5f // rad/s
        private const val JITTER_WINDOW_SIZE = 10
        private const val SMOOTHING_FACTOR = 0.8f
    }
    
    // Filter state
    private var previousOrientation = Quaternion.identity
    private var previousAngularVelocity = Vector3.zero
    private var previousTimestamp = 0L
    
    // Motion detection
    private var motionIntensity = 0f
    private val jitterBuffer = mutableListOf<Float>()
    private val jitterBufferLock = Any()
    private var currentFilterStrength = MIN_FILTER_STRENGTH
    
    // Smoothing parameters
    private var smoothingEnabled = true
    private var adaptiveMode = true
    
    /**
     * Filter orientation data with adaptive smoothing
     */
    fun filterOrientation(orientation: Quaternion, timestamp: Long): Quaternion {
        if (previousTimestamp == 0L) {
            previousTimestamp = timestamp
            previousOrientation = orientation
            return orientation
        }
        
        val deltaTime = (timestamp - previousTimestamp) * 1e-9f
        if (deltaTime <= 0 || deltaTime > 0.5f) {
            previousTimestamp = timestamp
            return orientation
        }
        
        // Calculate angular velocity
        val angularDistance = angularDistance(previousOrientation, orientation)
        val angularVelocity = angularDistance / deltaTime
        
        // Update motion intensity
        updateMotionIntensity(angularVelocity)
        
        // Adapt filter strength based on motion
        if (adaptiveMode) {
            adaptFilterStrength()
        }
        
        // Apply filtering
        val filteredOrientation = if (smoothingEnabled) {
            slerp(previousOrientation, orientation, 1f - currentFilterStrength)
        } else {
            orientation
        }
        
        // Update state
        previousOrientation = filteredOrientation
        previousTimestamp = timestamp
        previousAngularVelocity = Vector3(angularVelocity, 0f, 0f)
        
        return filteredOrientation
    }
    
    /**
     * Filter motion data with adaptive smoothing
     */
    fun filterMotion(motionData: MotionData, timestamp: Long): MotionData {
        val angularVelocity = motionData.angularVelocity
        
        // Apply low-pass filter to angular velocity
        val filteredVelocity = if (smoothingEnabled) {
            val alpha = 1f - currentFilterStrength * 0.5f // Less aggressive for velocity
            lerp(previousAngularVelocity, angularVelocity, alpha)
        } else {
            angularVelocity
        }
        
        previousAngularVelocity = filteredVelocity
        
        return MotionData(
            angularVelocity = filteredVelocity,
            timestamp = timestamp,
            accuracy = motionData.accuracy
        )
    }
    
    /**
     * Update motion intensity estimate
     */
    private fun updateMotionIntensity(angularVelocity: Float) {
        synchronized(jitterBufferLock) {
            // Add to jitter buffer
            jitterBuffer.add(angularVelocity)
            if (jitterBuffer.size > JITTER_WINDOW_SIZE) {
                jitterBuffer.removeAt(0)
            }

            // Calculate motion statistics
            if (jitterBuffer.size >= 3) {
                val mean = jitterBuffer.average().toFloat()
                val variance = jitterBuffer.map { (it - mean) * (it - mean) }.average().toFloat()
                val stdDev = sqrt(variance)

                // Motion intensity combines magnitude and variability
                motionIntensity = mean + stdDev * 0.5f
            }
        }
    }
    
    /**
     * Adapt filter strength based on motion characteristics
     */
    private fun adaptFilterStrength() {
        // Fast motion: reduce filtering
        // Slow/stable motion: increase filtering
        val targetStrength = when {
            motionIntensity < MOTION_THRESHOLD * 0.2f -> MAX_FILTER_STRENGTH
            motionIntensity < MOTION_THRESHOLD * 0.5f -> MAX_FILTER_STRENGTH * 0.7f
            motionIntensity < MOTION_THRESHOLD -> MAX_FILTER_STRENGTH * 0.5f
            motionIntensity < MOTION_THRESHOLD * 2f -> MAX_FILTER_STRENGTH * 0.3f
            else -> MIN_FILTER_STRENGTH
        }
        
        // Smooth filter strength transitions
        currentFilterStrength = currentFilterStrength * SMOOTHING_FACTOR + 
                                targetStrength * (1f - SMOOTHING_FACTOR)
    }
    
    /**
     * Get current filter parameters
     */
    fun getFilterState(): FilterState {
        return synchronized(jitterBufferLock) {
            FilterState(
                filterStrength = currentFilterStrength,
                motionIntensity = motionIntensity,
                jitterLevel = if (jitterBuffer.isNotEmpty()) {
                    jitterBuffer.map { abs(it - jitterBuffer.average()) }.average().toFloat()
                } else 0f,
                smoothingEnabled = smoothingEnabled,
                adaptiveMode = adaptiveMode
            )
        }
    }
    
    /**
     * Configure filter behavior
     */
    fun configure(config: FilterConfig) {
        smoothingEnabled = config.smoothingEnabled
        adaptiveMode = config.adaptiveMode
        if (!adaptiveMode) {
            currentFilterStrength = config.fixedFilterStrength.coerceIn(MIN_FILTER_STRENGTH, MAX_FILTER_STRENGTH)
        }
    }
    
    /**
     * Reset filter state
     */
    fun reset() {
        synchronized(jitterBufferLock) {
            previousOrientation = Quaternion.identity
            previousAngularVelocity = Vector3.zero
            previousTimestamp = 0L
            motionIntensity = 0f
            jitterBuffer.clear()
            currentFilterStrength = MIN_FILTER_STRENGTH
        }
    }
}

/**
 * Filter state information
 */
data class FilterState(
    val filterStrength: Float,
    val motionIntensity: Float,
    val jitterLevel: Float,
    val smoothingEnabled: Boolean,
    val adaptiveMode: Boolean
)

/**
 * Filter configuration
 */
data class FilterConfig(
    val smoothingEnabled: Boolean = true,
    val adaptiveMode: Boolean = true,
    val fixedFilterStrength: Float = 0.5f
)