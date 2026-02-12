/**
 * DynamicCommandGenerator.kt - App-level wrapper for screen scraping and overlay generation
 *
 * Composes: ElementExtractor + OverlayItemGenerator + OverlayStateManager
 * to bridge the accessibility service screen extraction with the overlay display.
 *
 * This is the app-level coordinator that was deleted during consolidation.
 * VoiceOSCore handles command generation internally; this class handles
 * the overlay badge generation that happens at the app level.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.content.res.Resources
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.ElementInfo

private const val TAG = "DynamicCommandGen"

/**
 * Generates overlay badges from the accessibility tree.
 *
 * Flow:
 * 1. Accessibility service provides root node
 * 2. ElementExtractor extracts all UI elements + hierarchy
 * 3. OverlayItemGenerator converts to NumberOverlayItems
 * 4. OverlayStateManager updates the overlay display
 *
 * Screen caching uses ScreenCacheManager to avoid redundant extractions.
 */
class DynamicCommandGenerator(
    resources: Resources,
    private val numberingExecutor: OverlayNumberingExecutor
) {

    private val screenCacheManager = ScreenCacheManager(resources)
    private var lastScreenHash: String = ""

    // Track top-level element structure to calculate structural change ratio.
    // Used to distinguish scroll (low change) from major navigation (high change)
    // within target apps like Gmail.
    private var lastTopLevelSignatures: Set<String> = emptySet()

    /**
     * Process the current screen and update overlay badges.
     *
     * @param rootNode Root accessibility node
     * @param packageName Current app package name
     * @param isTargetApp Whether this app is in the TARGET_APPS list
     */
    fun processScreen(
        rootNode: AccessibilityNodeInfo,
        packageName: String,
        isTargetApp: Boolean
    ) {
        try {
            // Executor handles app-change and screen-change reset logic
            val screenHash = screenCacheManager.generateScreenHash(rootNode)
            val isNewScreen = screenHash != lastScreenHash

            // Calculate structural change ratio for target app major navigation detection
            val structuralChangeRatio = if (isNewScreen && isTargetApp) {
                calculateStructuralChangeRatio(rootNode)
            } else 0f

            val didReset = numberingExecutor.handleScreenContext(
                packageName, isTargetApp, isNewScreen, structuralChangeRatio
            )
            if (didReset) {
                Log.d(TAG, "Numbering reset for $packageName (app/screen change, ratio=$structuralChangeRatio)")
                lastScreenHash = ""
            }

            // Re-check hash after potential reset
            val currentHash = if (didReset) screenCacheManager.generateScreenHash(rootNode) else screenHash
            if (currentHash == lastScreenHash) {
                Log.v(TAG, "Screen unchanged, skipping overlay update")
                return
            }
            lastScreenHash = currentHash

            // Extract elements
            val elements = mutableListOf<ElementInfo>()
            val hierarchy = mutableListOf<HierarchyNode>()
            val seenHashes = mutableSetOf<String>()
            val duplicates = mutableListOf<DuplicateInfo>()

            ElementExtractor.extractElements(
                node = rootNode,
                elements = elements,
                hierarchy = hierarchy,
                seenHashes = seenHashes,
                duplicates = duplicates,
                depth = 0
            )

            if (elements.isEmpty()) {
                OverlayStateManager.clearOverlayItems()
                return
            }

            // Derive labels for all elements
            val labels = ElementExtractor.deriveElementLabels(elements, hierarchy)

            // Generate overlay items based on app type
            val overlayItems = if (isTargetApp) {
                // List-based app: find list items and number them
                OverlayItemGenerator.generateForListApp(elements, hierarchy, labels)
            } else {
                // General app with overlay ON mode: number all clickable elements
                if (OverlayStateManager.numbersOverlayMode.value == OverlayStateManager.NumbersOverlayMode.ON) {
                    OverlayItemGenerator.generateForAllClickable(elements, labels)
                } else {
                    emptyList()
                }
            }

            // Executor assigns per-container numbers, then simple setter
            if (overlayItems.isNotEmpty()) {
                val numbered = numberingExecutor.assignNumbers(overlayItems)
                OverlayStateManager.updateNumberedOverlayItems(numbered)
            } else {
                OverlayStateManager.clearOverlayItems()
            }

            val actionableCount = elements.count { it.isClickable || it.isLongClickable || it.isScrollable }
            Log.d(TAG, "Processed $packageName: ${elements.size} elements, $actionableCount actionable, ${overlayItems.size} overlay items")

        } catch (e: Exception) {
            Log.e(TAG, "Error processing screen for $packageName", e)
        }
    }

    /**
     * Calculate how much the top-level UI structure changed between screens.
     *
     * Compares the set of top-level element signatures (depth <= 3) between
     * the previous and current screen. Returns a ratio from 0.0 (identical) to
     * 1.0 (completely different).
     *
     * Used to distinguish:
     * - Scroll in Gmail inbox (ratio ~0.1-0.3, same RecyclerView + toolbar structure)
     * - Navigation inbox → email detail (ratio ~0.7-0.9, fundamentally different layout)
     */
    private fun calculateStructuralChangeRatio(rootNode: AccessibilityNodeInfo): Float {
        val currentSignatures = mutableSetOf<String>()
        collectTopLevelSignatures(rootNode, currentSignatures, depth = 0, maxDepth = 3)

        val ratio = if (lastTopLevelSignatures.isEmpty()) {
            0f  // First screen, no comparison baseline
        } else {
            val intersection = lastTopLevelSignatures.intersect(currentSignatures)
            val unionSize = maxOf(lastTopLevelSignatures.size, currentSignatures.size)
            if (unionSize == 0) 0f else 1f - (intersection.size.toFloat() / unionSize.toFloat())
        }

        lastTopLevelSignatures = currentSignatures
        return ratio
    }

    /**
     * Collect top-level structural signatures for change ratio calculation.
     * Only goes to depth 3 — enough to capture toolbar, content area type, bottom nav.
     */
    private fun collectTopLevelSignatures(
        node: AccessibilityNodeInfo,
        signatures: MutableSet<String>,
        depth: Int,
        maxDepth: Int
    ) {
        if (depth > maxDepth) return

        val className = node.className?.toString()?.substringAfterLast(".") ?: ""
        val resourceId = node.viewIdResourceName?.substringAfterLast("/") ?: ""

        if (className.isNotEmpty() || resourceId.isNotEmpty()) {
            signatures.add("$className:$resourceId:d$depth")
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectTopLevelSignatures(child, signatures, depth + 1, maxDepth)
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    /**
     * Invalidate the screen hash so the next processScreen() always runs.
     * Called after scroll to force overlay refresh — we KNOW content changed,
     * no need to detect it via hash comparison.
     *
     * Unlike clearCache(), this does NOT clear overlay items or top-level signatures.
     * Overlay items are preserved so the transition is smooth (old badges stay
     * until new ones replace them). Top-level signatures are preserved so
     * structural change ratio still works for navigation detection.
     */
    fun invalidateScreenHash() {
        lastScreenHash = ""
    }

    /**
     * Clear cached state when switching apps.
     */
    fun clearCache() {
        lastScreenHash = ""
        lastTopLevelSignatures = emptySet()
        OverlayStateManager.clearOverlayItems()
    }
}
