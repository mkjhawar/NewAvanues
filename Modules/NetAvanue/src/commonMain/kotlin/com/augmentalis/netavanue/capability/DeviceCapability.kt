package com.augmentalis.netavanue.capability

import com.augmentalis.netavanue.signaling.DeviceCapabilityDto
import com.augmentalis.netavanue.signaling.NetworkType

/**
 * Local device capability snapshot.
 *
 * Collected by [CapabilityCollector] (platform-specific), scored by [CapabilityScorer],
 * and sent to the signaling server for hub election. The server uses the same scoring
 * algorithm so scores are consistent across client and server.
 */
data class DeviceCapability(
    val cpuCores: Int,
    val ramMb: Int,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val networkType: NetworkType,
    val bandwidthMbps: Int,
    val deviceType: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val supportedCodecs: List<String>,
    val installedModules: List<String>,
) {
    /** Convert to the DTO for signaling wire format */
    fun toDto(): DeviceCapabilityDto = DeviceCapabilityDto(
        cpuCores = cpuCores,
        ramMb = ramMb,
        batteryPercent = batteryPercent,
        isCharging = isCharging,
        networkType = networkType,
        bandwidthMbps = bandwidthMbps,
        deviceType = deviceType,
        screenWidth = screenWidth,
        screenHeight = screenHeight,
        supportedCodecs = supportedCodecs,
        modules = installedModules,
    )
}

/**
 * Platform-specific capability collector.
 *
 * Each platform provides an `actual` implementation that gathers hardware/software
 * info from system APIs (e.g. ActivityManager on Android, UIDevice on iOS).
 */
expect class CapabilityCollector() {
    /** Gather current device capabilities. May suspend for battery/network checks. */
    suspend fun collect(): DeviceCapability
}
