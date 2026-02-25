package com.augmentalis.netavanue.capability

import com.augmentalis.netavanue.signaling.NetworkType
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.Foundation.NSProcessInfo

/**
 * iOS implementation of [CapabilityCollector].
 *
 * Uses UIKit and Foundation APIs:
 * - CPU cores from NSProcessInfo.processInfo.processorCount
 * - RAM from NSProcessInfo.processInfo.physicalMemory
 * - Battery from UIDevice.currentDevice (must enable battery monitoring)
 * - Screen from UIScreen.mainScreen
 */
actual class CapabilityCollector {
    actual suspend fun collect(): DeviceCapability {
        val processInfo = NSProcessInfo.processInfo
        val device = UIDevice.currentDevice
        val screen = UIScreen.mainScreen

        device.batteryMonitoringEnabled = true
        val batteryLevel = device.batteryLevel
        val batteryPercent = if (batteryLevel < 0) 50 else (batteryLevel * 100).toInt()
        val isCharging = device.batteryState.toInt() == 2 || device.batteryState.toInt() == 3 // Charging or Full

        val screenWidth = (screen.bounds.size.width * screen.scale).toInt()
        val screenHeight = (screen.bounds.size.height * screen.scale).toInt()

        val deviceModel = device.model
        val isTablet = deviceModel.contains("iPad", ignoreCase = true)

        return DeviceCapability(
            cpuCores = processInfo.processorCount.toInt(),
            ramMb = (processInfo.physicalMemory / (1024uL * 1024uL)).toInt(),
            batteryPercent = batteryPercent,
            isCharging = isCharging,
            networkType = NetworkType.WIFI, // Detailed detection requires NWPathMonitor (future)
            bandwidthMbps = 50, // Conservative estimate
            deviceType = if (isTablet) "TABLET" else "PHONE",
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            supportedCodecs = listOf("VP8", "H264", "H265", "OPUS", "AAC"),
            installedModules = emptyList(),
        )
    }
}
