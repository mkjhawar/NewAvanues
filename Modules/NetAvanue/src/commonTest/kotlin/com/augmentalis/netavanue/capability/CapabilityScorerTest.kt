package com.augmentalis.netavanue.capability

import com.augmentalis.netavanue.signaling.DeviceCapabilityDto
import com.augmentalis.netavanue.signaling.NetworkType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CapabilityScorerTest {

    @Test
    fun `desktop device scores higher than phone`() {
        val desktop = DeviceCapability(
            cpuCores = 16, ramMb = 32768, batteryPercent = 100, isCharging = true,
            networkType = NetworkType.ETHERNET, bandwidthMbps = 1000, deviceType = "DESKTOP",
            screenWidth = 3840, screenHeight = 2160, supportedCodecs = listOf("VP8", "VP9", "H264", "AV1", "OPUS"),
            installedModules = emptyList(),
        )
        val phone = DeviceCapability(
            cpuCores = 8, ramMb = 8192, batteryPercent = 60, isCharging = false,
            networkType = NetworkType.WIFI, bandwidthMbps = 50, deviceType = "PHONE",
            screenWidth = 1080, screenHeight = 2400, supportedCodecs = listOf("VP8", "H264", "OPUS"),
            installedModules = emptyList(),
        )

        val desktopScore = CapabilityScorer.calculateScore(desktop)
        val phoneScore = CapabilityScorer.calculateScore(phone)

        assertTrue(desktopScore > phoneScore, "Desktop ($desktopScore) should score higher than phone ($phoneScore)")
    }

    @Test
    fun `charging device scores higher than same device on battery`() {
        val base = DeviceCapability(
            cpuCores = 8, ramMb = 8192, batteryPercent = 50, isCharging = false,
            networkType = NetworkType.WIFI, bandwidthMbps = 50, deviceType = "PHONE",
            screenWidth = 1080, screenHeight = 2400, supportedCodecs = listOf("VP8", "H264"),
            installedModules = emptyList(),
        )
        val charging = base.copy(isCharging = true)

        val batteryScore = CapabilityScorer.calculateScore(base)
        val chargingScore = CapabilityScorer.calculateScore(charging)

        assertTrue(chargingScore > batteryScore, "Charging ($chargingScore) should score higher than battery ($batteryScore)")
    }

    @Test
    fun `scoring matches expected formula exactly`() {
        val caps = DeviceCapability(
            cpuCores = 8, ramMb = 12288, batteryPercent = 85, isCharging = false,
            networkType = NetworkType.WIFI, bandwidthMbps = 50, deviceType = "PHONE",
            screenWidth = 1440, screenHeight = 3200, supportedCodecs = listOf("VP8", "VP9", "H264", "OPUS"),
            installedModules = emptyList(),
        )

        val expected = 8 * 10 +       // cpuCores * 10 = 80
            12288 / 100 +              // ramMb / 100 = 122
            85 * 2 +                   // batteryPercent * 2 (not charging) = 170
            50 * 5 +                   // bandwidthMbps * 5 = 250
            0 +                        // not desktop
            0 +                        // not ethernet
            1440 / 10 +                // screenWidth / 10 = 144
            4 * 15                     // 4 codecs * 15 = 60

        assertEquals(expected, CapabilityScorer.calculateScore(caps))
    }

    @Test
    fun `dto scoring handles null fields gracefully`() {
        val dto = DeviceCapabilityDto(
            cpuCores = 4,
            ramMb = 4096,
            batteryPercent = null,
            isCharging = null,
            networkType = null,
            bandwidthMbps = null,
            deviceType = null,
            screenWidth = null,
            screenHeight = null,
            supportedCodecs = null,
            modules = null,
        )

        val score = CapabilityScorer.calculateScore(dto)
        // Should not crash, all nulls treated as 0
        val expected = 4 * 10 + 4096 / 100 + 0 * 2 + 0 + 0 + 0 + 0 + 0
        assertEquals(expected, score)
    }
}
