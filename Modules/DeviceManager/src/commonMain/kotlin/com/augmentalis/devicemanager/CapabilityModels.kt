// Author: Manoj Jhawar
// Purpose: Cross-platform device capability data models

package com.augmentalis.devicemanager

import kotlinx.serialization.Serializable

/**
 * Device type enumeration
 */
enum class KmpDeviceType {
    PHONE,
    TABLET,
    WATCH,
    TV,
    DESKTOP,
    SMART_GLASS,
    XR_HEADSET,
    AUTOMOTIVE,
    UNKNOWN
}

/**
 * Performance classification for adaptive UX
 */
enum class PerformanceClass {
    LOW_END,    // Budget devices, optimize for performance
    MID_RANGE,  // Mainstream devices, balanced quality
    HIGH_END    // Flagship devices, maximum quality
}

/**
 * Basic device information
 */
@Serializable
data class KmpDeviceInfo(
    val manufacturer: String,
    val model: String,
    val brand: String = "",
    val device: String = "",
    val osVersion: String,
    val osVersionCode: Int = 0,
    val deviceType: KmpDeviceType = KmpDeviceType.UNKNOWN
)

/**
 * Hardware profile information
 */
@Serializable
data class HardwareProfile(
    val cpuCores: Int,
    val cpuArchitecture: String,
    val cpuMaxFrequencyMhz: Int = 0,
    val totalRamMb: Int,
    val availableRamMb: Int = 0,
    val gpuVendor: String = "",
    val gpuRenderer: String = "",
    val internalStorageGb: Int = 0
)

/**
 * Network capability flags
 */
@Serializable
data class NetworkCapabilities(
    val hasBluetooth: Boolean = false,
    val hasBluetoothLE: Boolean = false,
    val hasWiFi: Boolean = false,
    val hasWiFiDirect: Boolean = false,
    val hasWiFiAware: Boolean = false,
    val hasNfc: Boolean = false,
    val hasUwb: Boolean = false,
    val hasCellular: Boolean = false,
    val has5G: Boolean = false
)

/**
 * Sensor capability flags
 */
@Serializable
data class SensorCapabilities(
    val hasAccelerometer: Boolean = false,
    val hasGyroscope: Boolean = false,
    val hasMagnetometer: Boolean = false,
    val hasBarometer: Boolean = false,
    val hasProximity: Boolean = false,
    val hasLight: Boolean = false,
    val hasStepCounter: Boolean = false,
    val hasHeartRate: Boolean = false,
    val totalSensorCount: Int = 0
)

/**
 * Display capability information
 */
@Serializable
data class DisplayCapabilities(
    val widthPixels: Int = 0,
    val heightPixels: Int = 0,
    val densityDpi: Int = 0,
    val refreshRate: Float = 60f,
    val isHdr: Boolean = false,
    val isWideColorGamut: Boolean = false,
    val hasXrSupport: Boolean = false
)

/**
 * Biometric capability flags
 */
@Serializable
data class BiometricCapabilities(
    val hasFingerprint: Boolean = false,
    val hasFace: Boolean = false,
    val hasIris: Boolean = false,
    val biometricLevel: String = "none"
)

/**
 * Device fingerprint for identification and licensing
 */
@Serializable
data class DeviceFingerprint(
    val value: String,
    val type: String = "hardware",
    val components: List<String> = emptyList(),
    val timestamp: Long = 0L
) {
    /**
     * Short fingerprint for display (first 8 chars)
     */
    fun shortValue(): String = if (value.length >= 8) value.take(8) else value
}

/**
 * Comprehensive device capabilities container
 */
@Serializable
data class DeviceCapabilities(
    val deviceInfo: KmpDeviceInfo,
    val hardware: HardwareProfile,
    val network: NetworkCapabilities,
    val sensors: SensorCapabilities,
    val display: DisplayCapabilities,
    val biometric: BiometricCapabilities?,
    val fingerprint: DeviceFingerprint,
    val performanceClass: PerformanceClass = PerformanceClass.MID_RANGE
)
