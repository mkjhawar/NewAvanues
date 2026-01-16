package com.augmentalis.voiceoscoreng.functions

import java.security.MessageDigest

/**
 * Utility object for hash generation.
 */
object HashUtils {
    /**
     * Generate a hash from input string.
     *
     * @param input The string to hash
     * @param length The desired hash length (default 16)
     * @return The generated hash string
     */
    fun generateHash(input: String, length: Int = 16): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }.take(length)
    }
}
