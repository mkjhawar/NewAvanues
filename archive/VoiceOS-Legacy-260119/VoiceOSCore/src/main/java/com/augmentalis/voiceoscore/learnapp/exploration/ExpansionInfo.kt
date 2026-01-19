/**
 * ExpansionInfo.kt - Expansion state information
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Tracks element expansion state during exploration.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Expansion Info
 *
 * Tracks the state of expandable elements during exploration.
 *
 * @property node The accessibility node being expanded
 * @property isExpanded Whether the element is currently expanded
 * @property expansionDepth Depth of expansion (for nested expansions)
 * @property childCount Number of children after expansion
 * @property timestamp When the expansion occurred
 */
data class ExpansionInfo(
    val node: AccessibilityNodeInfo? = null,
    val isExpanded: Boolean = false,
    val expansionDepth: Int = 0,
    val childCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /** Create empty expansion info */
        fun empty() = ExpansionInfo()

        /** Create from expanded node */
        fun fromNode(node: AccessibilityNodeInfo, expanded: Boolean = true): ExpansionInfo {
            return ExpansionInfo(
                node = node,
                isExpanded = expanded,
                childCount = node.childCount,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
