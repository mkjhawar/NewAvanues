/**
 * ExpandableControlDetector.kt - Detects expandable/collapsible controls
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Detects expandable controls like dropdowns, accordions, and menus
 */

package com.augmentalis.voiceoscore.learnapp.detection

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Expandable Control Detector
 *
 * Identifies expandable/collapsible UI controls that need special handling
 * during exploration to discover hidden content.
 */
object ExpandableControlDetector {

    // Class names that typically indicate expandable controls
    private val EXPANDABLE_CLASS_PATTERNS = listOf(
        "Spinner",
        "DropDown",
        "ExpansionPanel",
        "Accordion",
        "ExpandableListView",
        "TreeView",
        "CollapsibleToolbar",
        "NavigationDrawer"
    )

    // Resource ID patterns that indicate expandable controls
    private val EXPANDABLE_RESOURCE_PATTERNS = listOf(
        "dropdown",
        "spinner",
        "expand",
        "collapse",
        "menu",
        "drawer"
    )

    /**
     * Check if node is an expandable control
     *
     * @param node Accessibility node to check
     * @return true if node appears to be expandable
     */
    fun isExpandableControl(node: AccessibilityNodeInfo): Boolean {
        val className = node.className?.toString() ?: ""
        val resourceId = node.viewIdResourceName ?: ""

        // Check class name patterns
        for (pattern in EXPANDABLE_CLASS_PATTERNS) {
            if (className.contains(pattern, ignoreCase = true)) {
                return true
            }
        }

        // Check resource ID patterns
        for (pattern in EXPANDABLE_RESOURCE_PATTERNS) {
            if (resourceId.contains(pattern, ignoreCase = true)) {
                return true
            }
        }

        // Check if has expand/collapse actions
        val hasExpandAction = node.actionList.any { action ->
            action.id == AccessibilityNodeInfo.ACTION_EXPAND ||
            action.id == AccessibilityNodeInfo.ACTION_COLLAPSE
        }

        return hasExpandAction
    }

    /**
     * Check if node is currently expanded
     *
     * @param node Accessibility node to check
     * @return true if node is expanded, false if collapsed, null if not expandable
     */
    fun isExpanded(node: AccessibilityNodeInfo): Boolean? {
        // Check expand/collapse state if available
        if (node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_COLLAPSE }) {
            return true  // Has collapse action = currently expanded
        }
        if (node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_EXPAND }) {
            return false  // Has expand action = currently collapsed
        }
        return null  // Not an expandable control
    }

    /**
     * Find all expandable controls in tree
     *
     * @param rootNode Root of tree to search
     * @return List of expandable control nodes
     */
    fun findExpandableControls(rootNode: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        if (rootNode == null) return emptyList()

        val expandables = mutableListOf<AccessibilityNodeInfo>()
        findExpandablesRecursive(rootNode, expandables)
        return expandables
    }

    private fun findExpandablesRecursive(
        node: AccessibilityNodeInfo,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (isExpandableControl(node)) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findExpandablesRecursive(child, results)
            }
        }
    }
}
