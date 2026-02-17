package com.augmentalis.cockpit.spatial

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic interface for spatial orientation data.
 *
 * Android: wraps IMUPublicAPI (accelerometer + gyroscope + rotation vector)
 * Desktop: wraps mouse drag or keyboard input
 * iOS (future): wraps CMMotionManager
 *
 * Implementations must be multi-consumer safe — multiple UI elements can
 * observe [orientationFlow] simultaneously.
 */
interface ISpatialOrientationSource {
    /**
     * Flow of spatial orientation updates.
     * Emissions represent the user's current head/device orientation
     * as yaw (horizontal) and pitch (vertical) in degrees.
     */
    val orientationFlow: Flow<SpatialOrientation>

    /**
     * Start tracking orientation for the given consumer.
     * @param consumerId Unique identifier for the consumer (e.g., "cockpit_spatial")
     * @return true if tracking started successfully
     */
    fun startTracking(consumerId: String): Boolean

    /**
     * Stop tracking for the given consumer.
     * Tracking stops only when all consumers have stopped.
     */
    fun stopTracking(consumerId: String)

    /** Whether tracking is currently active for any consumer */
    fun isTracking(): Boolean
}

/**
 * Orientation data point from the spatial source.
 */
data class SpatialOrientation(
    /** Horizontal head turn in degrees (negative = left, positive = right) */
    val yawDegrees: Float,
    /** Vertical head tilt in degrees (negative = up, positive = down) */
    val pitchDegrees: Float,
    /** Timestamp in milliseconds */
    val timestamp: Long
) {
    companion object {
        val ZERO = SpatialOrientation(0f, 0f, 0L)
    }
}
