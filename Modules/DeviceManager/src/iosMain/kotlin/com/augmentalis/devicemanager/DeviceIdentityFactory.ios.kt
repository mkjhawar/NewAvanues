// Author: Manoj Jhawar
// Purpose: iOS actual implementation of DeviceIdentityFactory

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.augmentalis.devicemanager

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSUUID
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIDevice
import platform.posix._SC_NPROCESSORS_ONLN
import platform.posix.sysconf

/**
 * iOS actual implementation of DeviceIdentityFactory
 */
actual object DeviceIdentityFactory {
    private var provider: DeviceIdentityProvider? = null

    /**
     * Create iOS device identity provider
     */
    actual fun create(): DeviceIdentityProvider {
        provider?.let { return it }

        val newProvider = IosDeviceIdentityProvider()
        provider = newProvider
        return newProvider
    }
}

/**
 * iOS implementation of DeviceIdentityProvider
 */
internal class IosDeviceIdentityProvider : DeviceIdentityProvider {

    private val device = UIDevice.currentDevice

    /**
     * Get unique device identifier for IPC messages.
     *
     * Uses identifierForVendor which is stable per vendor but
     * resets if all apps from vendor are uninstalled.
     */
    override fun getDeviceId(): String {
        val vendorId = device.identifierForVendor?.UUIDString
        return vendorId ?: NSUUID().UUIDString
    }

    /**
     * Get fingerprint for license validation.
     *
     * Uses hardware and vendor identifiers for stability.
     */
    override fun getFingerprint(): DeviceFingerprint {
        val components = listOf(
            device.model,
            device.systemName,
            device.systemVersion,
            device.identifierForVendor?.UUIDString ?: "unknown",
            getProcessorCount().toString()
        )

        val fingerprint = generateFnv1aHash(components.joinToString(":"))

        return DeviceFingerprint(
            value = fingerprint,
            type = "hardware",
            components = listOf("model", "os", "version", "vendor_id", "cores"),
            timestamp = currentTimeMillis()
        )
    }

    // ===== Private Helpers =====

    private fun getProcessorCount(): Int {
        return sysconf(_SC_NPROCESSORS_ONLN).toInt()
    }

    private fun currentTimeMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000).toLong()
    }

    /**
     * Generate FNV-1a hash for fingerprint
     */
    private fun generateFnv1aHash(input: String): String {
        val FNV_OFFSET_BASIS = 2166136261L
        val FNV_PRIME = 16777619L

        var hash = FNV_OFFSET_BASIS
        for (byte in input.encodeToByteArray()) {
            hash = hash xor (byte.toLong() and 0xFF)
            hash = (hash * FNV_PRIME) and 0xFFFFFFFFL
        }

        return hash.toString(16).padStart(8, '0')
    }
}
