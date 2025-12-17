/**
 * ScreenExplorer.kt - Explores screen content for learning
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Explores and catalogs screen UI elements for app learning.
 */
package com.augmentalis.voiceoscore.learnapp.exploration

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ScreenState

/**
 * Screen Explorer
 *
 * Explores and catalogs screen UI elements.
 */
class ScreenExplorer(
    private val elementClassifier: ElementClassifier
) {

    /**
     * Explore the current screen
     *
     * @param rootNode Root accessibility node
     * @param packageName Optional package name for context
     * @param depth Optional depth in navigation hierarchy
     */
    fun exploreScreen(
        rootNode: AccessibilityNodeInfo?,
        packageName: String? = null,
        depth: Int = 0
    ): ScreenExplorationResult {
        if (rootNode == null) {
            return ScreenExplorationResult.Error(
                message = "Root node is null"
            )
        }

        val allElements = mutableListOf<ElementInfo>()
        val safeClickable = mutableListOf<ElementInfo>()
        val dangerous = mutableListOf<Pair<ElementInfo, String>>()
        var scrollableCount = 0

        try {
            collectElements(rootNode, allElements, depth = 0, scrollableCounter = { scrollableCount++ })

            for (element in allElements) {
                if (element.isClickable) {
                    if (isDangerousText(element.text) || isDangerousText(element.contentDescription)) {
                        dangerous.add(element to "Contains dangerous keyword")
                    } else {
                        safeClickable.add(element)
                    }
                }
            }

            val screenState = createScreenState(rootNode, allElements, packageName)

            return ScreenExplorationResult.Success(
                screenState = screenState,
                allElements = allElements,
                safeClickableElements = safeClickable,
                dangerousElements = dangerous,
                elementClassifications = emptyList(),
                scrollableContainerCount = scrollableCount
            )
        } catch (e: Exception) {
            return ScreenExplorationResult.Error(
                message = "Error exploring screen: ${e.message}"
            )
        }
    }

    /**
     * Collect all elements from hierarchy using ElementInfo.fromNode
     */
    private fun collectElements(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>,
        depth: Int = 0,
        scrollableCounter: (() -> Unit)? = null
    ) {
        val element = ElementInfo.fromNode(node, depth = depth)
        elements.add(element)

        // Count scrollable containers
        if (node.isScrollable) {
            scrollableCounter?.invoke()
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectElements(child, elements, depth + 1, scrollableCounter)
            }
        }
    }

    /**
     * Check if text contains dangerous keywords
     */
    private fun isDangerousText(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase()
        return DANGEROUS_KEYWORDS.any { it in lower }
    }

    /**
     * Create screen state from exploration
     */
    private fun createScreenState(
        rootNode: AccessibilityNodeInfo,
        elements: List<ElementInfo>,
        packageNameOverride: String? = null
    ): ScreenState {
        return ScreenState(
            packageName = packageNameOverride ?: rootNode.packageName?.toString() ?: "",
            activityName = null,
            hash = calculateHash(elements),
            elementCount = elements.size,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Calculate hash for screen state
     */
    private fun calculateHash(elements: List<ElementInfo>): String {
        val fingerprint = elements.joinToString("|") {
            "${it.className}:${it.resourceId}:${it.isClickable}"
        }
        return fingerprint.hashCode().toString()
    }

    companion object {
        private val DANGEROUS_KEYWORDS = listOf(
            "delete", "remove", "logout", "sign out", "uninstall",
            "clear", "erase", "reset", "format", "wipe"
        )
    }
}
