package com.augmentalis.netavanue.pairing

import com.augmentalis.netavanue.signaling.DevicePlatform
import com.augmentalis.netavanue.signaling.DeviceType
import kotlinx.serialization.Serializable

/**
 * A device that has been paired with this device.
 *
 * Paired devices can auto-connect to sessions without invite codes.
 * The pairing is stored locally and validated by the signaling server.
 */
@Serializable
data class PairedDevice(
    val pairingId: String,
    val fingerprint: String,
    val deviceName: String,
    val platform: DevicePlatform,
    val deviceType: DeviceType,
    val pairedAt: Long, // epoch millis
)
