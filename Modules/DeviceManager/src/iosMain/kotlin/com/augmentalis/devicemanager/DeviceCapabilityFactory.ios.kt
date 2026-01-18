// Author: Manoj Jhawar
// Purpose: iOS actual implementation of DeviceCapabilityFactory

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.augmentalis.devicemanager

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetWidth
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen
import platform.posix._SC_NPROCESSORS_ONLN
import platform.posix._SC_PAGESIZE
import platform.posix._SC_PHYS_PAGES
import platform.posix.sysconf

/**
 * iOS actual implementation of DeviceCapabilityFactory
 */
actual object DeviceCapabilityFactory {
    private var provider: DeviceCapabilityProvider? = null

    /**
     * Create iOS device capability provider
     */
    actual fun create(): DeviceCapabilityProvider {
        provider?.let { return it }

        val newProvider = IosDeviceCapabilityProvider()
        provider = newProvider
        return newProvider
    }
}

/**
 * iOS implementation of DeviceCapabilityProvider
 */
internal class IosDeviceCapabilityProvider : DeviceCapabilityProvider {

    private val device = UIDevice.currentDevice
    private val screen = UIScreen.mainScreen

    override fun getKmpDeviceInfo(): KmpDeviceInfo {
        return KmpDeviceInfo(
            manufacturer = "Apple",
            model = device.model,
            brand = "Apple",
            device = device.name,
            osVersion = device.systemVersion,
            osVersionCode = parseVersionCode(device.systemVersion),
            deviceType = detectDeviceType()
        )
    }

    override fun getHardwareProfile(): HardwareProfile {
        return HardwareProfile(
            cpuCores = getProcessorCount(),
            cpuArchitecture = getArchitecture(),
            cpuMaxFrequencyMhz = 0, // Not available on iOS
            totalRamMb = getTotalMemoryMb(),
            availableRamMb = getAvailableMemoryMb(),
            gpuVendor = "Apple",
            gpuRenderer = "Metal",
            internalStorageGb = getStorageGb()
        )
    }

    override fun getNetworkCapabilities(): NetworkCapabilities {
        return NetworkCapabilities(
            hasBluetooth = true, // All iOS devices have Bluetooth
            hasBluetoothLE = true,
            hasWiFi = true,
            hasWiFiDirect = false, // iOS uses different approach
            hasWiFiAware = false,
            hasNfc = hasNfcCapability(),
            hasUwb = hasUwbCapability(),
            hasCellular = hasCellularCapability(),
            has5G = false // Would need carrier info
        )
    }

    override fun getSensorCapabilities(): SensorCapabilities {
        return SensorCapabilities(
            hasAccelerometer = true,
            hasGyroscope = true,
            hasMagnetometer = true,
            hasBarometer = hasBarometerCapability(),
            hasProximity = true,
            hasLight = true,
            hasStepCounter = true,
            hasHeartRate = false, // Apple Watch only
            totalSensorCount = 7
        )
    }

    override fun getDisplayCapabilities(): DisplayCapabilities {
        val scale = screen.scale
        // Use nativeBounds for actual pixel dimensions
        val nativeBounds = screen.nativeBounds
        val widthPixels = CGRectGetWidth(nativeBounds).toInt()
        val heightPixels = CGRectGetHeight(nativeBounds).toInt()

        return DisplayCapabilities(
            widthPixels = widthPixels,
            heightPixels = heightPixels,
            densityDpi = (scale * 160).toInt(),
            refreshRate = 60f, // Default, ProMotion devices have 120Hz
            isHdr = false,
            isWideColorGamut = true, // Most modern iOS devices
            hasXrSupport = false
        )
    }

    override fun getBiometricCapabilities(): BiometricCapabilities? {
        return BiometricCapabilities(
            hasFingerprint = hasTouchId(),
            hasFace = hasFaceId(),
            hasIris = false,
            biometricLevel = "strong"
        )
    }

    override fun getDeviceFingerprint(): DeviceFingerprint {
        val components = listOf(
            device.model,
            device.systemVersion,
            device.identifierForVendor?.UUIDString ?: "unknown",
            getProcessorCount().toString()
        )

        val fingerprint = components.joinToString("-").hashCode().toString(16)

        return DeviceFingerprint(
            value = fingerprint,
            type = "hardware",
            components = listOf("model", "os", "vendor_id", "cores"),
            timestamp = currentTimeMillis()
        )
    }

    override fun getPerformanceClass(): PerformanceClass {
        val ramGb = getTotalMemoryMb() / 1024
        val cores = getProcessorCount()

        return when {
            ramGb >= 6 && cores >= 6 -> PerformanceClass.HIGH_END
            ramGb >= 3 && cores >= 4 -> PerformanceClass.MID_RANGE
            else -> PerformanceClass.LOW_END
        }
    }

    override suspend fun refreshCapabilities(): DeviceCapabilities {
        return getAllCapabilities()
    }

    // ===== Private Helpers =====

    private fun detectDeviceType(): KmpDeviceType {
        return when {
            device.model.contains("iPad") -> KmpDeviceType.TABLET
            device.model.contains("Apple TV") -> KmpDeviceType.TV
            device.model.contains("Watch") -> KmpDeviceType.WATCH
            else -> KmpDeviceType.PHONE
        }
    }

    private fun parseVersionCode(version: String): Int {
        return try {
            version.split(".").firstOrNull()?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    }

    private fun getProcessorCount(): Int {
        return sysconf(_SC_NPROCESSORS_ONLN).toInt()
    }

    private fun getArchitecture(): String {
        return "arm64" // All modern iOS devices
    }

    private fun getTotalMemoryMb(): Int {
        return try {
            val pageSize = sysconf(_SC_PAGESIZE)
            val pageCount = sysconf(_SC_PHYS_PAGES)
            ((pageSize * pageCount) / (1024 * 1024)).toInt()
        } catch (e: Exception) {
            0
        }
    }

    private fun getAvailableMemoryMb(): Int {
        return getTotalMemoryMb() / 2 // Rough estimate
    }

    private fun getStorageGb(): Int {
        return 64 // Default estimate
    }

    private fun hasNfcCapability(): Boolean {
        return true // iPhone 7 and later
    }

    private fun hasUwbCapability(): Boolean {
        return false // Would need model detection
    }

    private fun hasCellularCapability(): Boolean {
        return !device.model.contains("WiFi")
    }

    private fun hasBarometerCapability(): Boolean {
        return true // iPhone 6 and later
    }

    private fun hasTouchId(): Boolean {
        return true // Would need LocalAuthentication check
    }

    private fun hasFaceId(): Boolean {
        return false // Would need model detection
    }

    private fun currentTimeMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000).toLong()
    }
}
