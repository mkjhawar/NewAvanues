// Author: Manoj Jhawar
// Purpose: Cross-platform IMU data models for head tracking and spatial input

package com.augmentalis.devicemanager

import kotlinx.serialization.Serializable

/**
 * IMU (Inertial Measurement Unit) Data Models
 *
 * These models provide platform-agnostic representations of IMU data
 * for use in head tracking, spatial input, and motion sensing applications.
 *
 * Note: IMUManager implementation remains Android-only as it directly uses
 * Android SensorManager. These data models enable cross-platform data exchange
 * while keeping hardware-specific code in androidMain.
 *
 * Used by:
 * - VoiceCursor (Android-only): Head tracking for cursor control
 * - HUDManager (Android-only): Spatial head tracking for AR overlays
 */

/**
 * IMU orientation data - platform-agnostic representation.
 *
 * Represents the device's orientation in 3D space using Euler angles.
 *
 * @property yaw Rotation around vertical axis (left-right head turn), in degrees
 * @property pitch Rotation around lateral axis (head tilt up-down), in degrees
 * @property roll Rotation around longitudinal axis (head tilt side-to-side), in degrees
 * @property timestamp Timestamp in milliseconds when this orientation was captured
 */
@Serializable
data class IMUOrientation(
    val yaw: Float,
    val pitch: Float,
    val roll: Float,
    val timestamp: Long
) {
    /**
     * Convert to radians
     */
    fun toRadians(): IMUOrientation = copy(
        yaw = (yaw * kotlin.math.PI / 180.0).toFloat(),
        pitch = (pitch * kotlin.math.PI / 180.0).toFloat(),
        roll = (roll * kotlin.math.PI / 180.0).toFloat()
    )
}

/**
 * IMU cursor position data.
 *
 * Represents a 2D cursor position derived from IMU orientation,
 * typically used for head-controlled cursor movement.
 *
 * @property x Horizontal position, typically 0.0 to 1.0 (normalized) or pixels
 * @property y Vertical position, typically 0.0 to 1.0 (normalized) or pixels
 * @property timestamp Timestamp in milliseconds when this position was calculated
 */
@Serializable
data class IMUCursorPosition(
    val x: Float,
    val y: Float,
    val timestamp: Long
) {
    /**
     * Convert to screen coordinates
     *
     * @param screenWidth Screen width in pixels
     * @param screenHeight Screen height in pixels
     * @return Pixel coordinates
     */
    fun toScreenCoordinates(screenWidth: Int, screenHeight: Int): Pair<Int, Int> {
        return Pair(
            (x * screenWidth).toInt().coerceIn(0, screenWidth),
            (y * screenHeight).toInt().coerceIn(0, screenHeight)
        )
    }
}

/**
 * IMU calibration state.
 *
 * Represents the current calibration status of the IMU sensors.
 */
enum class IMUCalibrationState {
    /** Initial state, no calibration performed */
    UNCALIBRATED,

    /** Calibration in progress */
    CALIBRATING,

    /** Calibration complete and valid */
    CALIBRATED,

    /** Calibration has drifted and needs refresh */
    NEEDS_RECALIBRATION
}

/**
 * Raw IMU sensor data.
 *
 * Contains the raw accelerometer and gyroscope readings before fusion.
 *
 * @property accelerometer 3D acceleration vector (x, y, z) in m/s²
 * @property gyroscope 3D angular velocity vector (x, y, z) in rad/s
 * @property magnetometer Optional 3D magnetic field vector (x, y, z) in μT
 * @property timestamp Timestamp in nanoseconds (sensor event time)
 */
@Serializable
data class IMURawData(
    val accelerometer: FloatArray,
    val gyroscope: FloatArray,
    val magnetometer: FloatArray? = null,
    val timestamp: Long
) {
    init {
        require(accelerometer.size == 3) { "Accelerometer must have 3 components" }
        require(gyroscope.size == 3) { "Gyroscope must have 3 components" }
        magnetometer?.let {
            require(it.size == 3) { "Magnetometer must have 3 components" }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as IMURawData
        return accelerometer.contentEquals(other.accelerometer) &&
                gyroscope.contentEquals(other.gyroscope) &&
                (magnetometer?.contentEquals(other.magnetometer) ?: (other.magnetometer == null)) &&
                timestamp == other.timestamp
    }

    override fun hashCode(): Int {
        var result = accelerometer.contentHashCode()
        result = 31 * result + gyroscope.contentHashCode()
        result = 31 * result + (magnetometer?.contentHashCode() ?: 0)
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * IMU configuration options.
 *
 * Platform-agnostic configuration for IMU processing.
 */
@Serializable
data class IMUConfig(
    /** Sensor sampling rate in Hz */
    val samplingRateHz: Int = 60,

    /** Low-pass filter cutoff frequency in Hz */
    val lowPassCutoffHz: Float = 5.0f,

    /** Enable sensor fusion (accelerometer + gyroscope + magnetometer) */
    val useSensorFusion: Boolean = true,

    /** Enable motion prediction for latency compensation */
    val useMotionPrediction: Boolean = true,

    /** Prediction lookahead time in milliseconds */
    val predictionLookaheadMs: Int = 16
)
