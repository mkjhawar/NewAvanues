package com.augmentalis.magiccode.plugins.assets

import java.security.MessageDigest

/**
 * Android implementation of ChecksumCalculator using java.security.MessageDigest.
 */
actual class ChecksumCalculator {
    actual fun calculateMD5(data: ByteArray): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }

    actual fun calculateSHA256(data: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
