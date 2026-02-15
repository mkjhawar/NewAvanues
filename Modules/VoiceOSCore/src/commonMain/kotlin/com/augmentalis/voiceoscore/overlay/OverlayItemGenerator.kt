/**
 * OverlayItemGenerator.kt - Generates overlay items from extracted elements
 *
 * Converts ElementInfo data from accessibility/DOM extraction into
 * NumberOverlayItem instances for display as numbered badges.
 *
 * Pure Kotlin logic — no platform dependencies. Uses ElementLabels (KMP)
 * for list-item detection and ElementFingerprint for unified AVID generation.
 *
 * AVIDs are now unified with the command system (ElementFingerprint):
 * Format: {TypeCode}:{hash8} (e.g., "BTN:a3f2e1c9")
 * Hash includes packageName for cross-app uniqueness and VOS export portability.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore

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
     * Uses ElementLabels.findTopLevelListItems for smart row detection.
     *
     * @param elements All extracted elements
     * @param hierarchy Hierarchy information
     * @param labels Derived labels map (index -> label)
     * @param packageName App package name (included in AVID hash for cross-app uniqueness)
     * @return List of NumberOverlayItem ready for display
     */
    fun generateForListApp(
        elements: List<ElementInfo>,
        hierarchy: List<HierarchyNode>,
        labels: Map<Int, String>,
        packageName: String = ""
    ): List<NumberOverlayItem> {
        val listItems = elements.filter { it.listIndex >= 0 }
        if (listItems.isEmpty()) return emptyList()

        val topLevelItems = ElementLabels.findTopLevelListItems(listItems, elements)

        val items = topLevelItems.mapIndexed { index, element ->
            val elementIndex = elements.indexOf(element)
            NumberOverlayItem(
                number = index + 1,
                label = labels[elementIndex] ?: "",
                left = element.bounds.left,
                top = element.bounds.top,
                right = element.bounds.right,
                bottom = element.bounds.bottom,
                avid = ElementFingerprint.fromElementInfo(element, packageName)
            )
        }
        return items
    }

    /**
     * Generate overlay items for all clickable elements.
     * Used for general apps that aren't list-based.
     *
     * @param elements All extracted elements
     * @param labels Derived labels map
     * @param packageName App package name (included in AVID hash for cross-app uniqueness)
     * @return List of NumberOverlayItem
     */
    fun generateForAllClickable(
        elements: List<ElementInfo>,
        labels: Map<Int, String>,
        packageName: String = ""
    ): List<NumberOverlayItem> {
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
            LoggingUtils.d("Too many clickable elements (${clickableElements.size}), skipping overlay", TAG)
            return emptyList()
        }

        val items = clickableElements.mapIndexed { index, element ->
            val elementIndex = elements.indexOf(element)
            NumberOverlayItem(
                number = index + 1,
                label = labels[elementIndex] ?: "",
                left = element.bounds.left,
                top = element.bounds.top,
                right = element.bounds.right,
                bottom = element.bounds.bottom,
                avid = ElementFingerprint.fromElementInfo(element, packageName)
            )
        }
        return items
    }
}
