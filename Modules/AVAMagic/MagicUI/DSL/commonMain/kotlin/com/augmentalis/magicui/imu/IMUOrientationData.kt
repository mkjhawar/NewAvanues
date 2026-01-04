package com.augmentalis.magicui.imu

import kotlinx.serialization.Serializable

/**
 * Snapshot of device orientation from IMU sensors.
 *
 * Orientation is measured in radians using the right-hand rule:
 * - **Pitch**: Rotation around X-axis (nose up/down)
 * - **Roll**: Rotation around Y-axis (tilt left/right)
 * - **Yaw**: Rotation around Z-axis (turn left/right)
 *
 * ## Coordinate System
 *
 * ```
 *     Y (Roll)
 *     |
 *     |
 *     +---- X (Pitch)
 *    /
 *   Z (Yaw)
 * ```
 *
 * ## Example Usage
 *
 * ```kotlin
 * val orientation = IMUOrientationData(
 *     pitch = 0.1f,  // Slightly looking up
 *     roll = 0.0f,   // Level
 *     yaw = -0.2f,   // Turned slightly left
 *     timestamp = System.currentTimeMillis()
 * )
 * ```
 *
 * @property pitch Rotation around X-axis in radians (-π to π)
 * @property roll Rotation around Y-axis in radians (-π to π)
 * @property yaw Rotation around Z-axis in radians (-π to π)
 * @property timestamp Capture time in milliseconds since epoch
 *
 * @since 3.1.0
 */
@Serializable
data class IMUOrientationData(
    val pitch: Float,
    val roll: Float,
    val yaw: Float,
    val timestamp: Long
) {
    /**
     * Returns the magnitude of rotation (Euclidean norm).
     */
    fun magnitude(): Float {
        return kotlin.math.sqrt(pitch * pitch + roll * roll + yaw * yaw)
    }

    /**
     * Returns a copy with the given axis locked (set to 0).
     */
    fun withLockedPitch(): IMUOrientationData = copy(pitch = 0f)
    fun withLockedRoll(): IMUOrientationData = copy(roll = 0f)
    fun withLockedYaw(): IMUOrientationData = copy(yaw = 0f)

    /**
     * Returns the delta between this orientation and another.
     *
     * @param previous Previous orientation snapshot
     * @return New IMUOrientationData representing the change
     */
    fun delta(previous: IMUOrientationData): IMUOrientationData {
        return IMUOrientationData(
            pitch = pitch - previous.pitch,
            roll = roll - previous.roll,
            yaw = yaw - previous.yaw,
            timestamp = timestamp
        )
    }

    companion object {
        /**
         * Zero orientation (all axes at 0).
         */
        val ZERO = IMUOrientationData(0f, 0f, 0f, 0L)
    }
}
