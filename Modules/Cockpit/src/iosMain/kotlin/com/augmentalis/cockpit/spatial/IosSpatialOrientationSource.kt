/**
 * IosSpatialOrientationSource.kt — iOS IMU-based spatial orientation
 *
 * Uses CMMotionManager (CoreMotion framework) to provide head-tracking
 * orientation data for the spatial canvas shell. On supported devices
 * (iPhone, iPad), the gyroscope provides real-time yaw/pitch data that
 * drives viewport panning in the SpaceAvanue shell.
 *
 * On iOS Simulator or devices without a gyroscope, falls back to manual
 * orientation control (same as Desktop).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.cockpit.spatial

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import platform.CoreMotion.CMMotionManager
import platform.Foundation.NSOperationQueue

/**
 * iOS implementation of [ISpatialOrientationSource] using CoreMotion.
 *
 * Provides yaw/pitch orientation from the device's gyroscope for spatial
 * canvas head-tracking. Updates at 60Hz when tracking is active.
 *
 * Falls back gracefully: if gyroscope is unavailable, orientation stays
 * at ZERO and [startTracking] returns false.
 */
class IosSpatialOrientationSource : ISpatialOrientationSource {

    private val motionManager = CMMotionManager()
    private val _orientationFlow = MutableStateFlow(SpatialOrientation.ZERO)
    override val orientationFlow: Flow<SpatialOrientation> = _orientationFlow

    private var tracking = false

    init {
        // 60Hz update interval for smooth head tracking
        motionManager.deviceMotionUpdateInterval = 1.0 / 60.0
    }

    override fun startTracking(consumerId: String): Boolean {
        if (tracking) return true

        if (!motionManager.isDeviceMotionAvailable()) {
            // No gyroscope (e.g. simulator) — return false, stay at ZERO
            return false
        }

        motionManager.startDeviceMotionUpdatesToQueue(
            NSOperationQueue.mainQueue
        ) { motion, _ ->
            if (motion != null) {
                val attitude = motion.attitude
                // Convert radians to degrees
                // yaw = rotation around vertical axis (looking left/right)
                // pitch = rotation around lateral axis (looking up/down)
                val yawDeg = (attitude.yaw * 180.0 / kotlin.math.PI).toFloat()
                val pitchDeg = (attitude.pitch * 180.0 / kotlin.math.PI).toFloat()

                _orientationFlow.value = SpatialOrientation(
                    yawDegrees = yawDeg,
                    pitchDegrees = pitchDeg,
                    timestamp = (motion.timestamp * 1000.0).toLong()
                )
            }
        }

        tracking = true
        return true
    }

    override fun stopTracking(consumerId: String) {
        if (!tracking) return
        motionManager.stopDeviceMotionUpdates()
        tracking = false
    }

    override fun isTracking(): Boolean = tracking

    /**
     * Manually set orientation (for testing or fallback mode).
     */
    fun setOrientation(yaw: Float, pitch: Float) {
        _orientationFlow.value = SpatialOrientation(
            yawDegrees = yaw,
            pitchDegrees = pitch,
            timestamp = platform.Foundation.NSDate().timeIntervalSince1970.toLong() * 1000
        )
    }

    /**
     * Reset orientation to center.
     */
    fun reset() {
        _orientationFlow.value = SpatialOrientation.ZERO
    }
}
