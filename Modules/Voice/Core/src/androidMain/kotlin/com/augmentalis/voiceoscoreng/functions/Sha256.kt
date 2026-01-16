/**
 * Sha256.kt - Android implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 */
package com.augmentalis.voiceoscoreng.functions

import java.security.MessageDigest

/**
 * Android SHA-256 implementation using java.security.
 */
actual fun sha256(input: String): String {
    if (input.isEmpty()) return FingerprintUtils.EMPTY_HASH

    return try {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        hashBytes.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        FingerprintUtils.EMPTY_HASH
    }
}
