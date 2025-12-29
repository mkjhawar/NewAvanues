package com.augmentalis.ava.platform

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
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

    // ==================== FINGERPRINTING ====================

    private val fingerprintVersion = 1
    private var cachedFingerprint: DeviceFingerprint? = null

    private val prefs: java.util.prefs.Preferences by lazy {
        java.util.prefs.Preferences.userRoot().node("com.augmentalis.ava.laas")
    }

    actual fun getFingerprint(): DeviceFingerprint {
        cachedFingerprint?.let { return it }

        val components = mutableListOf<String>()

        // 1. Machine ID (primary - most stable on desktop)
        val machineId = getMachineId()
        val machineIdPresent = machineId.isNotEmpty() && machineId != "unknown"
        if (machineIdPresent) {
            components.add("mid:${sha256(machineId)}")
        }

        // 2. MAC address (always available on desktop)
        val mac = getMacAddress()
        val macPresent = mac.isNotEmpty() && mac != "Unknown"
        if (macPresent) {
            components.add("mac:${sha256(mac)}")
        }

        // 3. OS/System information (very stable)
        val systemInfo = collectSystemInfo()
        components.add("sys:${sha256(systemInfo)}")

        // 4. Hardware characteristics
        val hardwareInfo = collectHardwareInfo()
        components.add("hw:${sha256(hardwareInfo)}")

        // 5. Hostname (moderately stable)
        val hostname = getHostname()
        if (hostname.isNotEmpty()) {
            components.add("host:${sha256(hostname)}")
        }

        // 6. Installation ID (consistency fallback)
        val installId = getOrCreateInstallId()
        components.add("inst:${sha256(installId)}")

        // Combine all components
        val combined = "v$fingerprintVersion:" + components.joinToString("|")
        val fingerprint = sha256(combined)

        val result = DeviceFingerprint(
            fingerprint = fingerprint,
            fingerprintShort = fingerprint.take(16).uppercase(),
            version = fingerprintVersion,
            platform = getPlatform(),
            deviceType = getDeviceType(),
            isStable = (machineIdPresent || macPresent),
            generatedAt = System.currentTimeMillis()
        )

        cachedFingerprint = result
        return result
    }

    actual fun getFingerprintString(): String = getFingerprint().fingerprint

    actual fun getFingerprintShort(): String = getFingerprint().fingerprintShort

    actual fun getFingerprintDebugInfo(): FingerprintDebugInfo {
        val machineId = getMachineId()
        val mac = getMacAddress()
        return FingerprintDebugInfo(
            primaryIdPresent = machineId.isNotEmpty() && machineId != "unknown",
            buildInfoPresent = osVersion.isNotEmpty(),
            macAddressPresent = mac.isNotEmpty() && mac != "Unknown",
            hardwareInfoPresent = true,
            displayInfoPresent = false, // Desktop doesn't use display for fingerprinting
            installIdPresent = getOrCreateInstallId().isNotEmpty()
        )
    }

    actual fun getDeviceType(): DeviceType = DeviceType.DESKTOP

    // ==================== FINGERPRINT HELPERS ====================

    private fun getMachineId(): String {
        return try {
            when {
                // Windows: Query MachineGuid from registry
                osName.lowercase().contains("win") -> {
                    getWindowsMachineId()
                }
                // macOS: Use hardware UUID
                osName.lowercase().contains("mac") || osName.lowercase().contains("darwin") -> {
                    getMacOSMachineId()
                }
                // Linux: Read /etc/machine-id or /var/lib/dbus/machine-id
                else -> {
                    getLinuxMachineId()
                }
            }
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getWindowsMachineId(): String {
        return try {
            val process = ProcessBuilder(
                "reg", "query",
                "HKLM\\SOFTWARE\\Microsoft\\Cryptography",
                "/v", "MachineGuid"
            ).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()

            // Parse the registry output for the GUID
            val regex = Regex("MachineGuid\\s+REG_SZ\\s+([\\w-]+)")
            regex.find(output)?.groupValues?.get(1) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getMacOSMachineId(): String {
        return try {
            val process = ProcessBuilder(
                "ioreg", "-rd1", "-c", "IOPlatformExpertDevice"
            ).start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()

            // Parse the output for IOPlatformUUID
            val regex = Regex("\"IOPlatformUUID\"\\s*=\\s*\"([\\w-]+)\"")
            regex.find(output)?.groupValues?.get(1) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getLinuxMachineId(): String {
        return try {
            // Try /etc/machine-id first (systemd)
            val machineIdFile = File("/etc/machine-id")
            if (machineIdFile.exists()) {
                return machineIdFile.readText().trim()
            }

            // Fallback to /var/lib/dbus/machine-id (older systems)
            val dbusIdFile = File("/var/lib/dbus/machine-id")
            if (dbusIdFile.exists()) {
                return dbusIdFile.readText().trim()
            }

            "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun collectSystemInfo(): String {
        return buildString {
            append(osName)
            append(osVersion)
            append(osArch)
            append(System.getProperty("os.name.real", ""))
            append(System.getProperty("java.vendor", ""))
        }
    }

    private fun collectHardwareInfo(): String {
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        return buildString {
            append(runtime.availableProcessors())
            append(osBean.arch)
            append(osBean.name)
            append(System.getProperty("sun.arch.data.model", ""))
            // Try to get total memory if available
            try {
                val method = osBean.javaClass.getMethod("getTotalPhysicalMemorySize")
                method.isAccessible = true
                append(method.invoke(osBean))
            } catch (e: Exception) {
                append(runtime.maxMemory())
            }
        }
    }

    private fun getHostname(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            ""
        }
    }

    private fun getOrCreateInstallId(): String {
        val key = "install_id"
        var id = prefs.get(key, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.put(key, id)
            prefs.flush()
        }
        return id
    }

    private fun sha256(input: String): String {
        if (input.isEmpty()) return ""
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
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
