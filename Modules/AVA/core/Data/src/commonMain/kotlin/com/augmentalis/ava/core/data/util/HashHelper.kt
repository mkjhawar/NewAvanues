package com.augmentalis.ava.core.data.util

/**
 * Cross-platform hash utilities
 *
 * Provides platform-agnostic MD5 hashing for deduplication.
 */
expect object HashHelper {
    /**
     * Generate MD5 hash of input string
     *
     * @param input String to hash
     * @return MD5 hash as hex string
     */
    fun md5(input: String): String
}
