/**
 * HashUtils.kt - SHA-256 hashing utilities for VOS4 scraping
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-26
 */
package com.augmentalis.voiceoscore.scraping

import java.security.MessageDigest

/**
 * Hash Utilities
 *
 * Centralized SHA-256 hashing utilities for app and element identification.
 * Replaces deprecated MD5-based AppHashCalculator for better collision resistance.
 *
 * ## Hash Types
 *
 * 1. **App Hash**: SHA-256(packageName:versionCode)
 *    - Unique identifier for each app version
 *    - Used to detect app updates and avoid re-scraping
 *
 * 2. **Element Hash**: Delegated to AccessibilityFingerprint
 *    - SHA-256 with hierarchy path awareness
 *    - Collision-resistant element identification
 *
 * ## Migration from MD5
 *
 * This utility was created during migration from MD5 to SHA-256:
 * - Old: AppHashCalculator.calculateAppHash() → MD5 (32 chars)
 * - New: HashUtils.calculateAppHash() → SHA-256 (64 chars)
 * - Improvement: 2^64 → 2^128 collision resistance (18 quintillion times stronger)
 *
 * ## Usage
 *
 * ```kotlin
 * // App-level hash
 * val appHash = HashUtils.calculateAppHash("com.example.app", 42)
 *
 * // Element-level hash (use AccessibilityFingerprint instead)
 * val elementHash = AccessibilityFingerprint.fromNode(node, packageName, appVersion).generateHash()
 * ```
 */
object HashUtils {

    /**
     * Calculate SHA-256 hash for app identification
     *
     * Generates a unique hash based on package name and version code.
     * This hash is used to identify app versions in the scraping database.
     *
     * When the app updates (version code changes), a new hash is generated,
     * triggering a fresh scrape to capture UI changes.
     *
     * @param packageName Android package name (e.g., "com.example.app")
     * @param versionCode Android version code (integer)
     * @return SHA-256 hash (64 characters, lowercase hexadecimal)
     *
     * @see com.augmentalis.uuidcreator.thirdparty.AccessibilityFingerprint For element-level hashing
     */
    fun calculateAppHash(packageName: String, versionCode: Int): String {
        val input = "$packageName:$versionCode"
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate SHA-256 hash for any string
     *
     * Generic hashing utility for custom use cases.
     * Most code should use calculateAppHash() or AccessibilityFingerprint instead.
     *
     * @param input String to hash
     * @return SHA-256 hash (64 characters, lowercase hexadecimal)
     */
    fun calculateHash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
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
}
