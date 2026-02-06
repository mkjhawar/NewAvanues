package com.augmentalis.foundation.util

import java.security.MessageDigest

internal actual fun sha256Impl(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}
