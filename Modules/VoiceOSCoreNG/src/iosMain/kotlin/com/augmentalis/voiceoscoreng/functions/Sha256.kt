/**
 * Sha256.kt - iOS implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * iOS SHA-256 implementation using hashCode for fingerprinting.
 * Full cryptographic implementation would use CommonCrypto CC_SHA256.
 */
package com.augmentalis.voiceoscoreng.functions

/**
 * iOS SHA-256 implementation.
 *
 * Note: This is a simplified implementation using hashCode for fingerprinting.
 * Full cryptographic implementation would require linking CommonCrypto.
 */
actual fun sha256(input: String): String {
    if (input.isEmpty()) return FingerprintUtils.EMPTY_HASH

    // Simplified implementation using hashCode
    // Full implementation would use CC_SHA256 from CommonCrypto
    val hash = input.hashCode().toLong() and 0xFFFFFFFFL
    val hash2 = (input.reversed().hashCode().toLong() and 0xFFFFFFFFL)
    val hash3 = (input.hashCode() xor input.length).toLong() and 0xFFFFFFFFL
    val hash4 = ((input.reversed().hashCode()) xor input.length).toLong() and 0xFFFFFFFFL

    return buildString {
        append(hash.toHexString(8))
        append(hash2.toHexString(8))
        append(hash3.toHexString(8))
        append(hash4.toHexString(8))
        append(hash.toHexString(8))
        append(hash2.toHexString(8))
        append(hash3.toHexString(8))
        append(hash4.toHexString(8))
    }
}

/**
 * Convert Long to hex string with specified length (zero-padded).
 */
private fun Long.toHexString(length: Int): String {
    val hex = this.toString(16)
    return if (hex.length >= length) {
        hex.takeLast(length)
    } else {
        "0".repeat(length - hex.length) + hex
    }
}
