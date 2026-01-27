// Author: Manoj Jhawar
// Purpose: Advanced sensor fusion for high-quality orientation tracking

package com.augmentalis.devicemanager.imu

import kotlin.math.*

/**
 * Enhanced sensor fusion combining multiple IMU sensors for optimal tracking
 */
class EnhancedSensorFusion {
    
    companion object {
        private const val COMPLEMENTARY_ALPHA = 0.98f
        private const val GYRO_BIAS_ALPHA = 0.001f
        private const val GRAVITY_MAGNITUDE = 9.81f
        private const val GRAVITY_TOLERANCE = 2f
    }
    
    // Current orientation estimate
    private var orientation = Quaternion.identity
    
    // Gyroscope bias compensation
    private var gyroscopeBias = Vector3.zero
    private var gyroCalibrationSamples = 0
    private val maxCalibrationSamples = 1000
    
    // Sensor data storage
    private var lastGyroData = Vector3.zero
    private var lastAccelData = Vector3.zero
    private var lastMagData = Vector3.zero
    private var lastTimestamp = 0L
    
    // Kalman filter for noise reduction
    private val kalmanFilter = SimpleKalmanFilter()
    
    // Movement detection for bias calibration
    private var isStationary = false
    private val stationaryThreshold = 0.1f
    
    /**
     * Process rotation vector sensor data
     */
    fun processRotationVector(quaternion: Quaternion, timestamp: Long): Quaternion {
        // Apply Kalman filtering to reduce noise
        val filtered = kalmanFilter.filter(quaternion, timestamp)
        
        // Update orientation
        orientation = filtered.normalized
        lastTimestamp = timestamp
        
        return orientation
    }
    
    /**
     * Process gyroscope data for sensor fusion
     */
    fun processGyroscope(angularVelocity: Vector3, timestamp: Long) {
        lastGyroData = angularVelocity
        
        // Perform bias calibration if device is stationary
        if (isStationary && gyroCalibrationSamples < maxCalibrationSamples) {
            updateGyroscopeBias(angularVelocity)
        }
        
        // If we don't have rotation vector sensor, integrate gyroscope
        if (lastTimestamp == 0L) {
            integrateGyroscope(angularVelocity, timestamp)
        }
    }
    
    /**
     * Process accelerometer data for sensor fusion
     */
    fun processAccelerometer(acceleration: Vector3, timestamp: Long) {
        lastAccelData = acceleration
        
        // Check if device is stationary for gyro calibration
        val accelMagnitude = acceleration.magnitude
        isStationary = abs(accelMagnitude - GRAVITY_MAGNITUDE) < GRAVITY_TOLERANCE
        
        // Use accelerometer to correct orientation if no rotation sensor
        if (lastTimestamp == 0L) {
            correctWithAccelerometer(acceleration)
        }
    }
    
    /**
     * Process magnetometer data for sensor fusion
     */
    fun processMagnetometer(magneticField: Vector3, timestamp: Long) {
        lastMagData = magneticField
        
        // Use magnetometer for yaw correction if no rotation sensor
        if (lastTimestamp == 0L) {
            correctWithMagnetometer(magneticField)
        }
    }
    
    /**
     * Get current fused orientation
     */
    fun getCurrentOrientation(): Quaternion = orientation
    
    /**
     * Get gyroscope bias estimate
     */
    fun getGyroscopeBias(): Vector3 = gyroscopeBias
    
    /**
     * Reset fusion state
     */
    fun reset() {
        orientation = Quaternion.identity
        gyroscopeBias = Vector3.zero
        gyroCalibrationSamples = 0
        lastTimestamp = 0L
        kalmanFilter.reset()
    }
    
    private fun updateGyroscopeBias(angularVelocity: Vector3) {
        // Exponential moving average for bias estimation
        gyroscopeBias = gyroscopeBias * (1f - GYRO_BIAS_ALPHA) + angularVelocity * GYRO_BIAS_ALPHA
        gyroCalibrationSamples++
    }
    
    private fun integrateGyroscope(angularVelocity: Vector3, timestamp: Long) {
        if (lastTimestamp == 0L) {
            lastTimestamp = timestamp
            return
        }
        
        val deltaTime = (timestamp - lastTimestamp) * 1e-9f // Convert to seconds
        if (deltaTime <= 0 || deltaTime > 0.1f) { // Ignore invalid time deltas
            lastTimestamp = timestamp
            return
        }
        
        // Apply bias compensation
        val correctedVelocity = angularVelocity - gyroscopeBias
        
        // Integrate angular velocity to get rotation
        val rotationAngle = correctedVelocity.magnitude * deltaTime
        
        if (rotationAngle > 0.001f) { // Avoid division by zero
            val rotationAxis = correctedVelocity.normalized
            val deltaRotation = Quaternion.fromAxisAngle(rotationAxis, rotationAngle)
            
            // Apply rotation to current orientation
            orientation = (orientation * deltaRotation).normalized
        }
        
        lastTimestamp = timestamp
    }
    
    private fun correctWithAccelerometer(acceleration: Vector3) {
        val accelMagnitude = acceleration.magnitude
        
        // Only use accelerometer if close to gravity
        if (abs(accelMagnitude - GRAVITY_MAGNITUDE) < GRAVITY_TOLERANCE) {
            val gravity = acceleration.normalized
            
            // Calculate rotation to align with gravity
            val currentDown = orientation * Vector3(0f, 0f, -1f) // Assuming Z down
            val correctionAxis = currentDown.cross(gravity)
            val correctionAngle = asin(correctionAxis.magnitude.coerceIn(-1f, 1f))
            
            if (correctionAngle > 0.01f) {
                val correction = Quaternion.fromAxisAngle(correctionAxis.normalized, correctionAngle * (1f - COMPLEMENTARY_ALPHA))
                orientation = (correction * orientation).normalized
            }
        }
    }
    
    private fun correctWithMagnetometer(magneticField: Vector3) {
        val magNormalized = magneticField.normalized
        
        // Project magnetic field onto horizontal plane
        val gravity = Vector3(0f, 0f, -1f) // Assuming Z down
        val east = gravity.cross(magNormalized).normalized
        val north = east.cross(gravity).normalized
        
        // Calculate yaw correction
        val currentNorth = orientation * Vector3(0f, 1f, 0f) // Assuming Y north
        val projectedNorth = currentNorth - gravity * currentNorth.dot(gravity)
        
        if (projectedNorth.magnitude > 0.1f) {
            val yawError = atan2(
                projectedNorth.cross(north).dot(gravity),
                projectedNorth.dot(north)
            )
            
            if (abs(yawError) > 0.01f) {
                val yawCorrection = Quaternion.fromAxisAngle(gravity, yawError * (1f - COMPLEMENTARY_ALPHA))
                orientation = (yawCorrection * orientation).normalized
            }
        }
    }
}

/**
 * Simple Kalman filter for quaternion noise reduction
 */
class SimpleKalmanFilter {
    
    companion object {
        private const val PROCESS_NOISE = 0.001f
        private const val MEASUREMENT_NOISE = 0.1f
    }
    
    private var state = Quaternion.identity
    private var errorCovariance = 1.0f
    
    fun filter(measurement: Quaternion, timestamp: Long): Quaternion {
        // Prediction step (assume constant orientation)
        val predictedState = state
        val predictedCovariance = errorCovariance + PROCESS_NOISE
        
        // Update step
        val kalmanGain = predictedCovariance / (predictedCovariance + MEASUREMENT_NOISE)
        
        // Update state using SLERP for quaternions
        state = slerp(predictedState, measurement, kalmanGain)
        errorCovariance = (1f - kalmanGain) * predictedCovariance
        
        return state
    }
    
    fun reset() {
        state = Quaternion.identity
        errorCovariance = 1.0f
    }
}