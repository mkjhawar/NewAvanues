/**
 * ScreenHashAssembler.kt - KMP-portable screen hash assembly and normalization
 *
 * Extracted from app-layer ScreenCacheManager.kt to enable cross-platform reuse.
 * Contains: text normalization (dynamic content masking), hybrid hash assembly
 * algorithm, and ScreenInfo factory.
 *
 * The platform layer collects raw element signatures from the accessibility tree
 * (Android: AccessibilityNodeInfo, iOS: UIAccessibility), then passes them here
 * for deterministic hash computation. This ensures both platforms produce identical
 * hashes for structurally equivalent screens.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore

import com.augmentalis.foundation.util.HashUtils
import kotlinx.datetime.Clock

/**
 * Assembles screen hashes from pre-collected element signatures.
 *
 * Hash formula: SHA-256(dimensionKey | sorted(signatures) | contentDigest) → first 16 chars
 *
 * The content digest captures text from scrollable containers so the hash changes
 * when new content scrolls into view, while the structural signatures capture the
 * layout skeleton (class names, resource IDs, depth, child count, flags).
 */
object ScreenHashAssembler {

    // Regex patterns for normalizing dynamic text content in hashes.
    // These mask volatile text (timestamps, counters) so hash stability improves.
    private val timePattern = Regex("\\d+:\\d+\\s*(am|pm)?", RegexOption.IGNORE_CASE)
    private val relativeTimePattern = Regex("\\d+\\s*(min|hour|day|week)s?\\s*ago", RegexOption.IGNORE_CASE)
    private val countPattern = Regex("\\(\\d+\\)")

    /**
     * Normalize text by replacing dynamic patterns with stable tokens.
     *
     * - "3:45 PM" → "[T]"
     * - "5 hours ago" → "[RT]"
     * - "(42)" → "[N]"
     *
     * This ensures hash stability when only timestamps/counts change,
     * which is common in email/messaging list items.
     *
     * @param text Raw text from a UI element (already trimmed to 30 chars)
     * @return Normalized text with dynamic patterns masked
     */
    fun normalizeText(text: String): String {
        return text
            .replace(timePattern, "[T]")
            .replace(relativeTimePattern, "[RT]")
            .replace(countPattern, "[N]")
            .trim()
    }

    /**
     * Assemble a screen hash from pre-collected structural signatures and content digest.
     *
     * The hash is a hybrid of:
     * 1. Screen dimensions (so different orientations/devices get separate entries)
     * 2. Sorted structural signatures (class:resourceId:depth:childCount:flags)
     * 3. Content digest from scrollable containers (first 5 + last 5 text snippets)
     *
     * @param signatures Structural signatures collected by platform code
     * @param contentDigest Normalized text snippets from scrollable container children
     * @param screenWidth Display width in pixels
     * @param screenHeight Display height in pixels
     * @return 16-character hex hash string
     */
    fun assembleScreenHash(
        signatures: List<String>,
        contentDigest: List<String>,
        screenWidth: Int,
        screenHeight: Int
    ): String {
        val dimensionKey = "${screenWidth}x${screenHeight}"

        val contentKey = if (contentDigest.isNotEmpty()) {
            val first5 = contentDigest.take(5).joinToString(",")
            val last5 = contentDigest.takeLast(5).joinToString(",")
            "|ct${contentDigest.size}:$first5:$last5"
        } else ""

        val signature = "$dimensionKey|${signatures.sorted().joinToString("|")}$contentKey"
        return HashUtils.calculateHash(signature).take(16)
    }

    /**
     * Create a ScreenInfo from exploration results.
     *
     * Uses kotlinx.datetime Clock for cross-platform time instead of
     * platform-specific System.currentTimeMillis().
     *
     * @param hash Screen hash identifying this layout
     * @param packageName Application package name
     * @param activityName Current activity name (nullable)
     * @param elementCount Total extracted elements
     * @param actionableCount Clickable/scrollable/long-clickable elements
     * @param commandCount Voice commands generated
     * @param isCached Whether this screen was served from cache
     * @return Populated ScreenInfo instance
     */
    fun createScreenInfo(
        hash: String,
        packageName: String,
        activityName: String?,
        elementCount: Int,
        actionableCount: Int,
        commandCount: Int,
        isCached: Boolean
    ): ScreenInfo {
        return ScreenInfo(
            hash = hash,
            packageName = packageName,
            activityName = activityName,
            appVersion = "",
            elementCount = elementCount,
            actionableCount = actionableCount,
            commandCount = commandCount,
            scannedAt = Clock.System.now().toEpochMilliseconds(),
            isCached = isCached
        )
    }
}
