package com.augmentalis.netavanue.capability

import com.augmentalis.netavanue.signaling.NetworkType
import java.awt.Toolkit
import java.net.NetworkInterface

/**
 * Desktop (JVM) implementation of [CapabilityCollector].
 *
 * Uses Java Runtime and AWT APIs to gather system info.
 * Desktop devices are always treated as DESKTOP type and get
 * a scoring bonus (isCharging = true equivalent since they're plugged in).
 */
actual class CapabilityCollector {
    actual suspend fun collect(): DeviceCapability {
        val runtime = Runtime.getRuntime()
        val screenSize = try {
            val toolkit = Toolkit.getDefaultToolkit()
            toolkit.screenSize.width to toolkit.screenSize.height
        } catch (_: Exception) {
            1920 to 1080
        }

        return DeviceCapability(
            cpuCores = runtime.availableProcessors(),
            ramMb = (runtime.maxMemory() / (1024 * 1024)).toInt(),
            batteryPercent = 100, // Desktop always "full"
            isCharging = true, // Desktop always plugged in
            networkType = detectNetworkType(),
            bandwidthMbps = estimateBandwidth(),
            deviceType = "DESKTOP",
            screenWidth = screenSize.first,
            screenHeight = screenSize.second,
            supportedCodecs = listOf("VP8", "VP9", "H264", "AV1", "OPUS"),
            installedModules = emptyList(),
        )
    }

    private fun detectNetworkType(): NetworkType {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces().toList()
            val active = interfaces.filter { it.isUp && !it.isLoopback && !it.isVirtual }
            when {
                active.any { it.name.startsWith("eth") || it.name.startsWith("en") } -> NetworkType.ETHERNET
                active.any { it.name.startsWith("wl") || it.name.startsWith("Wi-Fi") } -> NetworkType.WIFI
                else -> NetworkType.UNKNOWN
            }
        } catch (_: Exception) {
            NetworkType.UNKNOWN
        }
    }

    private fun estimateBandwidth(): Int {
        // JVM doesn't expose bandwidth directly; conservative estimate
        return 100 // Assume 100 Mbps for desktop
    }
}
