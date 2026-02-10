/**
 * ScreenCacheManager.kt - Screen hash generation and caching
 *
 * Migrated from VoiceOS to Avanues consolidated app.
 * Caches screen layouts to avoid redundant rescanning.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.content.res.Resources
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.foundation.util.HashUtils
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
     */
    fun generateScreenHash(rootNode: AccessibilityNodeInfo): String {
        val elements = mutableListOf<String>()
        collectElementSignatures(rootNode, elements, maxDepth = 8)

        val displayMetrics = resources.displayMetrics
        val dimensionKey = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
        val signature = "$dimensionKey|${elements.sorted().joinToString("|")}"
        return HashUtils.calculateHash(signature).take(16)
    }

    /**
     * Collects structural element signatures for hashing.
     * Uses ONLY structural properties (not text content) for stability.
     */
    fun collectElementSignatures(
        node: AccessibilityNodeInfo,
        signatures: MutableList<String>,
        depth: Int = 0,
        maxDepth: Int = 5
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

        val childDepthLimit = if (isScrollableContainer) depth + 2 else maxDepth
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectElementSignatures(child, signatures, depth + 1, childDepthLimit)
                child.recycle()
            }
        }
    }

    /**
     * Create a new ScreenInfo from exploration results.
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
            scannedAt = System.currentTimeMillis(),
            isCached = isCached
        )
    }
}
