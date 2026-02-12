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
     * Generate a content-based AVID for an element.
     *
     * Uses className + resourceId + text + contentDescription to create a stable
     * identifier that survives scroll recycling. Unlike bounds-based AVIDs, this
     * returns the same value for the same element even if its screen position changes
     * (e.g., after scrolling down and back up in a RecyclerView).
     */
    private fun generateContentAvid(element: ElementInfo): String {
        val contentKey = buildString {
            append(element.className.substringAfterLast("."))
            if (element.resourceId.isNotBlank()) {
                append("|")
                append(element.resourceId.substringAfterLast("/"))
            }
            if (element.text.isNotBlank()) {
                append("|")
                append(element.text.take(40))
            }
            if (element.contentDescription.isNotBlank()) {
                append("|")
                append(element.contentDescription.take(40))
            }
        }
        val hash = contentKey.hashCode().toUInt().toString(16).padStart(8, '0')
        return "dyn_$hash"
    }

    /**
     * Deduplicate AVIDs within a single overlay item list.
     *
     * When multiple elements share the same content-based AVID (e.g., three
     * "Google Flights" emails with identical subject+sender), append an ordinal
     * suffix so each gets a unique AVID → unique badge number.
     *
     * The ordinal is based on list order (top-to-bottom, which is the adapter
     * order in a RecyclerView). This is stable across scroll because the adapter
     * doesn't reorder items on scroll.
     */
    private fun deduplicateAvids(
        items: List<OverlayStateManager.NumberOverlayItem>
    ): List<OverlayStateManager.NumberOverlayItem> {
        val seen = mutableMapOf<String, Int>()
        return items.map { item ->
            val count = seen.getOrDefault(item.avid, 0)
            seen[item.avid] = count + 1
            if (count == 0) item else item.copy(avid = "${item.avid}_$count")
        }
    }

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

        val items = topLevelItems.mapIndexed { index, element ->
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
                avid = generateContentAvid(element)
            )
        }
        return deduplicateAvids(items)
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

        val items = clickableElements.mapIndexed { index, element ->
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
                avid = generateContentAvid(element)
            )
        }
        return deduplicateAvids(items)
    }
}
