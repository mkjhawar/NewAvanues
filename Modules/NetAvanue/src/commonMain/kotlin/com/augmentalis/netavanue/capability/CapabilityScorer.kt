package com.augmentalis.netavanue.capability

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

    fun calculateScore(capability: DeviceCapability): Int {
        var score = 0
        score += capability.cpuCores * 10
        score += capability.ramMb / 100
        score += if (capability.isCharging) 200 else capability.batteryPercent * 2
        score += capability.bandwidthMbps * 5
        score += if (capability.deviceType == "DESKTOP") 100 else 0
        score += if (capability.networkType == com.augmentalis.netavanue.signaling.NetworkType.ETHERNET) 50 else 0
        score += capability.screenWidth / 10
        score += capability.supportedCodecs.size * 15
        return score
    }

    /** Overload for DTO input (used when evaluating remote peer capabilities) */
    fun calculateScore(dto: com.augmentalis.netavanue.signaling.DeviceCapabilityDto): Int {
        var score = 0
        score += dto.cpuCores * 10
        score += dto.ramMb / 100
        score += if (dto.isCharging == true) 200 else (dto.batteryPercent ?: 0) * 2
        score += (dto.bandwidthMbps ?: 0) * 5
        score += if (dto.deviceType == "DESKTOP") 100 else 0
        score += if (dto.networkType == com.augmentalis.netavanue.signaling.NetworkType.ETHERNET) 50 else 0
        score += (dto.screenWidth ?: 0) / 10
        score += (dto.supportedCodecs?.size ?: 0) * 15
        return score
    }
}
