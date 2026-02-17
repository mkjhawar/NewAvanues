package com.augmentalis.cockpit.spatial

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Desktop implementation of [ISpatialOrientationSource].
 *
 * Desktop has no IMU hardware, so orientation is controlled manually:
 * - Mouse drag (middle button) rotates the viewport
 * - Keyboard arrows nudge the viewport
 * - External joystick input (future)
 *
 * Call [setOrientation] from input handlers to update the orientation.
 */
class DesktopSpatialOrientationSource : ISpatialOrientationSource {

    private val _orientationFlow = MutableStateFlow(SpatialOrientation.ZERO)
    override val orientationFlow: Flow<SpatialOrientation> = _orientationFlow

    private var tracking = false

    /**
     * Manually set orientation from desktop input (mouse drag, keyboard, joystick).
     *
     * @param yaw Horizontal angle in degrees
     * @param pitch Vertical angle in degrees
     */
    fun setOrientation(yaw: Float, pitch: Float) {
        _orientationFlow.value = SpatialOrientation(
            yawDegrees = yaw,
            pitchDegrees = pitch,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Nudge orientation by a delta (useful for keyboard arrow keys).
     *
     * @param deltaYaw Horizontal nudge in degrees (positive = right)
     * @param deltaPitch Vertical nudge in degrees (positive = down)
     */
    fun nudge(deltaYaw: Float, deltaPitch: Float) {
        val current = _orientationFlow.value
        setOrientation(
            yaw = current.yawDegrees + deltaYaw,
            pitch = current.pitchDegrees + deltaPitch
        )
    }

    /** Reset orientation to center (0, 0) */
    fun reset() {
        setOrientation(0f, 0f)
    }

    override fun startTracking(consumerId: String): Boolean {
        tracking = true
        return true
    }

    override fun stopTracking(consumerId: String) {
        tracking = false
    }

    override fun isTracking(): Boolean = tracking
}
