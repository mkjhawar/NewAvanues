package com.augmentalis.remotecast.model

import kotlinx.serialization.Serializable

/**
 * Represents the state of a remote screen casting session.
 * Used by Cockpit to display a mirrored view of another device's screen.
 */
@Serializable
data class CastState(
    val deviceName: String = "",
    val deviceId: String = "",
    val isConnected: Boolean = false,
    val isStreaming: Boolean = false,
    val resolution: CastResolution = CastResolution.HD,
    val frameRate: Int = 30,
    val latencyMs: Long = 0,
    val error: String? = null
)

enum class CastResolution(val width: Int, val height: Int) {
    SD(640, 480),
    HD(1280, 720),
    FHD(1920, 1080)
}

@Serializable
data class CastDevice(
    val id: String,
    val name: String,
    val address: String,
    val isAvailable: Boolean = true
)
