@file:Suppress("DEPRECATION") // recycle() deprecated in Android 14+

package com.augmentalis.voiceoscore

import android.content.res.Resources
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.foundation.util.HashUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "ScreenCacheManager"

/**
 * Manages screen hash generation and caching for known screens.
 *
 * Screen hashing allows recognizing previously-visited screens and
 * loading cached commands instead of re-scanning the entire screen.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-20
 */
class ScreenCacheManager(
    private val screenHashRepository: ScreenHashRepository,
    private val resources: Resources
) {

    private val _currentScreenInfo = MutableStateFlow<ScreenInfo?>(null)
    val currentScreenInfo: StateFlow<ScreenInfo?> = _currentScreenInfo.asStateFlow()

    /**
     * Generate a hash of the current screen for comparison.
     *
     * IMPORTANT: Includes screen dimensions in the hash so that different
     * orientations/window sizes get separate cache entries. This means:
     * - Portrait 1080x1920 = hash1
     * - Landscape 1920x1080 = hash2
     * - Freeform 800x600 = hash3
     *
     * When user rotates back to portrait, we load from hash1 cache (instant).
     *
     * @param rootNode Root accessibility node of the current screen
     * @return Hash string uniquely identifying this screen layout
     */
    fun generateScreenHash(rootNode: AccessibilityNodeInfo): String {
        val elements = mutableListOf<String>()
        collectElementSignatures(rootNode, elements, maxDepth = 5)

        // Include screen dimensions in hash for orientation/freeform support
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val dimensionKey = "${screenWidth}x${screenHeight}"

        val signature = "$dimensionKey|${elements.sorted().joinToString("|")}"
        return HashUtils.calculateHash(signature).take(16)
    }

    /**
     * Collect element signatures for screen hashing.
     *
     * STRUCTURAL HASH: Uses only structural properties to create stable hash.
     * Does NOT include text content to avoid false rescans from:
     * - Changing counters ("3 unread" -> "4 unread")
     * - Timestamps ("10:45 AM" -> "10:46 AM")
     * - Dynamic data (user names, messages, etc.)
     * - Loading states
     *
     * What IS included (stable structural properties):
     * - className: The widget type (TextView, Button, etc.)
     * - resourceId: Developer-assigned ID (stable across sessions)
     * - depth: Position in hierarchy
     * - childCount: Number of children (structural shape)
     * - isClickable/isScrollable: Interaction flags
     *
     * This ensures same screens always produce same hash, regardless of content.
     *
     * @param node Current accessibility node
     * @param signatures Output list for collected signatures
     * @param depth Current depth in tree
     * @param maxDepth Maximum depth to traverse
     */
    fun collectElementSignatures(
        node: AccessibilityNodeInfo,
        signatures: MutableList<String>,
        depth: Int = 0,
        maxDepth: Int = 5
    ) {
        if (depth > maxDepth) return

        // Build signature from STRUCTURAL properties only (no text content!)
        val className = node.className?.toString()?.substringAfterLast(".") ?: ""
        val resourceId = node.viewIdResourceName?.substringAfterLast("/") ?: ""
        val isClickable = if (node.isClickable) "C" else ""
        val isScrollable = if (node.isScrollable) "S" else ""

        // For scrollable containers (RecyclerView, ListView, ScrollView), don't include childCount
        // as it changes when user scrolls (items are recycled/added)
        val isScrollableContainer = className in listOf(
            "RecyclerView", "ListView", "GridView", "ScrollView",
            "HorizontalScrollView", "NestedScrollView", "ViewPager", "ViewPager2"
        ) || node.isScrollable

        val childCount = if (isScrollableContainer) {
            "v"  // "v" for variable - indicates scrollable container
        } else {
            "c${node.childCount}"
        }

        // Only include elements with identifying structure
        if (className.isNotEmpty() || resourceId.isNotEmpty()) {
            // Format: "ClassName:resourceId:depth:childCount:flags"
            // Example: "TextView:message_count:d2:c0:C" (TextView at depth 2, no children, clickable)
            // Example: "RecyclerView:list:d1:v:S" (RecyclerView at depth 1, variable children, scrollable)
            signatures.add("$className:$resourceId:d$depth:$childCount:$isClickable$isScrollable")
        }

        // Recurse into children (but limit depth in scrollable containers to avoid
        // including recycled item content which changes on scroll)
        val childDepthLimit = if (isScrollableContainer) depth + 2 else maxDepth
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                collectElementSignatures(child, signatures, depth + 1, childDepthLimit)
            } finally {
                // Guaranteed cleanup even if exception occurs during recursion
                child.recycle()
            }
        }
    }

    /**
     * Check if a screen hash is known (cached).
     *
     * @param screenHash Hash to check
     * @return true if screen is in cache
     */
    suspend fun hasScreen(screenHash: String): Boolean {
        return screenHashRepository.hasScreen(screenHash)
    }

    /**
     * Get cached app version for a screen.
     *
     * @param screenHash Screen hash
     * @return Version string or null if not cached
     */
    suspend fun getAppVersion(screenHash: String): String? {
        return screenHashRepository.getAppVersion(screenHash)
    }

    /**
     * Get cached commands for a screen.
     *
     * @param screenHash Screen hash
     * @return List of cached commands
     */
    suspend fun getCommandsForScreen(screenHash: String): List<QuantizedCommand> {
        return screenHashRepository.getCommandsForScreen(screenHash)
    }

    /**
     * Get screen info for a hash.
     *
     * @param screenHash Screen hash
     * @return ScreenInfo or null if not found
     */
    suspend fun getScreenInfo(screenHash: String): ScreenInfo? {
        return screenHashRepository.getScreenInfo(screenHash)
    }

    /**
     * Save screen to cache.
     *
     * @param hash Screen hash
     * @param packageName App package name
     * @param activityName Optional activity name
     * @param appVersion App version string
     * @param elementCount Number of elements on screen
     */
    suspend fun saveScreen(
        hash: String,
        packageName: String,
        activityName: String?,
        appVersion: String,
        elementCount: Int
    ) {
        screenHashRepository.saveScreen(hash, packageName, activityName, appVersion, elementCount)
    }

    /**
     * Save commands for a cached screen.
     *
     * @param screenHash Screen hash
     * @param commands Commands to cache
     */
    suspend fun saveCommandsForScreen(screenHash: String, commands: List<QuantizedCommand>) {
        screenHashRepository.saveCommandsForScreen(screenHash, commands)
    }

    /**
     * Update current screen info state flow.
     *
     * @param screenInfo Screen info to set
     */
    fun updateCurrentScreenInfo(screenInfo: ScreenInfo?) {
        _currentScreenInfo.value = screenInfo
    }

    /**
     * Create a new ScreenInfo from exploration results.
     *
     * @param hash Screen hash
     * @param packageName Package name
     * @param activityName Activity name (nullable)
     * @param appVersion App version
     * @param elementCount Total elements
     * @param actionableCount Clickable + scrollable elements
     * @param commandCount Generated commands
     * @param isCached Whether this was loaded from cache
     * @return ScreenInfo instance
     */
    fun createScreenInfo(
        hash: String,
        packageName: String,
        activityName: String?,
        appVersion: String,
        elementCount: Int,
        actionableCount: Int,
        commandCount: Int,
        isCached: Boolean
    ): ScreenInfo {
        return ScreenInfo(
            hash = hash,
            packageName = packageName,
            activityName = activityName,
            appVersion = appVersion,
            elementCount = elementCount,
            actionableCount = actionableCount,
            commandCount = commandCount,
            scannedAt = System.currentTimeMillis(),
            isCached = isCached
        )
    }

    /**
     * Clear cached screens for a specific package.
     *
     * @param packageName Package name to clear
     * @return Number of screens cleared
     */
    suspend fun clearScreensForPackage(packageName: String): Int {
        return screenHashRepository.clearScreensForPackage(packageName)
    }

    /**
     * Clear all cached screens.
     *
     * @return Number of screens cleared
     */
    suspend fun clearAllScreens(): Int {
        return screenHashRepository.clearAllScreens()
    }

    /**
     * Get total count of cached screens.
     *
     * @return Count of cached screens
     */
    suspend fun getScreenCount(): Int {
        return screenHashRepository.getScreenCount()
    }

    /**
     * Get cached screen count for a specific package.
     *
     * @param packageName Package name
     * @return Count of cached screens for package
     */
    suspend fun getScreenCountForPackage(packageName: String): Int {
        return screenHashRepository.getScreenCountForPackage(packageName)
    }
}
