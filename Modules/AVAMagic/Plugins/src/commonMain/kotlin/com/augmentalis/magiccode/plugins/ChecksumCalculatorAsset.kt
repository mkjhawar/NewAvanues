package com.augmentalis.avacode.plugins

/**
 * Calculates checksums for asset integrity verification.
 *
 * Supports MD5 and SHA-256 algorithms for file integrity checks (FR-040).
 */
expect class ChecksumCalculator() {
    /**
     * Calculate MD5 checksum of file contents.
     *
     * @param data File data as byte array
     * @return MD5 checksum as hex string
     */
    fun calculateMD5(data: ByteArray): String

    /**
     * Calculate SHA-256 checksum of file contents.
     *
     * @param data File data as byte array
     * @return SHA-256 checksum as hex string
     */
    fun calculateSHA256(data: ByteArray): String
}

/**
 * Checksum algorithm enum.
 */
enum class ChecksumAlgorithm {
    MD5,
    SHA256
}
