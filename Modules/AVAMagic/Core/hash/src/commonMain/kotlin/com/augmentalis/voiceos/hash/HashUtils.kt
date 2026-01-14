/**
 * HashUtils.kt - SHA-256 hashing utilities for VoiceOS (KMP Library)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-26
 * Extracted to KMP Library: 2025-11-16
 *
 * Kotlin Multiplatform compatible hash utilities for app and data identification.
 * Works on Android, iOS, Desktop, Web, and Native platforms.
 *
 * ## Hash Types
 *
 * 1. **App Hash**: SHA-256(packageName:versionCode)
 *    - Unique identifier for each app version
 *    - Used to detect app updates and avoid re-processing
 *
 * 2. **Generic Hash**: SHA-256(input)
 *    - General-purpose hashing for any string
 *    - Collision-resistant data identification
 *
 * ## Migration from MD5
 *
 * This utility replaced deprecated MD5-based hashing:
 * - Old: MD5 (32 chars, 2^64 collision resistance)
 * - New: SHA-256 (64 chars, 2^128 collision resistance)
 * - Improvement: 18 quintillion times stronger collision resistance
 *
 * ## Usage
 *
 * ```kotlin
 * // App-level hash
 * val appHash = HashUtils.calculateAppHash("com.example.app", 42)
 * // Returns: "a1b2c3d4...f9e8d7c6" (64-char SHA-256 hash)
 *
 * // Generic hash
 * val dataHash = HashUtils.calculateHash("some data to hash")
 *
 * // Validate hash format
 * val isValid = HashUtils.isValidHash(hash)
 * ```
 */
package com.augmentalis.voiceos.hash

/**
 * Hash Utilities
 *
 * Centralized SHA-256 hashing utilities for app and data identification.
 */
object HashUtils {
    /**
     * Calculate SHA-256 hash for app identification
     *
     * Generates a unique hash based on package name and version code.
     * This hash is used to identify app versions.
     *
     * When the app updates (version code changes), a new hash is generated,
     * triggering fresh processing to capture changes.
     *
     * @param packageName Package identifier (e.g., "com.example.app")
     * @param versionCode Version code (integer)
     * @return SHA-256 hash (64 characters, lowercase hexadecimal)
     */
    fun calculateAppHash(packageName: String, versionCode: Int): String {
        val input = "$packageName:$versionCode"
        return calculateHash(input)
    }

    /**
     * Calculate SHA-256 hash for any string
     *
     * Generic hashing utility for custom use cases.
     *
     * @param input String to hash
     * @return SHA-256 hash (64 characters, lowercase hexadecimal)
     */
    fun calculateHash(input: String): String {
        return sha256(input)
    }

    /**
     * Validate hash format
     *
     * Checks if a string is a valid SHA-256 hash (64 hexadecimal characters).
     *
     * @param hash String to validate
     * @return true if valid SHA-256 format, false otherwise
     */
    fun isValidHash(hash: String): Boolean {
        return hash.length == 64 && hash.all { it in '0'..'9' || it in 'a'..'f' }
    }

    /**
     * Calculate SHA-256 hash from input string
     *
     * Internal implementation of SHA-256 hashing.
     * Uses platform-specific SHA-256 implementation.
     *
     * @param input String to hash
     * @return SHA-256 hash as hexadecimal string (64 characters)
     */
    private fun sha256(input: String): String {
        // Note: Actual SHA-256 implementation is platform-specific
        // This is provided by expect/actual mechanism in KMP
        return sha256Impl(input)
    }
}

/**
 * Platform-specific SHA-256 implementation
 *
 * Each platform (Android, iOS, JVM) provides its own implementation.
 */
internal expect fun sha256Impl(input: String): String
