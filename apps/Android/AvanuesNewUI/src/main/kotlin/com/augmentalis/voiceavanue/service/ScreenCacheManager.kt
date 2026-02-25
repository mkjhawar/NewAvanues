/**
 * ScreenCacheManager.kt - Screen hash generation and caching
 *
 * Migrated from VoiceOS to Avanues consolidated app.
 * Caches screen layouts to avoid redundant rescanning.
 *
 * Pure algorithms (text normalization, hash assembly, ScreenInfo creation)
 * are in KMP: ScreenHashAssembler.kt. This file contains only the Android-specific
 * AccessibilityNodeInfo signature collection.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.content.res.Resources
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.ScreenHashAssembler
import com.augmentalis.voiceoscore.ScreenInfo

private const val TAG = "ScreenCacheManager"

/**
 * Manages screen hash generation and caching for known screens.
 *
 * Screen hashing allows recognizing previously-visited screens and
 * loading cached commands instead of re-scanning the entire screen.
 */
class ScreenCacheManager(private val resources: Resources) {

    /**
     * Generate a hash of the current screen for comparison.
     * Includes screen dimensions so different orientations get separate cache entries.
     *
     * Uses a hybrid hash: structural signatures + content digest from scrollable containers.
     * The content digest captures visible text snippets so the hash changes when new content
     * scrolls into view or when navigating between screens with similar structure (e.g.,
     * Gmail inbox list → email detail view).
     */
    fun generateScreenHash(rootNode: AccessibilityNodeInfo): String {
        val signatures = mutableListOf<String>()
        val contentDigest = mutableListOf<String>()
        collectElementSignatures(rootNode, signatures, contentDigest = contentDigest, maxDepth = 8)

        val displayMetrics = resources.displayMetrics
        return ScreenHashAssembler.assembleScreenHash(
            signatures = signatures,
            contentDigest = contentDigest,
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels
        )
    }

    /**
     * Collects structural element signatures AND content digest for hashing.
     *
     * Structural signatures use className, resourceId, depth, childCount, click/scroll flags.
     * Content digest captures text from scrollable container children so the hash changes
     * when new content scrolls into view.
     *
     * @param contentDigest Collects normalized text snippets from scrollable container children.
     *                      Used to make the hash content-aware for scroll and navigation detection.
     */
    fun collectElementSignatures(
        node: AccessibilityNodeInfo,
        signatures: MutableList<String>,
        depth: Int = 0,
        maxDepth: Int = 5,
        contentDigest: MutableList<String> = mutableListOf(),
        inScrollableContainer: Boolean = false
    ) {
        if (depth > maxDepth) return

        val className = node.className?.toString()?.substringAfterLast(".") ?: ""
        val resourceId = node.viewIdResourceName?.substringAfterLast("/") ?: ""
        val isClickable = if (node.isClickable) "C" else ""
        val isScrollable = if (node.isScrollable) "S" else ""

        val isScrollableContainer = className in listOf(
            "RecyclerView", "ListView", "GridView", "ScrollView",
            "HorizontalScrollView", "NestedScrollView", "ViewPager", "ViewPager2"
        ) || node.isScrollable

        val childCount = if (isScrollableContainer) "v" else "c${node.childCount}"

        if (className.isNotEmpty() || resourceId.isNotEmpty()) {
            signatures.add("$className:$resourceId:d$depth:$childCount:$isClickable$isScrollable")
        }

        // Collect text from scrollable container children for content digest
        val isInsideScrollable = inScrollableContainer || isScrollableContainer
        if (isInsideScrollable && contentDigest.size < 30) {
            val text = node.text?.toString()?.take(30)
            if (!text.isNullOrBlank()) {
                val normalized = ScreenHashAssembler.normalizeText(text)
                if (normalized.isNotBlank() && normalized.length > 2) {
                    contentDigest.add(normalized)
                }
            }
        }

        val childDepthLimit = if (isScrollableContainer) depth + 2 else maxDepth
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectElementSignatures(
                    child, signatures, depth + 1, childDepthLimit,
                    contentDigest, isInsideScrollable
                )
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    /**
     * Create a new ScreenInfo from exploration results.
     * Delegates to KMP ScreenHashAssembler for cross-platform consistency.
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
        return ScreenHashAssembler.createScreenInfo(
            hash = hash,
            packageName = packageName,
            activityName = activityName,
            elementCount = elementCount,
            actionableCount = actionableCount,
            commandCount = commandCount,
            isCached = isCached
        )
    }
}
