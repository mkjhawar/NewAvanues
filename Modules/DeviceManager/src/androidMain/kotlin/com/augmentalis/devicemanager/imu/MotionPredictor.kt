// Author: Manoj Jhawar
// Purpose: Motion prediction to compensate for display latency

package com.augmentalis.devicemanager.imu

import kotlin.math.*

/**
 * Predicts future orientation based on current motion to reduce latency
 */
class MotionPredictor {
    
    companion object {
        private const val PREDICTION_TIME_MS = 16f // 60fps frame time
        private const val MAX_PREDICTION_TIME_MS = 33f // Don't predict beyond 2 frames
        private const val VELOCITY_SMOOTHING = 0.8f
        private const val ACCELERATION_SMOOTHING = 0.6f
        private const val MIN_VELOCITY_THRESHOLD = 0.01f // rad/s
    }
    
    // Motion history for velocity calculation
    private var previousOrientation = Quaternion.identity
    private var previousTimestamp = 0L
    
    // Smoothed motion estimates
    private var smoothedAngularVelocity = Vector3.zero
    private var smoothedAngularAcceleration = Vector3.zero
    private var lastAngularVelocity = Vector3.zero
    
    /**
     * Predict orientation at a future time point
     */
    fun predictOrientation(
        currentOrientation: Quaternion, 
        timestamp: Long,
        predictionTimeMs: Float = PREDICTION_TIME_MS
    ): Quaternion {
        
        // Update motion estimates
        updateMotionEstimates(currentOrientation, timestamp)
        
        // Clamp prediction time
        val clampedPredictionTime = predictionTimeMs.coerceIn(0f, MAX_PREDICTION_TIME_MS)
        val predictionTimeSeconds = clampedPredictionTime / 1000f
        
        // Don't predict if velocity is too small (likely noise)
        if (smoothedAngularVelocity.magnitude < MIN_VELOCITY_THRESHOLD) {
            return currentOrientation
        }
        
        // Predict using constant angular velocity + acceleration model
        val predictedVelocity = smoothedAngularVelocity + smoothedAngularAcceleration * predictionTimeSeconds
        val predictedRotation = predictedVelocity * predictionTimeSeconds
        
        // Convert to quaternion rotation
        val rotationMagnitude = predictedRotation.magnitude
        if (rotationMagnitude > 0.001f) {
            val rotationAxis = predictedRotation / rotationMagnitude
            val deltaRotation = Quaternion.fromAxisAngle(rotationAxis, rotationMagnitude)
            return (currentOrientation * deltaRotation).normalized
        }
        
        return currentOrientation
    }
    
    /**
     * Get current angular velocity estimate
     */
    fun getAngularVelocity(): Vector3 = smoothedAngularVelocity
    
    /**
     * Get current angular acceleration estimate  
     */
    fun getAngularAcceleration(): Vector3 = smoothedAngularAcceleration
    
    /**
     * Reset predictor state
     */
    fun reset() {
        previousOrientation = Quaternion.identity
        previousTimestamp = 0L
        smoothedAngularVelocity = Vector3.zero
        smoothedAngularAcceleration = Vector3.zero
        lastAngularVelocity = Vector3.zero
    }
    
    private fun updateMotionEstimates(orientation: Quaternion, timestamp: Long) {
        if (previousTimestamp == 0L) {
            previousOrientation = orientation
            previousTimestamp = timestamp
            return
        }
        
        val deltaTime = (timestamp - previousTimestamp) * 1e-9f // Convert to seconds
        if (deltaTime <= 0 || deltaTime > 0.1f) { // Ignore invalid time deltas
            previousTimestamp = timestamp
            return
        }
        
        // Calculate angular velocity from orientation change
        val deltaRotation = orientation * previousOrientation.inverse
        val rotationAngle = deltaRotation.getAngle()
        
        val instantAngularVelocity = if (rotationAngle > 0.001f && deltaTime > 0) {
            val axis = deltaRotation.getAxis()
            axis * (rotationAngle / deltaTime)
        } else {
            Vector3.zero
        }
        
        // Smooth angular velocity
        smoothedAngularVelocity = smoothedAngularVelocity * VELOCITY_SMOOTHING + 
                                 instantAngularVelocity * (1f - VELOCITY_SMOOTHING)
        
        // Calculate angular acceleration
        val instantAcceleration = if (deltaTime > 0) {
            (smoothedAngularVelocity - lastAngularVelocity) / deltaTime
        } else {
            Vector3.zero
        }
        
        // Smooth angular acceleration
        smoothedAngularAcceleration = smoothedAngularAcceleration * ACCELERATION_SMOOTHING + 
                                     instantAcceleration * (1f - ACCELERATION_SMOOTHING)
        
        // Update history
        lastAngularVelocity = smoothedAngularVelocity
        previousOrientation = orientation
        previousTimestamp = timestamp
    }
}

/**
 * Adaptive filter that adjusts smoothing based on movement characteristics
 */
class MotionFilter {
    
    companion object {
        private const val GENTLE_MOVEMENT_THRESHOLD = 0.1f // rad/s
        private const val RAPID_MOVEMENT_THRESHOLD = 1.0f // rad/s
        private const val JITTER_ANGLE_THRESHOLD = 0.002f // rad (~0.1 degrees)
        private const val JITTER_TIME_WINDOW = 100_000_000L // 100ms in nanoseconds
    }
    
    // Movement classification
    private enum class MovementType {
        GENTLE,      // Slow, deliberate movements
        RAPID,       // Fast movements  
        INTENTIONAL, // Purposeful movements with high acceleration
        JITTER       // Small oscillating movements (noise)
    }
    
    // Filter state
    private var previousOrientation = Quaternion.identity
    private var previousTimestamp = 0L
    private val velocityTracker = AngularVelocityTracker()
    
    // Jitter suppression
    private val recentOrientations = mutableListOf<TimestampedOrientation>()
    private var suppressedOrientation = Quaternion.identity
    private var jitterSuppressionActive = false
    
    /**
     * Filter orientation based on movement characteristics
     */
    fun filterOrientation(orientation: Quaternion, timestamp: Long): Quaternion {
        // Update velocity tracking
        velocityTracker.update(orientation, timestamp)
        
        // Classify movement type
        val movementType = classifyMovement(orientation, timestamp)
        
        // Apply appropriate filtering
        val filtered = when (movementType) {
            MotionFilter.MovementType.GENTLE -> applyHeavySmoothing(orientation, timestamp)
            MotionFilter.MovementType.RAPID -> applyLightSmoothing(orientation, timestamp)
            MotionFilter.MovementType.INTENTIONAL -> applyPredictiveFiltering(orientation, timestamp)
            MotionFilter.MovementType.JITTER -> applyJitterSuppression(orientation, timestamp)
        }
        
        previousOrientation = filtered
        previousTimestamp = timestamp
        
        return filtered
    }
    
    /**
     * Reset filter state
     */
    fun reset() {
        previousOrientation = Quaternion.identity
        previousTimestamp = 0L
        velocityTracker.reset()
        recentOrientations.clear()
        suppressedOrientation = Quaternion.identity
        jitterSuppressionActive = false
    }
    
    private fun classifyMovement(orientation: Quaternion, timestamp: Long): MotionFilter.MovementType {
        val angularVelocity = velocityTracker.getAngularVelocity()
        val angularAcceleration = velocityTracker.getAngularAcceleration()
        
        val velocityMagnitude = angularVelocity.magnitude
        val accelerationMagnitude = angularAcceleration.magnitude
        
        // Check for jitter (small movements back and forth)
        val angleDiff = angularDistance(orientation, previousOrientation)
        if (angleDiff < JITTER_ANGLE_THRESHOLD && isOscillating(timestamp)) {
            return MotionFilter.MovementType.JITTER
        }
        
        return when {
            velocityMagnitude < GENTLE_MOVEMENT_THRESHOLD -> MotionFilter.MovementType.GENTLE
            velocityMagnitude > RAPID_MOVEMENT_THRESHOLD -> MotionFilter.MovementType.RAPID
            accelerationMagnitude > 0.5f -> MotionFilter.MovementType.INTENTIONAL
            else -> MotionFilter.MovementType.GENTLE
        }
    }
    
    private fun applyHeavySmoothing(orientation: Quaternion, timestamp: Long): Quaternion {
        return slerp(previousOrientation, orientation, 0.1f)
    }
    
    private fun applyLightSmoothing(orientation: Quaternion, timestamp: Long): Quaternion {
        return slerp(previousOrientation, orientation, 0.7f)
    }
    
    private fun applyPredictiveFiltering(orientation: Quaternion, timestamp: Long): Quaternion {
        // For intentional movements, use less smoothing to maintain responsiveness
        return slerp(previousOrientation, orientation, 0.8f)
    }
    
    private fun applyJitterSuppression(orientation: Quaternion, timestamp: Long): Quaternion {
        // Add to recent orientations for analysis
        recentOrientations.add(TimestampedOrientation(orientation, timestamp))
        
        // Remove old orientations
        val cutoffTime = timestamp - JITTER_TIME_WINDOW
        recentOrientations.removeAll { it.timestamp < cutoffTime }
        
        // If we have enough samples, check if movement is consistently small
        if (recentOrientations.size >= 5) {
            val maxAngle = recentOrientations.maxOf { angularDistance(it.orientation, previousOrientation) }
            
            if (maxAngle < JITTER_ANGLE_THRESHOLD * 2f) {
                // Likely jitter, hold previous orientation
                jitterSuppressionActive = true
                suppressedOrientation = previousOrientation
                return suppressedOrientation
            }
        }
        
        jitterSuppressionActive = false
        return slerp(previousOrientation, orientation, 0.3f)
    }
    
    private fun isOscillating(timestamp: Long): Boolean {
        if (recentOrientations.size < 3) return false
        
        // Check if recent movements are changing direction (oscillating)
        var directionChanges = 0
        for (i in 1 until minOf(recentOrientations.size, 5)) {
            val current = recentOrientations[i]
            val previous = recentOrientations[i - 1]
            
            // This is a simplified oscillation check
            // A more sophisticated version would analyze velocity direction changes
            val angle = angularDistance(current.orientation, previous.orientation)
            if (angle > 0.001f) {
                directionChanges++
            }
        }
        
        return directionChanges >= 2
    }
    
    private data class TimestampedOrientation(
        val orientation: Quaternion,
        val timestamp: Long
    )
}

/**
 * Tracks angular velocity and acceleration over time
 */
class AngularVelocityTracker {
    
    private var previousOrientation = Quaternion.identity
    private var previousTimestamp = 0L
    private var angularVelocity = Vector3.zero
    private var angularAcceleration = Vector3.zero
    private var previousAngularVelocity = Vector3.zero
    
    fun update(orientation: Quaternion, timestamp: Long) {
        if (previousTimestamp == 0L) {
            previousOrientation = orientation
            previousTimestamp = timestamp
            return
        }
        
        val deltaTime = (timestamp - previousTimestamp) * 1e-9f
        if (deltaTime <= 0 || deltaTime > 0.1f) {
            previousTimestamp = timestamp
            return
        }
        
        // Calculate angular velocity
        val deltaRotation = orientation * previousOrientation.inverse
        val rotationAngle = deltaRotation.getAngle()
        
        angularVelocity = if (rotationAngle > 0.001f) {
            val axis = deltaRotation.getAxis()
            axis * (rotationAngle / deltaTime)
        } else {
            Vector3.zero
        }
        
        // Calculate angular acceleration
        angularAcceleration = (angularVelocity - previousAngularVelocity) / deltaTime
        
        // Update history
        previousAngularVelocity = angularVelocity
        previousOrientation = orientation
        previousTimestamp = timestamp
    }
    
    fun getAngularVelocity(): Vector3 = angularVelocity
    fun getAngularAcceleration(): Vector3 = angularAcceleration
    
    fun reset() {
        previousOrientation = Quaternion.identity
        previousTimestamp = 0L
        angularVelocity = Vector3.zero
        angularAcceleration = Vector3.zero
        previousAngularVelocity = Vector3.zero
    }
}