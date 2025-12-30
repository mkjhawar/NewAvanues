/**
 * ScreenFingerprinter.kt - Calculates SHA-256 fingerprints for screens
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/fingerprinting/ScreenFingerprinter.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Calculates deterministic SHA-256 hashes for UI screen states
 */

package com.augmentalis.learnapp.fingerprinting

import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest

/**
 * Screen Fingerprinter
 *
 * Calculates SHA-256 hash fingerprints for accessibility node trees.
 * Used to detect when navigation reaches the same screen.
 *
 * ## Algorithm
 *
 * 1. Traverse accessibility node tree (DFS)
 * 2. For each node, extract:
 *    - className
 *    - viewIdResourceName
 *    - text (filtered for dynamic content)
 *    - contentDescription
 * 3. Build signature string from all nodes
 * 4. Calculate SHA-256 hash
 *
 * ## Usage Example
 *
 * ```kotlin
 * val fingerprinter = ScreenFingerprinter()
 *
 * val rootNode = getRootInActiveWindow()
 * val hash = fingerprinter.calculateFingerprint(rootNode)
 *
 * // Hash will be same for identical screens
 * ```
 *
 * ## Dynamic Content Filtering
 *
 * Filters out dynamic content that changes frequently:
 * - Timestamps (e.g., "2 minutes ago")
 * - Time displays (e.g., "14:35")
 * - Ads and sponsored content
 * - Live counters
 *
 * This ensures the same screen produces the same hash even with
 * minor dynamic changes.
 *
 * @since 1.0.0
 */
class ScreenFingerprinter {

    /**
     * Calculate fingerprint for screen
     *
     * Main entry point. Calculates SHA-256 hash of node tree.
     *
     * @param rootNode Root accessibility node
     * @return SHA-256 hash (64 hex characters)
     */
    fun calculateFingerprint(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) {
            return EMPTY_HASH
        }

        val signature = buildSignature(rootNode)
        return calculateSHA256(signature)
    }

    /**
     * Build signature string from node tree
     *
     * Traverses tree and builds deterministic signature.
     *
     * @param rootNode Root node
     * @return Signature string
     */
    private fun buildSignature(rootNode: AccessibilityNodeInfo): String {
        val signatureBuilder = StringBuilder()

        traverseNodeTree(rootNode) { node ->
            // Extract node properties
            val className = node.className?.toString() ?: ""
            val resourceId = node.viewIdResourceName ?: ""
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""

            // Filter dynamic content
            val filteredText = filterDynamicContent(text)
            val filteredDesc = filterDynamicContent(contentDesc)

            // Append to signature (pipe-separated)
            signatureBuilder.append(className)
            signatureBuilder.append("|")
            signatureBuilder.append(resourceId)
            signatureBuilder.append("|")
            signatureBuilder.append(filteredText)
            signatureBuilder.append("|")
            signatureBuilder.append(filteredDesc)
            signatureBuilder.append("\n")
        }

        return signatureBuilder.toString()
    }

    /**
     * Traverse node tree (DFS)
     *
     * Recursively traverses all nodes in tree.
     *
     * @param node Current node
     * @param visitor Visitor function called for each node
     */
    private fun traverseNodeTree(
        node: AccessibilityNodeInfo,
        visitor: (AccessibilityNodeInfo) -> Unit
    ) {
        // Visit current node
        visitor(node)

        // Recursively visit children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                try {
                    traverseNodeTree(child, visitor)
                } finally {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        }
    }

    /**
     * Filter dynamic content from text
     *
     * Removes content that changes frequently (timestamps, ads, etc.)
     *
     * @param text Text to filter
     * @return Filtered text (or empty if all dynamic)
     */
    private fun filterDynamicContent(text: String): String {
        if (text.isBlank()) return ""

        // Check if text matches dynamic patterns
        for (pattern in DYNAMIC_PATTERNS) {
            if (pattern.containsMatchIn(text)) {
                return ""  // Exclude dynamic content
            }
        }

        return text
    }

    /**
     * Calculate SHA-256 hash
     *
     * @param input Input string
     * @return SHA-256 hash (64 hex characters)
     */
    private fun calculateSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate lightweight fingerprint (first 16 chars of hash)
     *
     * Faster comparison for deduplication.
     *
     * @param rootNode Root node
     * @return Short hash (16 hex characters)
     */
    fun calculateShortFingerprint(rootNode: AccessibilityNodeInfo?): String {
        val fullHash = calculateFingerprint(rootNode)
        return fullHash.take(16)
    }

    companion object {
        /**
         * Empty hash (for null roots)
         */
        private const val EMPTY_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

        /**
         * Dynamic content patterns (regex)
         *
         * Text matching these patterns is excluded from fingerprint.
         */
        private val DYNAMIC_PATTERNS = listOf(
            // Time patterns
            Regex("\\d{1,2}:\\d{2}"),  // HH:MM
            Regex("\\d{1,2}:\\d{2}:\\d{2}"),  // HH:MM:SS
            Regex("\\d+ (second|minute|hour|day|week|month|year)s? ago", RegexOption.IGNORE_CASE),
            Regex("(just now|a moment ago)", RegexOption.IGNORE_CASE),

            // Date patterns
            Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"),  // MM/DD/YYYY
            Regex("\\d{4}-\\d{2}-\\d{2}"),  // YYYY-MM-DD

            // Ad/sponsored patterns
            Regex("(ad|sponsored|promoted)", RegexOption.IGNORE_CASE),

            // Live counters
            Regex("\\d+[kKmMbB]? (views|likes|followers|comments)", RegexOption.IGNORE_CASE),

            // Loading states
            Regex("loading|refreshing", RegexOption.IGNORE_CASE)
        )
    }
}
