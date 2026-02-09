/**
 * OverlayItemGenerator.kt - Generates overlay items from extracted elements
 *
 * Converts ElementInfo data from accessibility extraction into
 * NumberOverlayItem instances for display as numbered badges.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.util.Log
import com.augmentalis.voiceoscore.ElementInfo

private const val TAG = "OverlayItemGenerator"

/**
 * Generates NumberOverlayItem list from extracted UI elements.
 *
 * Strategies:
 * - For list apps (Gmail, WhatsApp, etc.): Uses findTopLevelListItems to identify rows
 * - For general apps: Numbers all clickable elements with valid bounds
 */
object OverlayItemGenerator {

    /**
     * Generate overlay items from extracted elements for list-based apps.
     * Uses ElementExtractor.findTopLevelListItems for smart row detection.
     *
     * @param elements All extracted elements
     * @param hierarchy Hierarchy information
     * @param labels Derived labels map (index -> label)
     * @return List of NumberOverlayItem ready for display
     */
    fun generateForListApp(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
        labels: Map<Int, String>
    ): List<OverlayStateManager.NumberOverlayItem> {
        val listItems = elements.filter { it.listIndex >= 0 }
        if (listItems.isEmpty()) return emptyList()

        val topLevelItems = ElementExtractor.findTopLevelListItems(listItems, elements)

        return topLevelItems.mapIndexed { index, element ->
            val elementIndex = elements.indexOf(element)
            val label = labels[elementIndex] ?: element.contentDescription.take(20).ifBlank {
                element.text.take(20)
            }

            OverlayStateManager.NumberOverlayItem(
                number = index + 1,
                label = label,
                left = element.bounds.left,
                top = element.bounds.top,
                right = element.bounds.right,
                bottom = element.bounds.bottom,
                avid = "dynamic_${element.bounds.left}_${element.bounds.top}_${element.bounds.right}_${element.bounds.bottom}"
            )
        }
    }

    /**
     * Generate overlay items for all clickable elements.
     * Used for general apps that aren't list-based.
     *
     * @param elements All extracted elements
     * @param labels Derived labels map
     * @return List of NumberOverlayItem
     */
    fun generateForAllClickable(
        elements: List<ElementInfo>,
        labels: Map<Int, String>
    ): List<OverlayStateManager.NumberOverlayItem> {
        val clickableElements = elements.filter { element ->
            (element.isClickable || element.isLongClickable) &&
                    element.isEnabled &&
                    element.bounds.let { b ->
                        val width = b.right - b.left
                        val height = b.bottom - b.top
                        width > 20 && height > 20 && b.top >= 0
                    }
        }

        if (clickableElements.size > 50) {
            Log.d(TAG, "Too many clickable elements (${clickableElements.size}), skipping overlay")
            return emptyList()
        }

        return clickableElements.mapIndexed { index, element ->
            val elementIndex = elements.indexOf(element)
            val label = labels[elementIndex] ?: element.text.take(20).ifBlank {
                element.contentDescription.take(20)
            }

            OverlayStateManager.NumberOverlayItem(
                number = index + 1,
                label = label,
                left = element.bounds.left,
                top = element.bounds.top,
                right = element.bounds.right,
                bottom = element.bounds.bottom,
                avid = "dynamic_${element.bounds.left}_${element.bounds.top}_${element.bounds.right}_${element.bounds.bottom}"
            )
        }
    }
}
