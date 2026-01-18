// Author: Manoj Jhawar
// Purpose: Cross-platform device capability provider interface

package com.augmentalis.devicemanager

/**
 * Platform-agnostic device capability detection interface.
 *
 * Provides unified access to device information, hardware profiles,
 * and capability detection across Android, iOS, and Desktop platforms.
 */
interface DeviceCapabilityProvider {
    /**
     * Get basic device information
     */
    fun getKmpDeviceInfo(): KmpDeviceInfo

    /**
     * Get hardware profile (CPU, RAM, GPU, storage)
     */
    fun getHardwareProfile(): HardwareProfile

    /**
     * Get network capabilities (Bluetooth, WiFi, NFC, etc.)
     */
    fun getNetworkCapabilities(): NetworkCapabilities

    /**
     * Get sensor capabilities (accelerometer, gyroscope, etc.)
     */
    fun getSensorCapabilities(): SensorCapabilities

    /**
     * Get display capabilities (resolution, HDR, refresh rate)
     */
    fun getDisplayCapabilities(): DisplayCapabilities

    /**
     * Get biometric capabilities (fingerprint, face, iris)
     * Returns null if biometrics are not available
     */
    fun getBiometricCapabilities(): BiometricCapabilities?

    /**
     * Get device fingerprint for identification
     */
    fun getDeviceFingerprint(): DeviceFingerprint

    /**
     * Get performance classification
     */
    fun getPerformanceClass(): PerformanceClass

    /**
     * Get all capabilities in a single call
     */
    fun getAllCapabilities(): DeviceCapabilities {
        return DeviceCapabilities(
            deviceInfo = getKmpDeviceInfo(),
            hardware = getHardwareProfile(),
            network = getNetworkCapabilities(),
            sensors = getSensorCapabilities(),
            display = getDisplayCapabilities(),
            biometric = getBiometricCapabilities(),
            fingerprint = getDeviceFingerprint(),
            performanceClass = getPerformanceClass()
        )
    }

    /**
     * Refresh cached capabilities
     */
    suspend fun refreshCapabilities(): DeviceCapabilities
}

/**
 * Factory for creating platform-specific capability providers.
 *
 * Usage:
 * ```
 * val provider = DeviceCapabilityFactory.create()
 * val capabilities = provider.getAllCapabilities()
 * ```
 */
expect object DeviceCapabilityFactory {
    /**
     * Create a device capability provider for the current platform.
     * On Android, must call initialize(context) first.
     */
    fun create(): DeviceCapabilityProvider
}
