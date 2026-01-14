package com.augmentalis.ava.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Foundation.NSBundle
import platform.Foundation.NSDate
import platform.Foundation.NSProcessInfo
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.UIKit.UIDeviceBatteryState
import platform.UIKit.UIScreen
import platform.UIKit.UIUserInterfaceIdiom

/**
 * iOS implementation of DeviceInfo using UIDevice and ProcessInfo.
 */
@OptIn(ExperimentalForeignApi::class)
actual class DeviceInfo actual constructor() {
    private val device = UIDevice.currentDevice
    private val processInfo = NSProcessInfo.processInfo

    actual fun getPlatform(): PlatformType {
        return PlatformType.IOS
    }

    actual fun getManufacturer(): String {
        return "Apple"
    }

    actual fun getModel(): String {
        return device.model
    }

    actual fun getOsVersion(): String {
        return device.systemVersion
    }

    actual fun getSdkVersion(): Int {
        return 0
    }

    actual fun getMemoryInfo(): MemoryInfo {
        val physicalMemory = processInfo.physicalMemory.toLong()
        val availableMemory = getAvailableMemory()

        return MemoryInfo(
            totalMemory = physicalMemory,
            availableMemory = availableMemory
        )
    }

    actual fun getBatteryInfo(): BatteryInfo {
        device.batteryMonitoringEnabled = true

        val level = (device.batteryLevel * 100).toInt().coerceIn(0, 100)
        val isCharging = device.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateCharging ||
                        device.batteryState == UIDeviceBatteryState.UIDeviceBatteryStateFull
        // lowPowerModeEnabled is not exposed in Kotlin/Native bindings for NSProcessInfo
        // Return false as this is not critical for SDK functionality
        val isPowerSaveMode = false

        return BatteryInfo(
            level = level,
            isCharging = isCharging,
            isPowerSaveMode = isPowerSaveMode
        )
    }

    actual fun isLowMemory(): Boolean {
        val memInfo = getMemoryInfo()
        return memInfo.memoryPercentUsed > 80f
    }

    actual fun getAppVersion(): String {
        val bundle = NSBundle.mainBundle
        return bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "1.0.0"
    }

    actual fun getAppVersionCode(): Long {
        val bundle = NSBundle.mainBundle
        val versionString = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "1"
        return versionString.toLongOrNull() ?: 1L
    }

    actual fun getDeviceId(): String {
        return device.identifierForVendor?.UUIDString ?: "unknown"
    }

    actual fun getLocale(): String {
        val preferredLanguages = NSBundle.mainBundle.preferredLocalizations
        return preferredLanguages.firstOrNull() as? String ?: "en-US"
    }

    actual fun hasFeature(feature: String): Boolean {
        return when (feature) {
            DeviceFeatures.FEATURE_MICROPHONE -> true
            DeviceFeatures.FEATURE_CAMERA -> true
            DeviceFeatures.FEATURE_BLUETOOTH -> true
            DeviceFeatures.FEATURE_WIFI -> true
            DeviceFeatures.FEATURE_NFC -> {
                // NFC is available on iPhone 7 and later
                val modelName = getModel().lowercase()
                !modelName.contains("ipad") && !modelName.contains("ipod")
            }
            DeviceFeatures.FEATURE_BIOMETRICS -> true
            DeviceFeatures.FEATURE_TELEPHONY -> {
                // Telephony only on iPhone, not iPad or iPod
                val modelName = getModel().lowercase()
                modelName.contains("iphone")
            }
            else -> false
        }
    }

    private fun getAvailableMemory(): Long {
        // iOS doesn't provide a direct API for available memory
        // We can estimate based on physical memory and usage
        val totalMemory = processInfo.physicalMemory.toLong()
        // Assume ~30% is typically available on iOS
        return (totalMemory * 0.3).toLong()
    }

    // ==================== FINGERPRINTING ====================

    private val fingerprintVersion = 1
    private var cachedFingerprint: DeviceFingerprint? = null

    private val userDefaults: NSUserDefaults by lazy {
        NSUserDefaults.standardUserDefaults
    }

    actual fun getFingerprint(): DeviceFingerprint {
        cachedFingerprint?.let { return it }

        val components = mutableListOf<String>()

        // 1. IDFV (primary - most stable for app/vendor)
        val idfv = getIdentifierForVendor()
        val idfvPresent = idfv.isNotEmpty() && idfv != "unknown"
        if (idfvPresent) {
            components.add("idfv:${sha256(idfv)}")
        }

        // 2. Model information (stable)
        val modelInfo = collectModelInfo()
        val modelPresent = modelInfo.isNotEmpty()
        components.add("model:${sha256(modelInfo)}")

        // 3. Screen characteristics (stable for device)
        val screenInfo = collectScreenInfo()
        components.add("screen:${sha256(screenInfo)}")

        // 4. System information
        val systemInfo = collectSystemInfo()
        components.add("sys:${sha256(systemInfo)}")

        // 5. Hardware characteristics
        val hardwareInfo = collectHardwareInfo()
        components.add("hw:${sha256(hardwareInfo)}")

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
            platform = PlatformType.IOS,
            deviceType = getDeviceType(),
            isStable = idfvPresent && modelPresent,
            generatedAt = (NSDate().timeIntervalSince1970 * 1000).toLong()
        )

        cachedFingerprint = result
        return result
    }

    actual fun getFingerprintString(): String = getFingerprint().fingerprint

    actual fun getFingerprintShort(): String = getFingerprint().fingerprintShort

    actual fun getFingerprintDebugInfo(): FingerprintDebugInfo {
        val idfv = getIdentifierForVendor()
        return FingerprintDebugInfo(
            primaryIdPresent = idfv.isNotEmpty() && idfv != "unknown",
            buildInfoPresent = device.systemVersion.isNotEmpty(),
            macAddressPresent = false, // MAC not available on iOS since iOS 7
            hardwareInfoPresent = true,
            displayInfoPresent = true,
            installIdPresent = getOrCreateInstallId().isNotEmpty()
        )
    }

    actual fun getDeviceType(): DeviceType {
        // Use simple model-based detection to avoid enum interop issues
        val model = device.model.lowercase()
        return when {
            model.contains("iphone") -> DeviceType.PHONE
            model.contains("ipad") -> DeviceType.TABLET
            model.contains("tv") -> DeviceType.TV
            model.contains("watch") -> DeviceType.WATCH
            else -> DeviceType.UNKNOWN
        }
    }

    // ==================== FINGERPRINT HELPERS ====================

    private fun getIdentifierForVendor(): String {
        return device.identifierForVendor?.UUIDString ?: ""
    }

    private fun collectModelInfo(): String {
        return buildString {
            append(device.model)
            append(device.name)
            append(device.localizedModel)
        }
    }

    private fun collectScreenInfo(): String {
        val screen = UIScreen.mainScreen
        return screen.bounds.useContents {
            buildString {
                append(size.width.toInt())
                append(size.height.toInt())
                append(screen.scale.toInt())
                append(screen.nativeScale.toInt())
            }
        }
    }

    private fun collectSystemInfo(): String {
        return buildString {
            append(device.systemName)
            append(device.systemVersion)
            append(processInfo.operatingSystemVersionString)
        }
    }

    private fun collectHardwareInfo(): String {
        return buildString {
            append(processInfo.processorCount)
            append(processInfo.physicalMemory)
            append(processInfo.activeProcessorCount)
        }
    }

    private fun getOrCreateInstallId(): String {
        val key = "laas_install_id"
        var id = userDefaults.stringForKey(key)
        if (id == null) {
            id = NSUUID().UUIDString
            userDefaults.setObject(id, key)
            userDefaults.synchronize()
        }
        return id
    }

    private fun sha256(input: String): String {
        if (input.isEmpty()) return ""
        return try {
            val data = input.encodeToByteArray()
            val hash = UByteArray(CC_SHA256_DIGEST_LENGTH)

            data.usePinned { pinnedData ->
                hash.usePinned { pinnedHash ->
                    CC_SHA256(
                        pinnedData.addressOf(0),
                        data.size.convert(),
                        pinnedHash.addressOf(0)
                    )
                }
            }

            hash.joinToString("") { byte ->
                byte.toString(16).padStart(2, '0')
            }
        } catch (e: Exception) {
            ""
        }
    }
}

/**
 * iOS factory for DeviceInfo.
 */
actual object DeviceInfoFactory {
    actual fun create(): DeviceInfo {
        return DeviceInfo()
    }
}
