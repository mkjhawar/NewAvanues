package com.augmentalis.netavanue.capability

import com.augmentalis.netavanue.signaling.NetworkType

/**
 * JS/Browser implementation of [CapabilityCollector].
 *
 * Uses the Navigator API and Screen API to gather browser capabilities.
 * Browser environments have limited access to hardware information compared
 * to native platforms, so some values are estimated.
 */
actual class CapabilityCollector {
    actual suspend fun collect(): DeviceCapability {
        val cpuCores = js("typeof navigator !== 'undefined' ? navigator.hardwareConcurrency : 1") as Int
        val screenWidth = js("typeof screen !== 'undefined' ? screen.width : 1920") as Int
        val screenHeight = js("typeof screen !== 'undefined' ? screen.height : 1080") as Int

        // Browsers expose limited RAM info — deviceMemory is approximate GB
        val deviceMemoryGb = js(
            "typeof navigator !== 'undefined' && navigator.deviceMemory ? navigator.deviceMemory : 4"
        ) as Number
        val ramMb = (deviceMemoryGb.toDouble() * 1024).toInt()

        return DeviceCapability(
            cpuCores = cpuCores,
            ramMb = ramMb,
            batteryPercent = getBatteryPercent(),
            isCharging = getIsCharging(),
            networkType = detectNetworkType(),
            bandwidthMbps = estimateBandwidth(),
            deviceType = detectDeviceType(screenWidth),
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            supportedCodecs = listOf("VP8", "VP9", "H264", "OPUS"),
            installedModules = emptyList(),
        )
    }

    private fun getBatteryPercent(): Int {
        // Battery API may not be available in all browsers
        return 100 // Conservative default — Battery API requires async
    }

    private fun getIsCharging(): Boolean {
        return true // Conservative default — Battery API requires async
    }

    private fun detectNetworkType(): NetworkType {
        // Network Information API (limited browser support)
        val connectionType = js(
            "typeof navigator !== 'undefined' && navigator.connection " +
                "? navigator.connection.effectiveType : null"
        )
        return when (connectionType) {
            "4g" -> NetworkType.WIFI // 4G effective type likely means good connection
            "3g" -> NetworkType.CELLULAR_4G
            "2g" -> NetworkType.CELLULAR_3G
            "slow-2g" -> NetworkType.CELLULAR_2G
            else -> NetworkType.UNKNOWN
        }
    }

    private fun estimateBandwidth(): Int {
        // Use Network Information API downlink if available
        val downlink = js(
            "typeof navigator !== 'undefined' && navigator.connection " +
                "? navigator.connection.downlink : null"
        )
        return if (downlink != null) {
            (downlink as Number).toInt()
        } else {
            50 // Conservative estimate (50 Mbps)
        }
    }

    private fun detectDeviceType(screenWidth: Int): String {
        return when {
            screenWidth <= 768 -> "PHONE"
            screenWidth <= 1024 -> "TABLET"
            else -> "DESKTOP"
        }
    }
}
