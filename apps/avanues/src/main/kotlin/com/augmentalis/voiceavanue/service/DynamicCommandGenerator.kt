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

            val didReset = numberingExecutor.handleScreenContext(packageName, isTargetApp, isNewScreen)
            if (didReset) {
                Log.d(TAG, "Numbering reset for $packageName (app/screen change)")
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
     * Clear cached state when switching apps.
     */
    fun clearCache() {
        lastScreenHash = ""
        OverlayStateManager.clearOverlayItems()
    }
}
