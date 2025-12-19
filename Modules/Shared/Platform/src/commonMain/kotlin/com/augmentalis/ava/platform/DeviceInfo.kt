package com.augmentalis.ava.platform

/**
 * Platform type enumeration.
 */
enum class PlatformType {
    ANDROID,
    IOS,
    DESKTOP_WINDOWS,
    DESKTOP_MACOS,
    DESKTOP_LINUX,
    WEB
}

/**
 * Device memory information.
 */
data class MemoryInfo(
    val totalMemory: Long,
    val availableMemory: Long,
    val usedMemory: Long = totalMemory - availableMemory,
    val memoryPercentUsed: Float = if (totalMemory > 0) (usedMemory.toFloat() / totalMemory) * 100f else 0f
)

/**
 * Battery information.
 */
data class BatteryInfo(
    val level: Int,          // 0-100
    val isCharging: Boolean,
    val isPowerSaveMode: Boolean = false
)

/**
 * Cross-platform device information abstraction.
 *
 * Platform implementations:
 * - Android: Build class, ActivityManager, BatteryManager
 * - iOS: UIDevice, ProcessInfo
 * - Desktop: System properties, Runtime
 */
expect class DeviceInfo() {

    /**
     * Get the platform type.
     */
    fun getPlatform(): PlatformType

    /**
     * Get the device manufacturer (e.g., "Samsung", "Apple", "Dell").
     */
    fun getManufacturer(): String

    /**
     * Get the device model (e.g., "Pixel 7", "iPhone 15", "MacBook Pro").
     */
    fun getModel(): String

    /**
     * Get the OS version string.
     */
    fun getOsVersion(): String

    /**
     * Get the SDK/API level (Android-specific, returns 0 on other platforms).
     */
    fun getSdkVersion(): Int

    /**
     * Get memory information.
     */
    fun getMemoryInfo(): MemoryInfo

    /**
     * Get battery information.
     */
    fun getBatteryInfo(): BatteryInfo

    /**
     * Check if device is in low memory state.
     */
    fun isLowMemory(): Boolean

    /**
     * Get the app version name.
     */
    fun getAppVersion(): String

    /**
     * Get the app version code/build number.
     */
    fun getAppVersionCode(): Long

    /**
     * Get unique device identifier (anonymized where required).
     */
    fun getDeviceId(): String

    /**
     * Get the current locale/language code (e.g., "en-US").
     */
    fun getLocale(): String

    /**
     * Check if device supports a specific feature.
     */
    fun hasFeature(feature: String): Boolean
}

/**
 * Factory for creating DeviceInfo instances.
 */
expect object DeviceInfoFactory {
    fun create(): DeviceInfo
}

/**
 * Common feature constants.
 */
object DeviceFeatures {
    const val FEATURE_MICROPHONE = "microphone"
    const val FEATURE_CAMERA = "camera"
    const val FEATURE_BLUETOOTH = "bluetooth"
    const val FEATURE_WIFI = "wifi"
    const val FEATURE_NFC = "nfc"
    const val FEATURE_BIOMETRICS = "biometrics"
    const val FEATURE_TELEPHONY = "telephony"
}
