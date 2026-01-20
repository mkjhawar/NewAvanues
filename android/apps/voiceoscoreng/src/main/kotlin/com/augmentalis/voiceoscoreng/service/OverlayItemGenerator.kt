package com.augmentalis.voiceoscoreng.service

import com.augmentalis.voiceoscore.CommandGenerator
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ElementInfo

/**
 * Generates overlay items for numbered badge display.
 * Extracted from DynamicCommandGenerator for SOLID compliance.
 * Single Responsibility: Generate overlay items for visual badges.
 */
object OverlayItemGenerator {

    /**
     * Generate overlay items for numbered badge display.
     *
     * @param listItems Elements that are list items
     * @param allElements All extracted elements
     * @param packageName App package name
     * @return List of overlay items for visual display
     */
    fun generate(
        listItems: List<ElementInfo>,
        allElements: List<ElementInfo>,
        packageName: String
    ): List<OverlayStateManager.NumberOverlayItem> {
        val rowElements = ElementExtractor.findTopLevelListItems(listItems, allElements)
            .sortedBy { it.bounds.top }

        return rowElements.mapIndexed { index, element ->
            createOverlayItem(index + 1, element, packageName)
        }
    }

    /**
     * Generate overlay items incrementally, preserving numbers for existing items.
     *
     * @param listItems Elements that are list items
     * @param allElements All extracted elements
     * @param packageName App package name
     * @param avidAssignments Map of element hash to assigned AVID number
     * @return List of overlay items with preserved numbering
     */
    fun generateIncremental(
        listItems: List<ElementInfo>,
        allElements: List<ElementInfo>,
        packageName: String,
        avidAssignments: Map<String, Int>
    ): List<OverlayStateManager.NumberOverlayItem> {
        val rowElements = ElementExtractor.findTopLevelListItems(listItems, allElements)
            .sortedBy { it.bounds.top }

        return rowElements.mapIndexed { index, element ->
            val hash = element.hashCode().toString()
            val assignedNumber = avidAssignments[hash] ?: (index + 1)
            createOverlayItem(assignedNumber, element, packageName)
        }
    }

    private fun createOverlayItem(
        number: Int,
        element: ElementInfo,
        packageName: String
    ): OverlayStateManager.NumberOverlayItem {
        val label = CommandGenerator.extractShortLabel(element) ?: ""
        val fingerprint = ElementFingerprint.generate(
            className = element.className,
            packageName = packageName,
            resourceId = element.resourceId,
            text = element.text,
            contentDesc = element.contentDescription
        )

        return OverlayStateManager.NumberOverlayItem(
            number = number,
            label = label,
            left = element.bounds.left,
            top = element.bounds.top,
            right = element.bounds.right,
            bottom = element.bounds.bottom,
            vuid = fingerprint
        )
    }
}
