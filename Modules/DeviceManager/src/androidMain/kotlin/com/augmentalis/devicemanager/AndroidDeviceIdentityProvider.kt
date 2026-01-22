// Author: Manoj Jhawar
// Purpose: Android implementation of DeviceIdentityProvider

package com.augmentalis.devicemanager

import android.os.Build
import java.util.UUID

/**
 * Android implementation of DeviceIdentityProvider.
 *
 * Provides device identity for IPC handshakes and license validation.
 * Uses hardware-based fingerprinting for stable, unique identification.
 */
class AndroidDeviceIdentityProvider(
    private val capabilityProvider: DeviceCapabilityProvider
) : DeviceIdentityProvider {

    companion object {
        private const val TAG = "AndroidDeviceIdentity"
    }

    // Cached device ID for stability
    @Volatile
    private var cachedDeviceId: String? = null

    /**
     * Get unique device identifier for IPC messages.
     *
     * On Android, we use a combination of hardware identifiers that:
     * - Remain stable across app restarts
     * - Are unique per device (within reasonable confidence)
     * - Don't require special permissions
     */
    override fun getDeviceId(): String {
        cachedDeviceId?.let { return it }

        // Generate a pseudo-unique device ID from hardware properties
        val components = listOf(
            Build.BOARD,
            Build.BRAND,
            Build.DEVICE,
            Build.HARDWARE,
            Build.MANUFACTURER,
            Build.MODEL,
            Build.PRODUCT,
            Build.VERSION.SDK_INT.toString()
        )

        // Create UUID from hash of components
        val hash = components.joinToString(":").hashCode()
        val deviceId = UUID(hash.toLong(), Build.FINGERPRINT.hashCode().toLong()).toString()

        cachedDeviceId = deviceId
        return deviceId
    }

    /**
     * Get fingerprint for license validation (NODE_LOCKED licenses).
     *
     * Uses the capability provider's fingerprint which is based on
     * FNV-1a hash of hardware components.
     */
    override fun getFingerprint(): DeviceFingerprint {
        return capabilityProvider.getDeviceFingerprint()
    }
}
