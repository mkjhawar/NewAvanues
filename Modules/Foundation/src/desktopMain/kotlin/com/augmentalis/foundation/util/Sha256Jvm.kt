package com.augmentalis.foundation.util

import java.security.MessageDigest

internal actual fun sha256Impl(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(input.toByteArray())
    return hashBytes.joinToString("") { "%02x".format(it) }
}
