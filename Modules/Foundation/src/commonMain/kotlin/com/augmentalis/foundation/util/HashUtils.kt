/**
 * HashUtils.kt - SHA-256 hashing utilities (KMP)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-26
 * Moved to Foundation: 2026-02-06
 *
 * Kotlin Multiplatform compatible hash utilities for app and data identification.
 * Works on Android, iOS, Desktop, Web, and Native platforms.
 */
package com.augmentalis.foundation.util

/**
 * Centralized SHA-256 hashing utilities for app and data identification.
 */
object HashUtils {
    /**
     * Calculate SHA-256 hash for app identification.
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
     * Calculate SHA-256 hash for any string.
     *
     * @param input String to hash
     * @return SHA-256 hash (64 characters, lowercase hexadecimal)
     */
    fun calculateHash(input: String): String = sha256(input)

    /**
     * Validate hash format.
     *
     * @param hash String to validate
     * @return true if valid SHA-256 format, false otherwise
     */
    fun isValidHash(hash: String): Boolean {
        return hash.length == 64 && hash.all { it in '0'..'9' || it in 'a'..'f' }
    }

    /**
     * Calculate SHA-256 hash of input string.
     *
     * @param input String to hash
     * @return SHA-256 hash as hexadecimal string (64 characters)
     */
    fun sha256(input: String): String = sha256Impl(input)
}

/**
 * Platform-specific SHA-256 implementation.
 *
 * Each platform (Android, iOS, JVM) provides its own implementation.
 */
internal expect fun sha256Impl(input: String): String
