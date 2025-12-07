/**
 * Sha256Jvm.kt - JVM implementation of SHA-256
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-16
 */
package com.augmentalis.voiceos.hash

import java.security.MessageDigest

/**
 * JVM implementation of SHA-256 hashing
 *
 * Uses java.security.MessageDigest which is available on all JVM platforms.
 */
internal actual fun sha256Impl(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}
