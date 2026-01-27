// Author: Manoj Jhawar
// Purpose: User calibration and personalization for IMU tracking

package com.augmentalis.devicemanager.imu

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages IMU calibration and user personalization
 */
class CalibrationManager {
    
    companion object {
        private const val NEUTRAL_CALIBRATION_DURATION = 2000L // 2 seconds
        private const val RANGE_CALIBRATION_TIMEOUT = 10000L // 10 seconds per direction
        private const val STABILITY_THRESHOLD = 0.01f // rad/s
        private const val MIN_CALIBRATION_SAMPLES = 50
    }
    
    private enum class CalibrationState {
        IDLE,
        CALIBRATING_NEUTRAL,
        CALIBRATING_RANGE,
        COMPLETED,
        FAILED
    }
    
    private var calibrationState = CalibrationState.IDLE
    private val calibrationData = mutableListOf<Quaternion>()
    
    /**
     * Perform automatic neutral position calibration
     */
    suspend fun performCalibration(imuManager: IMUManager): CalibrationResult {
        return try {
            calibrationState = CalibrationState.CALIBRATING_NEUTRAL
            
            val neutralOrientation = calibrateNeutralPosition(imuManager)
            
            calibrationState = CalibrationState.COMPLETED
            CalibrationResult(
                success = true,
                baseOrientation = neutralOrientation,
                message = "Calibration completed successfully"
            )
        } catch (e: Exception) {
            calibrationState = CalibrationState.FAILED
            CalibrationResult(
                success = false,
                baseOrientation = Quaternion.identity,
                message = "Calibration failed: ${e.message}"
            )
        }
    }
    
    /**
     * Perform guided calibration with user instructions
     */
    suspend fun performGuidedCalibration(
        imuManager: IMUManager,
        onInstruction: (String) -> Unit,
        onProgress: (Float) -> Unit
    ): CalibrationResult {
        return try {
            // Step 1: Neutral position
            onInstruction("Hold device in comfortable neutral position")
            val neutralOrientation = calibrateNeutralPosition(imuManager) { progress ->
                onProgress(progress * 0.5f)
            }
            
            // Step 2: Range calibration
            onInstruction("Move device through comfortable range of motion")
            val movementRange = calibrateMovementRange(imuManager) { progress ->
                onProgress(0.5f + progress * 0.5f)
            }
            
            CalibrationResult(
                success = true,
                baseOrientation = neutralOrientation,
                message = "Guided calibration completed"
            )
        } catch (e: Exception) {
            CalibrationResult(
                success = false,
                baseOrientation = Quaternion.identity,
                message = "Guided calibration failed: ${e.message}"
            )
        }
    }
    
    /**
     * Get current calibration state
     */
    fun getCalibrationState(): String {
        return when (calibrationState) {
            CalibrationState.IDLE -> "Ready for calibration"
            CalibrationState.CALIBRATING_NEUTRAL -> "Calibrating neutral position..."
            CalibrationState.CALIBRATING_RANGE -> "Calibrating movement range..."
            CalibrationState.COMPLETED -> "Calibration completed"
            CalibrationState.FAILED -> "Calibration failed"
        }
    }
    
    private suspend fun calibrateNeutralPosition(
        imuManager: IMUManager,
        onProgress: ((Float) -> Unit)? = null
    ): Quaternion = withContext(Dispatchers.Default) {
        
        calibrationData.clear()
        val startTime = System.currentTimeMillis()
        
        // Collect orientation data for specified duration
        val job = launch {
            imuManager.orientationFlow.collect { orientationData ->
                if (isDeviceStable(imuManager)) {
                    //calibrationData.add(orientationData.quaternion)
                    
                    val elapsed = System.currentTimeMillis() - startTime
                    onProgress?.invoke(elapsed.toFloat() / NEUTRAL_CALIBRATION_DURATION)
                    
                    if (elapsed >= NEUTRAL_CALIBRATION_DURATION) {
                        cancel()
                    }
                }
            }
        }
        
        // Wait for completion
        try {
            job.join()
        } catch (e: CancellationException) {
            // Expected when duration is reached
        }
        
        // Calculate average orientation
        if (calibrationData.size < MIN_CALIBRATION_SAMPLES) {
            throw IllegalStateException("Insufficient calibration data: ${calibrationData.size} samples")
        }
        
        calculateAverageQuaternion(calibrationData)
    }
    
    private suspend fun calibrateMovementRange(
        imuManager: IMUManager,
        onProgress: ((Float) -> Unit)? = null
    ): MovementRange = withContext(Dispatchers.Default) {
        
        val ranges = mutableMapOf<String, Float>()
        val directions = listOf("up", "down", "left", "right")
        var completedDirections = 0
        
        val job = launch {
            var maxYaw = 0f
            var maxPitch = 0f
            var maxRoll = 0f
            
            imuManager.orientationFlow.collect { orientationData ->
                /*val euler = orientationData.quaternion.toEulerAngles()

                maxYaw = max(maxYaw, abs(euler.yaw))
                maxPitch = max(maxPitch, abs(euler.pitch))
                maxRoll = max(maxRoll, abs(euler.roll))

                ranges["yaw"] = maxYaw
                ranges["pitch"] = maxPitch
                ranges["roll"] = maxRoll

                onProgress?.invoke(completedDirections.toFloat() / directions.size)*/
            }
        }
        
        // Let it run for the timeout period
        delay(RANGE_CALIBRATION_TIMEOUT)
        job.cancel()
        
        MovementRange(
            yawRange = ranges["yaw"] ?: 0f,
            pitchRange = ranges["pitch"] ?: 0f,
            rollRange = ranges["roll"] ?: 0f
        )
    }
    
    private suspend fun isDeviceStable(imuManager: IMUManager): Boolean {
        val motionData = imuManager.getCurrentMotion()
        return motionData?.angularVelocity?.magnitude ?: Float.MAX_VALUE < STABILITY_THRESHOLD
    }
    
    private fun calculateAverageQuaternion(quaternions: List<Quaternion>): Quaternion {
        if (quaternions.isEmpty()) return Quaternion.identity
        if (quaternions.size == 1) return quaternions[0]
        
        // Use iterative averaging for quaternions
        var average = quaternions[0]
        
        for (i in 1 until quaternions.size) {
            val weight = 1f / (i + 1f)
            average = slerp(average, quaternions[i], weight)
        }
        
        return average.normalized
    }
}

/**
 * Represents user's comfortable movement range
 */
data class MovementRange(
    val yawRange: Float,
    val pitchRange: Float,
    val rollRange: Float
) {
    fun getComfortableScale(): Float {
        // Return a scale factor based on the user's natural range
        val averageRange = (yawRange + pitchRange + rollRange) / 3f
        return when {
            averageRange < 0.1f -> 2.0f  // Small movements - increase sensitivity
            averageRange < 0.3f -> 1.5f  // Normal movements
            averageRange < 0.6f -> 1.0f  // Large movements - normal sensitivity
            else -> 0.8f                  // Very large movements - reduce sensitivity
        }
    }
}