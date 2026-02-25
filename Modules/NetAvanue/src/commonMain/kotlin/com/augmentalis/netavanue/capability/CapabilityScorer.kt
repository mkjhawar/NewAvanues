package com.augmentalis.netavanue.capability

import com.augmentalis.netavanue.signaling.DeviceCapabilityDto
import com.augmentalis.netavanue.signaling.NetworkType

/**
 * Computes a capability score for a device, used for hub election.
 *
 * The scoring formula is identical to the server-side implementation in
 * AvanueCentral's `CapabilityService.calculateScore()` to ensure consistent
 * election results regardless of whether the client or server computes it.
 *
 * Scoring formula:
 * ```
 * score = cpuCores * 10
 *       + ramMb / 100
 *       + (isCharging ? 200 : batteryPercent * 2)
 *       + bandwidthMbps * 5
 *       + (isDesktop ? 100 : 0)
 *       + (hasEthernet ? 50 : 0)
 *       + screenWidth / 10
 *       + supportedCodecs.size * 15
 * ```
 */
object CapabilityScorer {

    fun calculateScore(capability: DeviceCapability): Int = computeScore(
        cpuCores = capability.cpuCores,
        ramMb = capability.ramMb,
        batteryPercent = capability.batteryPercent,
        isCharging = capability.isCharging,
        bandwidthMbps = capability.bandwidthMbps,
        deviceType = capability.deviceType,
        networkType = capability.networkType,
        screenWidth = capability.screenWidth,
        codecCount = capability.supportedCodecs.size,
    )

    /** Overload for DTO input (used when evaluating remote peer capabilities) */
    fun calculateScore(dto: DeviceCapabilityDto): Int = computeScore(
        cpuCores = dto.cpuCores,
        ramMb = dto.ramMb,
        batteryPercent = dto.batteryPercent ?: 0,
        isCharging = dto.isCharging ?: false,
        bandwidthMbps = dto.bandwidthMbps ?: 0,
        deviceType = dto.deviceType ?: "UNKNOWN",
        networkType = dto.networkType ?: NetworkType.UNKNOWN,
        screenWidth = dto.screenWidth ?: 0,
        codecCount = dto.supportedCodecs?.size ?: 0,
    )

    private fun computeScore(
        cpuCores: Int,
        ramMb: Int,
        batteryPercent: Int,
        isCharging: Boolean,
        bandwidthMbps: Int,
        deviceType: String,
        networkType: NetworkType,
        screenWidth: Int,
        codecCount: Int,
    ): Int {
        var score = 0
        score += cpuCores * 10
        score += ramMb / 100
        score += if (isCharging) 200 else batteryPercent * 2
        score += bandwidthMbps * 5
        score += if (deviceType == "DESKTOP") 100 else 0
        score += if (networkType == NetworkType.ETHERNET) 50 else 0
        score += screenWidth / 10
        score += codecCount * 15
        return score
    }
}
