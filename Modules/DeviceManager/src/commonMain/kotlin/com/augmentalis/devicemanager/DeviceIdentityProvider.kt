// Author: Manoj Jhawar
// Purpose: Cross-platform device identity provider for IPC and licensing

package com.augmentalis.devicemanager

/**
 * Provides device identity for IPC handshakes and license validation.
 *
 * This interface is specifically designed for integration with UniversalIPC
 * which uses deviceId in HandshakeMessage, PromotionMessage, and RoleChangeMessage.
 *
 * Also provides fingerprint for NODE_LOCKED license validation.
 */
interface DeviceIdentityProvider {
    /**
     * Get unique device identifier for IPC messages.
     *
     * This ID should be:
     * - Stable across app restarts
     * - Unique per device
     * - Not personally identifiable
     *
     * @return Device ID string suitable for IPC identification
     */
    fun getDeviceId(): String

    /**
     * Get fingerprint for license validation (NODE_LOCKED licenses).
     *
     * The fingerprint should be:
     * - Hardware-based for persistence
     * - Difficult to spoof
     * - Consistent across reinstalls
     *
     * @return Device fingerprint for licensing
     */
    fun getFingerprint(): DeviceFingerprint

    /**
     * Get short fingerprint for display purposes.
     *
     * @return First 8 characters of fingerprint
     */
    fun getFingerprintShort(): String = getFingerprint().shortValue()

    /**
     * Validate if current device matches a given fingerprint.
     *
     * @param fingerprint Fingerprint to validate against
     * @return true if fingerprints match
     */
    fun validateFingerprint(fingerprint: String): Boolean {
        return getFingerprint().value == fingerprint
    }
}

/**
 * Factory for creating platform-specific identity providers.
 *
 * Usage:
 * ```
 * val identity = DeviceIdentityFactory.create()
 * val deviceId = identity.getDeviceId()
 *
 * // For IPC handshake
 * val handshake = HandshakeMessage(
 *     protocolVersion = "2.0",
 *     appVersion = "1.0.0",
 *     deviceId = identity.getDeviceId()
 * )
 *
 * // For license validation
 * val isValid = identity.validateFingerprint(license.fingerprint)
 * ```
 */
expect object DeviceIdentityFactory {
    /**
     * Create a device identity provider for the current platform.
     * On Android, must call DeviceCapabilityFactory.initialize(context) first.
     */
    fun create(): DeviceIdentityProvider
}
