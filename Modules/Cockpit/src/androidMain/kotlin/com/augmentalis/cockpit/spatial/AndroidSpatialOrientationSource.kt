package com.augmentalis.cockpit.spatial

import android.content.Context
import com.augmentalis.devicemanager.imu.IMUPublicAPI
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Android implementation of [ISpatialOrientationSource].
 *
 * Wraps [IMUPublicAPI] from DeviceManager to provide orientation data
 * from the device's IMU sensors (accelerometer + gyroscope + rotation vector).
 *
 * IMUPublicAPI doesn't support multi-consumer tracking natively, so this
 * class manages consumer reference counting internally. The underlying
 * IMU is started on the first consumer and stopped when the last consumer
 * disconnects.
 *
 * Orientation values are converted from radians (IMU native) to degrees
 * (spatial canvas expectation).
 */
class AndroidSpatialOrientationSource(context: Context) : ISpatialOrientationSource {

    private val imuAPI = IMUPublicAPI(context)
    private val activeConsumers = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    override val orientationFlow: Flow<SpatialOrientation> =
        imuAPI.orientationFlow.map { orientation ->
            val euler = orientation.eulerAngles.toDegrees()
            SpatialOrientation(
                yawDegrees = euler.yaw,
                pitchDegrees = euler.pitch,
                timestamp = orientation.timestamp
            )
        }

    override fun startTracking(consumerId: String): Boolean {
        activeConsumers.add(consumerId)
        return if (activeConsumers.size == 1) {
            // First consumer — start the IMU
            imuAPI.startTracking()
        } else {
            // Already tracking for another consumer
            true
        }
    }

    override fun stopTracking(consumerId: String) {
        activeConsumers.remove(consumerId)
        if (activeConsumers.isEmpty()) {
            // Last consumer disconnected — stop the IMU
            imuAPI.stopTracking()
        }
    }

    override fun isTracking(): Boolean = imuAPI.isTracking()
}
