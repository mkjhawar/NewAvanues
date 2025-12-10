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

package com.augmentalis.voiceoscore.learnapp.fingerprinting

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

    /**
     * Calculate popup/dialog fingerprint (stable across content changes)
     *
     * FIX (2025-11-24): Generate stable hashes for popup windows
     * Issue: Popups with time-dependent content (e.g., time pickers) get different hashes
     * Solution: Hash based on STRUCTURE (buttons, layout) not CONTENT (time value)
     *
     * This method generates hashes based on:
     * - Dialog type (picker, alert, etc.)
     * - Button labels (OK, Cancel, etc.)
     * - Layout structure (view hierarchy)
     * - NOT time-dependent content
     *
     * @param rootNode Root node of popup
     * @param popupType Type of popup detected
     * @return SHA-256 hash based on structure
     */
    fun calculatePopupFingerprint(rootNode: AccessibilityNodeInfo?, popupType: String): String {
        if (rootNode == null) {
            return EMPTY_HASH
        }

        val signatureBuilder = StringBuilder()

        // Start with popup type for differentiation
        signatureBuilder.append("POPUP:$popupType\n")

        // Traverse tree and extract STRUCTURAL elements only
        traverseNodeTree(rootNode) { node ->
            val className = node.className?.toString() ?: ""
            val resourceId = node.viewIdResourceName ?: ""
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""

            // For popups, include STRUCTURAL elements only:
            // 1. All view IDs (structural)
            // 2. All class names (structural)
            // 3. Button/action labels (OK, Cancel, Set, etc.) - NOT time values
            // 4. Content descriptions for accessibility

            // Always include class and resource ID (structural)
            signatureBuilder.append(className)
            signatureBuilder.append("|")
            signatureBuilder.append(resourceId)
            signatureBuilder.append("|")

            // For text: Include if it's a button/action label, exclude if it's dynamic content
            val isActionLabel = isActionLabel(text, className)
            val includedText = if (isActionLabel) text else ""

            signatureBuilder.append(includedText)
            signatureBuilder.append("|")
            signatureBuilder.append(contentDesc)
            signatureBuilder.append("\n")
        }

        val signature = signatureBuilder.toString()
        android.util.Log.d("ScreenFingerprinter",
            "Generated popup fingerprint for type=$popupType (signature length=${signature.length})")

        return calculateSHA256(signature)
    }

    /**
     * Check if text is an action label (button text) vs dynamic content
     *
     * Action labels are stable (OK, Cancel, Set, etc.)
     * Dynamic content changes (time values, dates, etc.)
     *
     * @param text Text to check
     * @param className Class name of the node
     * @return true if this is an action label
     */
    private fun isActionLabel(text: String, className: String): Boolean {
        if (text.isBlank()) return false

        // If it's a button, text is likely an action label
        if (className.contains("Button", ignoreCase = true)) {
            return true
        }

        // Check for common action labels
        val actionLabels = listOf(
            "ok", "cancel", "set", "done", "yes", "no", "close", "dismiss",
            "save", "delete", "confirm", "apply", "reset", "clear", "submit",
            "next", "back", "skip", "retry", "continue"
        )

        val lowerText = text.lowercase()
        if (actionLabels.any { lowerText == it }) {
            return true
        }

        // Exclude if it matches dynamic patterns (time, date, numbers)
        for (pattern in DYNAMIC_PATTERNS) {
            if (pattern.containsMatchIn(text)) {
                return false
            }
        }

        // For short text (1-15 chars), likely a label if not dynamic
        if (text.length in 1..15) {
            return true
        }

        // Long text is likely dynamic content
        return false
    }

<<<<<<< HEAD
    /**
     * Calculate structural fingerprint for dynamic content screens.
     *
     * FIX (2025-12-07): Generate stable hashes for screens with dynamic content
     * Issue: Screens like Teams channels, chat lists, social feeds have constantly
     * changing content (new messages, timestamps) causing different hashes on each visit.
     * Solution: Hash based on STRUCTURE (view types, resource IDs, layout) not CONTENT.
     *
     * This is useful for:
     * - Chat/messaging apps (new messages appear)
     * - Social feeds (likes/comments update)
     * - Email lists (new emails arrive)
     * - Notification lists (badge counts change)
     *
     * @param rootNode Root node of screen
     * @return SHA-256 hash based on structure only
     */
    fun calculateStructuralFingerprint(rootNode: AccessibilityNodeInfo?): String {
        if (rootNode == null) {
            return EMPTY_HASH
        }

        val signatureBuilder = StringBuilder()
        signatureBuilder.append("STRUCTURAL:\n")

        // Count element types for structural signature
        val elementTypeCounts = mutableMapOf<String, Int>()
        val resourceIds = mutableListOf<String>()

        traverseNodeTree(rootNode) { node ->
            val className = node.className?.toString() ?: "Unknown"
            val resourceId = node.viewIdResourceName ?: ""

            // Count by class name (structural)
            elementTypeCounts[className] = elementTypeCounts.getOrDefault(className, 0) + 1

            // Collect unique resource IDs (structural identifiers)
            if (resourceId.isNotEmpty()) {
                resourceIds.add(resourceId)
            }
        }

        // Build signature from element type counts (order-independent)
        elementTypeCounts.entries.sortedBy { it.key }.forEach { (className, count) ->
            signatureBuilder.append("$className:$count\n")
        }

        // Add unique resource IDs (sorted for consistency)
        signatureBuilder.append("IDS:")
        resourceIds.distinct().sorted().forEach { id ->
            signatureBuilder.append("$id|")
        }

        return calculateSHA256(signatureBuilder.toString())
    }

=======
>>>>>>> AVA-Development
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
