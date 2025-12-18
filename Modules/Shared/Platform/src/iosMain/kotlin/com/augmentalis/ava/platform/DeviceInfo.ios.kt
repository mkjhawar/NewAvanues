package com.augmentalis.ava.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo
import platform.UIKit.UIDevice
import platform.darwin.NSObject

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
        val isCharging = device.batteryState == platform.UIKit.UIDeviceBatteryStateCharging ||
                        device.batteryState == platform.UIKit.UIDeviceBatteryStateFull
        val isPowerSaveMode = processInfo.lowPowerModeEnabled

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
}

/**
 * iOS factory for DeviceInfo.
 */
actual object DeviceInfoFactory {
    actual fun create(): DeviceInfo {
        return DeviceInfo()
    }
}
