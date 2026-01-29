package com.augmentalis.voiceos.service

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
     * GMAIL FIX: Uses stable ElementFingerprint instead of hashCode() for AVID assignments.
     * This prevents duplicate numbers when elements shift positions during scroll.
     *
     * @param listItems Elements that are list items
     * @param allElements All extracted elements
     * @param packageName App package name
     * @param avidAssignments Map of element fingerprint to assigned AVID number
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

        // Track used numbers to prevent duplicates
        val usedNumbers = mutableSetOf<Int>()
        val result = mutableListOf<OverlayStateManager.NumberOverlayItem>()

        rowElements.forEachIndexed { index, element ->
            // Use stable fingerprint instead of hashCode for reliable matching
            val fingerprint = ElementFingerprint.generate(
                className = element.className,
                packageName = packageName,
                resourceId = element.resourceId,
                text = element.text,
                contentDesc = element.contentDescription
            )

            // Try to use assigned number, but only if not already used
            var assignedNumber = avidAssignments[fingerprint]
            if (assignedNumber != null && usedNumbers.contains(assignedNumber)) {
                // Number already used by another element, assign new number
                assignedNumber = null
            }

            // If no valid assigned number, use next available
            val finalNumber = assignedNumber ?: run {
                var candidate = index + 1
                while (usedNumbers.contains(candidate)) {
                    candidate++
                }
                candidate
            }

            usedNumbers.add(finalNumber)
            result.add(createOverlayItem(finalNumber, element, packageName))
        }

        return result
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
            avid = fingerprint
        )
    }
}
