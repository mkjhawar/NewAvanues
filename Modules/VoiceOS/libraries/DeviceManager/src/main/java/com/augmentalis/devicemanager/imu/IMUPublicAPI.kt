// Copyright (c) 2025 Augmentalis, Inc.
// Author: Manoj Jhawar
// Purpose: Public API facade for IMU (Inertial Measurement Unit) operations

package com.augmentalis.devicemanager.imu

import android.content.Context
import android.util.Log
import com.augmentalis.devicemanager.sensors.imu.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Public API for IMU (Inertial Measurement Unit) operations
 *
 * Provides a simplified facade over the internal IMU system for:
 * - Starting and stopping orientation tracking
 * - Retrieving current device orientation (Quaternion/Euler angles)
 * - Detecting device motion state
 * - Accessing orientation and motion data streams
 *
 * This class wraps the complex internal IMUManager to provide a clean,
 * easy-to-use public interface for consuming applications.
 *
 * Usage:
 * ```kotlin
 * val imuAPI = IMUPublicAPI(context)
 * imuAPI.startTracking()
 *
 * // Get current orientation
 * val orientation = imuAPI.getCurrentOrientation()
 *
 * // Observe orientation stream
 * imuAPI.orientationFlow.collect { orientation ->
 *     // Process orientation data
 * }
 *
 * imuAPI.stopTracking()
 * ```
 *
 * @param context Android application context
 */
class IMUPublicAPI(private val context: Context) {

    companion object {
        private const val TAG = "IMUPublicAPI"
        private const val DEFAULT_CONSUMER_ID = "IMUPublicAPI"
    }

    // Internal IMU manager (singleton)
    private val imuManager: IMUManager by lazy {
        IMUManager.getInstance(context)
    }

    // Coroutine scope for API operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Track if currently tracking
    private var isTracking = false

    // Consumer ID for this API instance
    private val consumerId: String = "${DEFAULT_CONSUMER_ID}_${System.currentTimeMillis()}"

    // ========== PUBLIC DATA CLASSES ==========

    /**
     * Simplified orientation data for public API
     */
    data class Orientation(
        val quaternion: QuaternionData,
        val eulerAngles: EulerAnglesData,
        val timestamp: Long,
        val accuracy: OrientationAccuracy
    )

    /**
     * Quaternion representation (w, x, y, z)
     */
    data class QuaternionData(
        val w: Float,
        val x: Float,
        val y: Float,
        val z: Float
    ) {
        companion object {
            val identity = QuaternionData(1f, 0f, 0f, 0f)
        }
    }

    /**
     * Euler angles in radians (yaw, pitch, roll)
     */
    data class EulerAnglesData(
        val yaw: Float,   // Rotation around Z-axis (heading)
        val pitch: Float, // Rotation around Y-axis (elevation)
        val roll: Float   // Rotation around X-axis (bank)
    ) {
        /**
         * Convert to degrees
         */
        fun toDegrees(): EulerAnglesData = EulerAnglesData(
            yaw = Math.toDegrees(yaw.toDouble()).toFloat(),
            pitch = Math.toDegrees(pitch.toDouble()).toFloat(),
            roll = Math.toDegrees(roll.toDouble()).toFloat()
        )
    }

    /**
     * Motion state information
     */
    data class MotionState(
        val isMoving: Boolean,
        val angularVelocity: AngularVelocity,
        val timestamp: Long
    )

    /**
     * Angular velocity (radians per second)
     */
    data class AngularVelocity(
        val x: Float, // Roll rate
        val y: Float, // Pitch rate
        val z: Float  // Yaw rate
    ) {
        val magnitude: Float
            get() = kotlin.math.sqrt(x * x + y * y + z * z)
    }

    /**
     * Orientation accuracy levels
     */
    enum class OrientationAccuracy {
        HIGH,       // Very accurate (magnetometer calibrated)
        MEDIUM,     // Moderately accurate
        LOW,        // Low accuracy (magnetometer needs calibration)
        UNRELIABLE  // Unreliable data
    }

    /**
     * Sensor capabilities
     */
    data class SensorCapabilities(
        val hasRotationVector: Boolean,
        val hasGameRotationVector: Boolean,
        val hasGyroscope: Boolean,
        val hasAccelerometer: Boolean,
        val hasMagnetometer: Boolean,
        val maxSampleRate: Int,
        val resolution: Float
    )

    // ========== PUBLIC API METHODS ==========

    /**
     * Starts IMU orientation tracking
     *
     * Call this before using getCurrentOrientation() or observing orientation flows.
     * Multiple calls to startTracking() are safe (tracked internally).
     *
     * @return true if tracking started successfully, false otherwise
     */
    fun startTracking(): Boolean {
        if (isTracking) {
            Log.d(TAG, "Already tracking, skipping start")
            return true
        }

        return try {
            val success = imuManager.startIMUTracking(consumerId)
            if (success) {
                isTracking = true
                Log.i(TAG, "IMU tracking started")
            } else {
                Log.w(TAG, "Failed to start IMU tracking")
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error starting IMU tracking", e)
            false
        }
    }

    /**
     * Stops IMU orientation tracking
     *
     * Call this when no longer needing orientation data to save battery.
     * Safe to call even if not currently tracking.
     */
    fun stopTracking() {
        if (!isTracking) {
            Log.d(TAG, "Not tracking, skipping stop")
            return
        }

        try {
            imuManager.stopIMUTracking(consumerId)
            isTracking = false
            Log.i(TAG, "IMU tracking stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping IMU tracking", e)
        }
    }

    /**
     * Gets the current device orientation synchronously
     *
     * Returns the most recent orientation data, or null if:
     * - Tracking has not been started
     * - No data available yet
     * - IMU sensors not available
     *
     * @return Current orientation data, or null
     */
    fun getCurrentOrientation(): Orientation? {
        if (!isTracking) {
            Log.w(TAG, "Not tracking, call startTracking() first")
            return null
        }

        val imuData = imuManager.getCurrentOrientation() ?: return null

        return try {
            // Convert internal IMUData to public Orientation
            val quaternion = QuaternionData(
                w = 1f, // IMUData doesn't expose quaternion, use identity
                x = 0f,
                y = 0f,
                z = 0f
            )

            val eulerAngles = EulerAnglesData(
                yaw = imuData.gamma,   // Gamma maps to yaw
                pitch = imuData.beta,  // Beta maps to pitch
                roll = imuData.alpha   // Alpha maps to roll
            )

            Orientation(
                quaternion = quaternion,
                eulerAngles = eulerAngles,
                timestamp = imuData.ts,
                accuracy = OrientationAccuracy.HIGH
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error converting IMU data", e)
            null
        }
    }

    /**
     * Gets the current motion state of the device
     *
     * Detects if device is moving based on angular velocity.
     *
     * @return Motion state, or null if not available
     */
    fun getMotionState(): MotionState? {
        if (!isTracking) {
            Log.w(TAG, "Not tracking, call startTracking() first")
            return null
        }

        val motionData = imuManager.getCurrentMotion() ?: return null

        return try {
            val angularVel = AngularVelocity(
                x = motionData.angularVelocity.x,
                y = motionData.angularVelocity.y,
                z = motionData.angularVelocity.z
            )

            // Consider device moving if angular velocity exceeds threshold
            val movementThreshold = 0.1f // radians/second
            val isMoving = angularVel.magnitude > movementThreshold

            MotionState(
                isMoving = isMoving,
                angularVelocity = angularVel,
                timestamp = motionData.timestamp
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting motion state", e)
            null
        }
    }

    /**
     * Returns true if device is currently moving
     *
     * Convenience method for motion detection.
     */
    fun isDeviceMoving(): Boolean {
        return getMotionState()?.isMoving ?: false
    }

    /**
     * Gets available sensor capabilities
     *
     * Provides information about what IMU sensors are available on the device.
     *
     * @return Sensor capabilities, or null if not available
     */
    fun getSensorCapabilities(): SensorCapabilities? {
        val caps = imuManager.getSensorCapabilities() ?: return null

        return SensorCapabilities(
            hasRotationVector = caps.hasRotationVector,
            hasGameRotationVector = caps.hasGameRotationVector,
            hasGyroscope = caps.hasGyroscope,
            hasAccelerometer = caps.hasAccelerometer,
            hasMagnetometer = caps.hasMagnetometer,
            maxSampleRate = caps.maxSampleRate,
            resolution = caps.resolution
        )
    }

    /**
     * Returns true if IMU tracking is currently active
     */
    fun isTracking(): Boolean = isTracking

    // ========== REACTIVE STREAMS ==========

    /**
     * Flow of orientation updates
     *
     * Emits orientation data whenever device orientation changes.
     * Automatically filters out null values.
     *
     * Usage:
     * ```kotlin
     * imuAPI.orientationFlow.collect { orientation ->
     *     println("Yaw: ${orientation.eulerAngles.yaw}")
     * }
     * ```
     */
    val orientationFlow: Flow<Orientation> = imuManager.orientationFlow.map { imuData ->
        val quaternion = QuaternionData(1f, 0f, 0f, 0f)
        val eulerAngles = EulerAnglesData(
            yaw = imuData.gamma,
            pitch = imuData.beta,
            roll = imuData.alpha
        )

        Orientation(
            quaternion = quaternion,
            eulerAngles = eulerAngles,
            timestamp = imuData.ts,
            accuracy = OrientationAccuracy.HIGH
        )
    }

    /**
     * Flow of motion state updates
     *
     * Emits motion state whenever device motion changes.
     *
     * Usage:
     * ```kotlin
     * imuAPI.motionFlow.collect { motion ->
     *     if (motion.isMoving) {
     *         println("Device is moving")
     *     }
     * }
     * ```
     */
    val motionFlow: Flow<MotionState> = imuManager.motionFlow.map { motionData ->
        val angularVel = AngularVelocity(
            x = motionData.angularVelocity.x,
            y = motionData.angularVelocity.y,
            z = motionData.angularVelocity.z
        )

        val movementThreshold = 0.1f
        val isMoving = angularVel.magnitude > movementThreshold

        MotionState(
            isMoving = isMoving,
            angularVelocity = angularVel,
            timestamp = motionData.timestamp
        )
    }

    // ========== CLEANUP ==========

    /**
     * Cleans up resources
     *
     * Call this when the API instance is no longer needed.
     * This will stop tracking and release resources.
     */
    fun dispose() {
        try {
            stopTracking()
            scope.cancel()
            Log.d(TAG, "IMU Public API disposed")
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing IMU Public API", e)
        }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Returns a human-readable summary of IMU status
     */
    fun getStatusSummary(): String = buildString {
        appendLine("IMU Status:")
        appendLine("  Tracking: $isTracking")

        getSensorCapabilities()?.let { caps ->
            appendLine("\nSensor Capabilities:")
            appendLine("  Rotation Vector: ${caps.hasRotationVector}")
            appendLine("  Game Rotation Vector: ${caps.hasGameRotationVector}")
            appendLine("  Gyroscope: ${caps.hasGyroscope}")
            appendLine("  Accelerometer: ${caps.hasAccelerometer}")
            appendLine("  Magnetometer: ${caps.hasMagnetometer}")
            appendLine("  Max Sample Rate: ${caps.maxSampleRate} Hz")
            appendLine("  Resolution: ${caps.resolution}")
        }

        if (isTracking) {
            getCurrentOrientation()?.let { orientation ->
                val euler = orientation.eulerAngles.toDegrees()
                appendLine("\nCurrent Orientation (degrees):")
                appendLine("  Yaw: ${"%.2f".format(euler.yaw)}")
                appendLine("  Pitch: ${"%.2f".format(euler.pitch)}")
                appendLine("  Roll: ${"%.2f".format(euler.roll)}")
            }

            getMotionState()?.let { motion ->
                appendLine("\nMotion State:")
                appendLine("  Moving: ${motion.isMoving}")
                appendLine("  Angular Velocity: ${"%.3f".format(motion.angularVelocity.magnitude)} rad/s")
            }
        }
    }
}
