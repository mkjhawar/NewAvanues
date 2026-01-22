// Author: Manoj Jhawar
// Purpose: Desktop (JVM) actual implementation of DeviceCapabilityFactory

package com.augmentalis.devicemanager

import java.io.File
import java.net.InetAddress

/**
 * Desktop (JVM) implementation of DeviceCapabilityFactory
 */
actual object DeviceCapabilityFactory {
    private var provider: DeviceCapabilityProvider? = null

    /**
     * Create desktop device capability provider
     */
    actual fun create(): DeviceCapabilityProvider {
        provider?.let { return it }

        val newProvider = DesktopDeviceCapabilityProvider()
        provider = newProvider
        return newProvider
    }
}

/**
 * Desktop (JVM) implementation of DeviceCapabilityProvider
 */
internal class DesktopDeviceCapabilityProvider : DeviceCapabilityProvider {

    private val runtime = Runtime.getRuntime()
    private val osName = System.getProperty("os.name") ?: "Unknown"
    private val osVersion = System.getProperty("os.version") ?: "Unknown"
    private val osArch = System.getProperty("os.arch") ?: "Unknown"

    override fun getKmpDeviceInfo(): KmpDeviceInfo {
        val manufacturer = detectManufacturer()
        val model = detectModel()

        return KmpDeviceInfo(
            manufacturer = manufacturer,
            model = model,
            brand = determineBrand(),
            device = System.getProperty("os.name") ?: "Desktop",
            osVersion = osVersion,
            osVersionCode = parseOsVersionCode(),
            deviceType = KmpDeviceType.DESKTOP
        )
    }

    override fun getHardwareProfile(): HardwareProfile {
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()

        return HardwareProfile(
            cpuCores = runtime.availableProcessors(),
            cpuArchitecture = osArch,
            cpuMaxFrequencyMhz = 0, // Not easily available on JVM
            totalRamMb = (maxMemory / (1024 * 1024)).toInt(),
            availableRamMb = ((maxMemory - totalMemory + freeMemory) / (1024 * 1024)).toInt(),
            gpuVendor = "Unknown", // Requires native access
            gpuRenderer = "Unknown",
            internalStorageGb = getStorageGb()
        )
    }

    override fun getNetworkCapabilities(): NetworkCapabilities {
        // Desktop typically has WiFi, may have Bluetooth
        return NetworkCapabilities(
            hasBluetooth = checkBluetoothAvailable(),
            hasBluetoothLE = checkBluetoothAvailable(),
            hasWiFi = true, // Assume network available
            hasWiFiDirect = false,
            hasWiFiAware = false,
            hasNfc = false,
            hasUwb = false,
            hasCellular = false,
            has5G = false
        )
    }

    override fun getSensorCapabilities(): SensorCapabilities {
        // Desktop typically has no sensors
        return SensorCapabilities(
            hasAccelerometer = false,
            hasGyroscope = false,
            hasMagnetometer = false,
            hasBarometer = false,
            hasProximity = false,
            hasLight = false,
            hasStepCounter = false,
            hasHeartRate = false,
            totalSensorCount = 0
        )
    }

    override fun getDisplayCapabilities(): DisplayCapabilities {
        // Would need AWT/Swing for actual display info
        return DisplayCapabilities(
            widthPixels = 1920,
            heightPixels = 1080,
            densityDpi = 96,
            refreshRate = 60f,
            isHdr = false,
            isWideColorGamut = false,
            hasXrSupport = false
        )
    }

    override fun getBiometricCapabilities(): BiometricCapabilities? {
        // Desktop biometrics varies widely
        return null
    }

    override fun getDeviceFingerprint(): DeviceFingerprint {
        val components = listOf(
            osName,
            osVersion,
            osArch,
            runtime.availableProcessors().toString(),
            System.getProperty("user.name") ?: "",
            getHostname()
        )

        val fingerprint = components.joinToString("-").hashCode().toString(16)

        return DeviceFingerprint(
            value = fingerprint,
            type = "system",
            components = listOf("os", "arch", "hostname", "user"),
            timestamp = System.currentTimeMillis()
        )
    }

    override fun getPerformanceClass(): PerformanceClass {
        val cores = runtime.availableProcessors()
        val ramGb = runtime.maxMemory() / (1024 * 1024 * 1024)

        return when {
            cores >= 8 && ramGb >= 16 -> PerformanceClass.HIGH_END
            cores >= 4 && ramGb >= 8 -> PerformanceClass.MID_RANGE
            else -> PerformanceClass.LOW_END
        }
    }

    override suspend fun refreshCapabilities(): DeviceCapabilities {
        // Desktop capabilities are mostly static
        return getAllCapabilities()
    }

    private fun detectManufacturer(): String {
        return when {
            osName.contains("Mac", ignoreCase = true) -> "Apple"
            osName.contains("Windows", ignoreCase = true) -> detectWindowsManufacturer()
            else -> "Unknown"
        }
    }

    private fun detectWindowsManufacturer(): String {
        // Could use WMI or registry, but keep simple for now
        return System.getenv("COMPUTERNAME")?.let { "PC" } ?: "Unknown"
    }

    private fun detectModel(): String {
        return when {
            osName.contains("Mac", ignoreCase = true) -> detectMacModel()
            else -> osName
        }
    }

    private fun detectMacModel(): String {
        return try {
            val process = ProcessBuilder("sysctl", "-n", "hw.model").start()
            process.inputStream.bufferedReader().readLine()?.trim() ?: "Mac"
        } catch (e: Exception) {
            "Mac"
        }
    }

    private fun determineBrand(): String {
        return when {
            osName.contains("Mac", ignoreCase = true) -> "Apple"
            osName.contains("Windows", ignoreCase = true) -> "Microsoft"
            else -> "Linux"
        }
    }

    private fun parseOsVersionCode(): Int {
        return try {
            osVersion.split(".").firstOrNull()?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getStorageGb(): Int {
        return try {
            val roots = File.listRoots()
            val totalSpace = roots.sumOf { it.totalSpace }
            (totalSpace / (1024 * 1024 * 1024)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun getHostname(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun checkBluetoothAvailable(): Boolean {
        // Simplified check - would need native access for proper detection
        return when {
            osName.contains("Mac", ignoreCase = true) -> true
            osName.contains("Windows", ignoreCase = true) -> true
            else -> false
        }
    }
}
