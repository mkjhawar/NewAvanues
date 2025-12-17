/**
 * ScreenFingerprinter.kt - Calculates SHA-256 fingerprints for screens
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Calculates deterministic SHA-256 hashes for UI screen states
 */

package com.augmentalis.voiceoscore.learnapp.fingerprinting

import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest

/**
 * Screen Fingerprinter
 *
 * Calculates SHA-256 hash fingerprints for accessibility node trees.
 * Used to detect when navigation reaches the same screen.
 */
class ScreenFingerprinter {

    /**
     * Calculate fingerprint for screen
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
     */
    private fun buildSignature(rootNode: AccessibilityNodeInfo): String {
        val signatureBuilder = StringBuilder()

        traverseNodeTree(rootNode) { node ->
            val className = node.className?.toString() ?: ""
            val resourceId = node.viewIdResourceName ?: ""
            val text = node.text?.toString() ?: ""
            val contentDesc = node.contentDescription?.toString() ?: ""

            val filteredText = filterDynamicContent(text)
            val filteredDesc = filterDynamicContent(contentDesc)

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
     */
    private fun traverseNodeTree(
        node: AccessibilityNodeInfo,
        visitor: (AccessibilityNodeInfo) -> Unit
    ) {
        visitor(node)

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                try {
                    traverseNodeTree(child, visitor)
                } finally {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        }
    }

    /**
     * Filter dynamic content from text
     */
    private fun filterDynamicContent(text: String): String {
        if (text.isBlank()) return ""

        for (pattern in DYNAMIC_PATTERNS) {
            if (pattern.containsMatchIn(text)) {
                return ""
            }
        }

        return text
    }

    /**
     * Calculate SHA-256 hash
     */
    private fun calculateSHA256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Calculate lightweight fingerprint (first 16 chars of hash)
     */
    fun calculateShortFingerprint(rootNode: AccessibilityNodeInfo?): String {
        val fullHash = calculateFingerprint(rootNode)
        return fullHash.take(16)
    }

    companion object {
        private const val EMPTY_HASH = "0000000000000000000000000000000000000000000000000000000000000000"

        private val DYNAMIC_PATTERNS = listOf(
            Regex("\\d{1,2}:\\d{2}"),
            Regex("\\d{1,2}:\\d{2}:\\d{2}"),
            Regex("\\d+ (second|minute|hour|day|week|month|year)s? ago", RegexOption.IGNORE_CASE),
            Regex("(just now|a moment ago)", RegexOption.IGNORE_CASE),
            Regex("\\d{1,2}/\\d{1,2}/\\d{2,4}"),
            Regex("\\d{4}-\\d{2}-\\d{2}"),
            Regex("(ad|sponsored|promoted)", RegexOption.IGNORE_CASE),
            Regex("\\d+[kKmMbB]? (views|likes|followers|comments)", RegexOption.IGNORE_CASE),
            Regex("loading|refreshing", RegexOption.IGNORE_CASE)
        )
    }
}
