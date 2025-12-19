package com.augmentalis.ava.core.data.util

import java.security.MessageDigest

/**
 * Android implementation of HashHelper
 */
actual object HashHelper {
    actual fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
