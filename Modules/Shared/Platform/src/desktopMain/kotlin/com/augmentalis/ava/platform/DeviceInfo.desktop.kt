package com.augmentalis.ava.platform

import java.lang.management.ManagementFactory
import java.net.InetAddress
import java.net.NetworkInterface
import java.security.MessageDigest
import java.util.*

/**
 * Desktop (JVM) implementation of DeviceInfo.
 *
 * Uses System properties, Runtime, and ManagementFactory for device information.
 */
actual class DeviceInfo actual constructor() {

    private val osName = System.getProperty("os.name", "Unknown")
    private val osVersion = System.getProperty("os.version", "Unknown")
    private val osArch = System.getProperty("os.arch", "Unknown")
    private val runtime = Runtime.getRuntime()

    actual fun getPlatform(): PlatformType {
        val os = osName.lowercase()
        return when {
            os.contains("win") -> PlatformType.DESKTOP_WINDOWS
            os.contains("mac") || os.contains("darwin") -> PlatformType.DESKTOP_MACOS
            os.contains("linux") || os.contains("nix") || os.contains("nux") -> PlatformType.DESKTOP_LINUX
            else -> PlatformType.DESKTOP_LINUX // Default fallback
        }
    }

    actual fun getManufacturer(): String {
        // Desktop doesn't have a standard manufacturer property
        // Use OS vendor or "Generic"
        return System.getProperty("java.vendor", "Generic")
    }

    actual fun getModel(): String {
        // Combine OS name and architecture as model identifier
        return "$osName ($osArch)"
    }

    actual fun getOsVersion(): String {
        return osVersion
    }

    actual fun getSdkVersion(): Int {
        // Not applicable to desktop platforms
        return 0
    }

    actual fun getMemoryInfo(): MemoryInfo {
        val maxMemory = runtime.maxMemory()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()

        // Available memory is the amount we can still allocate
        val availableMemory = maxMemory - (totalMemory - freeMemory)
        val usedMemory = maxMemory - availableMemory

        return MemoryInfo(
            totalMemory = maxMemory,
            availableMemory = availableMemory,
            usedMemory = usedMemory
        )
    }

    actual fun getBatteryInfo(): BatteryInfo {
        // Desktop battery info is not easily accessible via standard Java APIs
        // Would require platform-specific native code
        return BatteryInfo(
            level = 100,
            isCharging = false,
            isPowerSaveMode = false
        )
    }

    actual fun isLowMemory(): Boolean {
        val memInfo = getMemoryInfo()
        return memInfo.memoryPercentUsed > 85f
    }

    actual fun getAppVersion(): String {
        // Read from system property or manifest
        // This should be set by the application at startup
        return System.getProperty("app.version", "1.0.0")
    }

    actual fun getAppVersionCode(): Long {
        // Read from system property
        return System.getProperty("app.version.code", "1")?.toLongOrNull() ?: 1L
    }

    actual fun getDeviceId(): String {
        // Generate a unique but anonymized device ID based on hardware properties
        // Use MAC address, hostname, and OS properties
        return try {
            val hostname = InetAddress.getLocalHost().hostName
            val mac = getMacAddress()
            val properties = "$hostname-$mac-$osName-$osArch"

            // Hash to anonymize
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(properties.toByteArray())
            hash.joinToString("") { "%02x".format(it) }.take(32)
        } catch (e: Exception) {
            // Fallback to UUID stored in preferences
            generateFallbackDeviceId()
        }
    }

    actual fun getLocale(): String {
        val locale = Locale.getDefault()
        return "${locale.language}-${locale.country}"
    }

    actual fun hasFeature(feature: String): Boolean {
        return when (feature) {
            DeviceFeatures.FEATURE_MICROPHONE -> true  // Assume present
            DeviceFeatures.FEATURE_CAMERA -> true      // Assume present
            DeviceFeatures.FEATURE_BLUETOOTH -> true   // Assume present
            DeviceFeatures.FEATURE_WIFI -> true        // Assume present
            DeviceFeatures.FEATURE_NFC -> false        // Rare on desktop
            DeviceFeatures.FEATURE_BIOMETRICS -> false // Platform-specific
            DeviceFeatures.FEATURE_TELEPHONY -> false  // Not available on desktop
            else -> false
        }
    }

    private fun getMacAddress(): String {
        return try {
            val network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
            val mac = network?.hardwareAddress
            mac?.joinToString("-") { "%02X".format(it) } ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun generateFallbackDeviceId(): String {
        // Generate and cache a UUID-based device ID
        val prefs = java.util.prefs.Preferences.userRoot()
            .node("com.augmentalis.ava.device")

        var deviceId = prefs.get("device_id", null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.put("device_id", deviceId)
            prefs.flush()
        }

        return deviceId
    }
}

/**
 * Factory for creating DeviceInfo instances on Desktop.
 */
actual object DeviceInfoFactory {
    actual fun create(): DeviceInfo {
        return DeviceInfo()
    }
}
