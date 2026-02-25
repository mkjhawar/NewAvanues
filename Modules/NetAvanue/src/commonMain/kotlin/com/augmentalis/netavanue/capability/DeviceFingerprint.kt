package com.augmentalis.netavanue.capability

/**
 * Stable device identifier that survives app reinstalls.
 *
 * Each platform provides an `actual` implementation:
 * - Android: Settings.Secure.ANDROID_ID + Android Keystore Ed25519 key pair
 * - iOS: identifierForVendor + Keychain-stored key pair
 * - Desktop: MAC address hash + file-stored key pair
 *
 * The fingerprint is a SHA-256 hash of platform-specific stable identifiers.
 * The Ed25519 key pair is used to sign capability advertisements and session
 * rejoin requests, preventing device impersonation.
 */
expect class DeviceFingerprint() {
    /** The stable SHA-256 fingerprint string (hex, 64 chars) */
    val fingerprint: String

    /** Ed25519 public key (base64-encoded) for signature verification */
    val publicKey: String

    /** Sign data with the Ed25519 private key. Returns base64-encoded signature. */
    fun sign(data: ByteArray): String
}
