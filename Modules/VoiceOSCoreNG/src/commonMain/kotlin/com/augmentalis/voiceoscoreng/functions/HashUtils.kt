package com.augmentalis.voiceoscoreng.functions

/**
 * Utility functions for generating hashes used in VUID creation.
 *
 * Provides platform-agnostic hash generation for:
 * - Package name hashing (6 chars)
 * - Element identifier hashing (8 chars)
 * - Combined VUID hashing
 */
object HashUtils {

    private val HEX_CHARS = "0123456789abcdef"

    /**
     * Generate a 6-character hash from a package name.
     * Uses a simple but deterministic hash algorithm suitable for cross-platform.
     *
     * @param packageName The package name to hash (e.g., "com.instagram.android")
     * @return 6-character lowercase hex string
     */
    fun hashPackageName(packageName: String): String {
        if (packageName.isBlank()) return "000000"
        return generateHash(packageName, 6)
    }

    /**
     * Generate an 8-character hash from element properties.
     *
     * @param resourceId The resource ID of the element
     * @param text The text content of the element
     * @param bounds The bounds string (e.g., "10,20,100,50")
     * @return 8-character lowercase hex string
     */
    fun hashElementProperties(
        resourceId: String,
        text: String,
        bounds: String
    ): String {
        val combined = "$resourceId|$text|$bounds"
        return generateHash(combined, 8)
    }

    /**
     * Generate a hash of specified length from input string.
     * Uses a simple polynomial rolling hash for deterministic, cross-platform results.
     *
     * @param input The string to hash
     * @param length Desired hash length (must be even, max 16)
     * @return Lowercase hex string of specified length
     */
    fun generateHash(input: String, length: Int): String {
        require(length in 1..16) { "Hash length must be between 1 and 16" }

        if (input.isEmpty()) {
            return "0".repeat(length)
        }

        // Use polynomial rolling hash with large prime
        val prime: Long = 31
        val mod: Long = Long.MAX_VALUE // Large modulus for hash space

        var hash1: Long = 0
        var hash2: Long = 0
        var power: Long = 1

        for (char in input) {
            val charValue = char.code.toLong()
            hash1 = ((hash1 * prime) + charValue) and mod
            hash2 = ((hash2 * 37) + charValue) and mod
            power = (power * prime) and mod
        }

        // Combine hashes for more entropy
        val combined = hash1 xor (hash2 * 0x5DEECE66DL)

        // Convert to hex
        val sb = StringBuilder()
        var value = combined
        repeat(length) {
            val nibble = (value and 0xF).toInt()
            sb.append(HEX_CHARS[nibble])
            value = value ushr 4
        }

        return sb.toString()
    }

    /**
     * Generate a random-looking hash for testing/fallback purposes.
     * Based on a seed for reproducibility.
     *
     * @param seed The seed value
     * @param length Desired hash length
     * @return Lowercase hex string
     */
    fun generateSeededHash(seed: Long, length: Int): String {
        require(length in 1..16) { "Hash length must be between 1 and 16" }

        // Linear congruential generator (same as java.util.Random)
        var state = seed
        val sb = StringBuilder()

        repeat(length) {
            state = (state * 0x5DEECE66DL + 0xBL) and 0xFFFFFFFFFFFFL
            val nibble = ((state ushr 17) and 0xF).toInt()
            sb.append(HEX_CHARS[nibble])
        }

        return sb.toString()
    }

    /**
     * Validate that a string is a valid hex hash of expected length.
     *
     * @param hash The hash to validate
     * @param expectedLength Expected length (0 for any length)
     * @return true if valid hex hash
     */
    fun isValidHash(hash: String, expectedLength: Int = 0): Boolean {
        if (hash.isEmpty()) return false
        if (expectedLength > 0 && hash.length != expectedLength) return false
        return hash.all { it in '0'..'9' || it in 'a'..'f' }
    }

    /**
     * Normalize a hash to lowercase.
     *
     * @param hash The hash to normalize
     * @return Lowercase hash
     */
    fun normalizeHash(hash: String): String = hash.lowercase()
}
