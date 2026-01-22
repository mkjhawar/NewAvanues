/**
 * ElementHasher.kt - Calculate unique hashes for UI elements
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
 * Element Hasher
 *
 * Generates unique hashes for UI elements based on their properties.
 * The hash is used to uniquely identify elements across scraping sessions.
 *
 * ## Hash Composition
 *
 * The hash is calculated from:
 * 1. className (Android widget class)
 * 2. viewIdResourceName (resource ID if available)
 * 3. text (visible text if available)
 * 4. contentDescription (accessibility description if available)
 *
 * This combination provides a stable identifier that survives app restarts
 * but changes when the element's properties change.
 *
 * ## Usage
 *
 * ```kotlin
 * val hash = ElementHasher.calculateHash(accessibilityNode)
 * // Result: "a1b2c3d4e5f6..." (MD5 hex string)
 * ```
 *
 * @deprecated Use AvidFingerprint from AvidCreator library instead.
 *   ElementHasher lacks hierarchy path awareness and version scoping, which can
 *   lead to hash collisions and cross-version conflicts. AvidFingerprint
 *   provides more robust hashing with collision prevention.
 *   Removal planned for v3.0.0.
 */
@Deprecated(
    message = "Use AvidFingerprint from AvidCreator library for more robust element hashing",
    replaceWith = ReplaceWith(
        "AvidFingerprint.fromNode(node, packageName, appVersion).generateHash()",
        "com.augmentalis.avidcreator.thirdparty.AvidFingerprint"
    ),
    level = DeprecationLevel.ERROR
)
object ElementHasher {

    private const val HASH_ALGORITHM = "MD5" // MD5 for speed, SHA-256 for security
    private const val SEPARATOR = "|"

    /**
     * Calculate unique hash for accessibility node
     *
     * @param node AccessibilityNodeInfo
     * @return MD5 hash as hex string
     */
    fun calculateHash(node: AccessibilityNodeInfo): String {
        val fingerprint = buildFingerprint(node)
        return hashString(fingerprint)
    }

    /**
     * Calculate hash from element properties
     *
     * @param className Element class name
     * @param viewIdResourceName Resource ID (optional)
     * @param text Text content (optional)
     * @param contentDescription Content description (optional)
     * @return MD5 hash as hex string
     */
    fun calculateHash(
        className: String,
        viewIdResourceName: String? = null,
        text: String? = null,
        contentDescription: String? = null
    ): String {
        val fingerprint = buildFingerprint(className, viewIdResourceName, text, contentDescription)
        return hashString(fingerprint)
    }

    /**
     * Build fingerprint string from node properties
     *
     * Creates a stable string representation combining all identifying properties.
     *
     * @param node AccessibilityNodeInfo
     * @return Fingerprint string
     */
    private fun buildFingerprint(node: AccessibilityNodeInfo): String {
        return buildFingerprint(
            className = node.className?.toString() ?: "",
            viewIdResourceName = node.viewIdResourceName?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString()
        )
    }

    /**
     * Build fingerprint string from properties
     *
     * @param className Element class name
     * @param viewIdResourceName Resource ID (optional)
     * @param text Text content (optional)
     * @param contentDescription Content description (optional)
     * @return Fingerprint string
     */
    private fun buildFingerprint(
        className: String,
        viewIdResourceName: String?,
        text: String?,
        contentDescription: String?
    ): String {
        return buildString {
            // Always include className
            append(className.trim())
            append(SEPARATOR)

            // Include viewIdResourceName if available (most stable identifier)
            if (!viewIdResourceName.isNullOrBlank()) {
                append(viewIdResourceName.trim())
            }
            append(SEPARATOR)

            // Include text if available
            if (!text.isNullOrBlank()) {
                append(text.trim())
            }
            append(SEPARATOR)

            // Include contentDescription if available
            if (!contentDescription.isNullOrBlank()) {
                append(contentDescription.trim())
            }
        }
    }

    /**
     * Hash string using MD5
     *
     * @param input String to hash
     * @return MD5 hash as hex string
     */
    private fun hashString(input: String): String {
        return try {
            val digest = MessageDigest.getInstance(HASH_ALGORITHM)
            val hashBytes = digest.digest(input.toByteArray())
            hashBytes.toHexString()
        } catch (e: Exception) {
            // Fallback: use hashCode if MD5 fails
            input.hashCode().toString()
        }
    }

    /**
     * Convert byte array to hex string
     */
    private fun ByteArray.toHexString(): String {
        return joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate SHA-256 hash (more secure but slower)
     *
     * Use this for security-sensitive applications.
     *
     * @param node AccessibilityNodeInfo
     * @return SHA-256 hash as hex string
     */
    fun calculateSecureHash(node: AccessibilityNodeInfo): String {
        val fingerprint = buildFingerprint(node)
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(fingerprint.toByteArray())
            hashBytes.toHexString()
        } catch (e: Exception) {
            hashString(fingerprint) // Fallback to MD5
        }
    }

    /**
     * Validate hash format
     *
     * @param hash Hash string to validate
     * @return true if hash is valid MD5 format
     */
    fun isValidHash(hash: String): Boolean {
        // MD5 hash is 32 hex characters
        return hash.matches(Regex("^[a-f0-9]{32}$"))
    }

    /**
     * Calculate hash with position weighting
     *
     * Includes element position in the hash for stricter matching.
     * Use when elements have identical properties but different positions.
     *
     * @param node AccessibilityNodeInfo
     * @param includePosition Include position in hash
     * @return Hash string
     */
    fun calculateHashWithPosition(
        node: AccessibilityNodeInfo,
        includePosition: Boolean = true
    ): String {
        val fingerprint = if (includePosition) {
            val bounds = android.graphics.Rect()
            node.getBoundsInScreen(bounds)
            buildFingerprint(node) + SEPARATOR +
                "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}"
        } else {
            buildFingerprint(node)
        }

        return hashString(fingerprint)
    }

    /**
     * Compare two hashes for equality
     *
     * @param hash1 First hash
     * @param hash2 Second hash
     * @return true if hashes match
     */
    fun hashesMatch(hash1: String, hash2: String): Boolean {
        return hash1.equals(hash2, ignoreCase = true)
    }

    /**
     * Calculate similarity score between two hashes
     *
     * Uses Hamming distance to measure similarity.
     * Useful for finding "close" matches when exact hash fails.
     *
     * @param hash1 First hash
     * @param hash2 Second hash
     * @return Similarity score (0.0 to 1.0, where 1.0 is identical)
     */
    fun calculateSimilarity(hash1: String, hash2: String): Float {
        if (hash1.length != hash2.length) return 0f

        val hammingDistance = hash1.zip(hash2).count { (a, b) -> a != b }
        val maxDistance = hash1.length

        return 1f - (hammingDistance.toFloat() / maxDistance.toFloat())
    }
}
