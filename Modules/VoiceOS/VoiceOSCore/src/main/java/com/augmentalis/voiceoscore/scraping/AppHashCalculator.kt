/**
 * AppHashCalculator.kt - MD5 hash calculator for app fingerprinting
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.scraping

import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest

/**
 * App Hash Calculator
 *
 * Generates MD5 hashes for app and element identification.
 *
 * Hash Types:
 * 1. App Hash: MD5(packageName + versionCode) - Unique per app version
 * 2. Element Hash: MD5(className + viewId + text + contentDesc) - Unique per UI element
 *
 * Use Cases:
 * - Detect when app has been updated (version change)
 * - Fast O(1) lookup of scraped elements by hash
 * - Avoid re-scraping unchanged app versions
 *
 * @deprecated Use AvidFingerprint from AvidCreator library instead.
 *   AppHashCalculator uses basic MD5 hashing without hierarchy path awareness,
 *   which can lead to hash collisions when multiple identical elements exist.
 *   AvidFingerprint provides version-scoped SHA-256 hashing with
 *   collision prevention via hierarchy paths.
 *   Removal planned for v3.0.0.
 */
@Deprecated(
    message = "Use AvidFingerprint from AvidCreator library for more robust element hashing with collision prevention",
    replaceWith = ReplaceWith(
        "AvidFingerprint.fromNode(node, packageName, appVersion).generateHash()",
        "com.augmentalis.uuidcreator.thirdparty.AvidFingerprint"
    ),
    level = DeprecationLevel.ERROR
)
object AppHashCalculator {

    /**
     * Calculate app hash from package name and version code
     *
     * This creates a unique fingerprint for each app version.
     * When the app updates, the version code changes, creating a new hash.
     *
     * @param packageName Android package name (e.g., "com.example.app")
     * @param versionCode Android version code (integer)
     * @return MD5 hash string (32 characters, hexadecimal)
     *
     * Example:
     * ```
     * val hash = calculateAppHash("com.example.app", 42)
     * // Returns: "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6"
     * ```
     */
    fun calculateAppHash(packageName: String, versionCode: Int): String {
        val input = "$packageName:$versionCode"
        return calculateMD5(input)
    }

    /**
     * Calculate element hash from accessibility node info
     *
     * Creates a unique identifier for UI elements based on their properties.
     * Used for fast lookup when executing voice commands.
     *
     * @param node AccessibilityNodeInfo to hash
     * @return MD5 hash string (32 characters, hexadecimal)
     *
     * Hash Components:
     * - className (e.g., "android.widget.Button")
     * - viewIdResourceName (e.g., "com.example:id/submit_button")
     * - text (visible text content)
     * - contentDescription (accessibility description)
     *
     * Example:
     * ```
     * val hash = calculateElementHash(buttonNode)
     * // Returns: "b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6q7"
     * ```
     */
    fun calculateElementHash(node: AccessibilityNodeInfo): String {
        val components = buildString {
            append(node.className?.toString() ?: "")
            append("|")
            append(node.viewIdResourceName?.toString() ?: "")
            append("|")
            append(node.text?.toString() ?: "")
            append("|")
            append(node.contentDescription?.toString() ?: "")
        }
        return calculateMD5(components)
    }

    /**
     * Calculate element hash from individual components
     *
     * Alternative to calculateElementHash(node) when you already have
     * the component strings extracted.
     *
     * @param className Android view class name
     * @param viewId View ID resource name (nullable)
     * @param text Visible text content (nullable)
     * @param contentDescription Accessibility description (nullable)
     * @return MD5 hash string
     */
    fun calculateElementHash(
        className: String,
        viewId: String?,
        text: String?,
        contentDescription: String?
    ): String {
        val components = buildString {
            append(className)
            append("|")
            append(viewId ?: "")
            append("|")
            append(text ?: "")
            append("|")
            append(contentDescription ?: "")
        }
        return calculateMD5(components)
    }

    /**
     * Check if two app hashes indicate the same app version
     *
     * @param hash1 First app hash
     * @param hash2 Second app hash
     * @return true if hashes match (same app version)
     */
    fun isSameAppVersion(hash1: String, hash2: String): Boolean {
        return hash1.equals(hash2, ignoreCase = true)
    }

    /**
     * Validate hash format
     *
     * MD5 hashes should be 32 hexadecimal characters
     *
     * @param hash Hash string to validate
     * @return true if hash is valid MD5 format
     */
    fun isValidHash(hash: String): Boolean {
        return hash.matches(Regex("^[a-fA-F0-9]{32}$"))
    }

    /**
     * Calculate MD5 hash from input string
     *
     * Internal method used by all hash calculation functions.
     *
     * @param input String to hash
     * @return MD5 hash as hexadecimal string (32 characters)
     */
    private fun calculateMD5(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(input.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            android.util.Log.e("AppHashCalculator", "Error calculating MD5", e)
            // Return a deterministic fallback hash on error
            "error_${input.hashCode().toString(16).padStart(32, '0')}"
        }
    }

    /**
     * Generate a unique hash for the current timestamp
     *
     * Useful for generating unique IDs when MD5 is not required.
     *
     * @return Hash based on current timestamp
     */
    fun generateTimestampHash(): String {
        val timestamp = System.currentTimeMillis().toString()
        return calculateMD5(timestamp)
    }

    /**
     * Combine multiple hashes into a single hash
     *
     * Useful for creating composite identifiers.
     *
     * @param hashes Variable number of hash strings to combine
     * @return MD5 hash of all input hashes concatenated
     */
    fun combineHashes(vararg hashes: String): String {
        val combined = hashes.joinToString("|")
        return calculateMD5(combined)
    }
}

/**
 * Extension function to calculate hash directly from AccessibilityNodeInfo
 *
 * @deprecated Use AvidFingerprint instead
 */
@Deprecated("Use AvidFingerprint.fromNode()", level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
fun AccessibilityNodeInfo.toHash(): String {
    return AppHashCalculator.calculateElementHash(this)
}

/**
 * Extension function to calculate MD5 hash of any string
 */
fun String.toMD5(): String {
    return try {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(this.toByteArray())
        digest.joinToString("") { "%02x".format(it) }
    } catch (e: Exception) {
        "error_${this.hashCode().toString(16).padStart(32, '0')}"
    }
}
