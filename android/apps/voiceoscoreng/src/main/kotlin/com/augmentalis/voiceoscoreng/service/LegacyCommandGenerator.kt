package com.augmentalis.voiceoscoreng.service

import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ElementInfo

/**
 * Generates legacy GeneratedCommand objects for UI display.
 * Extracted from DynamicCommandGenerator for SOLID compliance.
 * Single Responsibility: Generate legacy command format for backwards compatibility.
 */
object LegacyCommandGenerator {

    /**
     * Generate legacy GeneratedCommand objects for UI display.
     *
     * @param elements All extracted UI elements
     * @param elementLabels Pre-derived labels for elements
     * @param packageName App package name
     * @return List of legacy GeneratedCommand objects
     */
    fun generate(
        elements: List<ElementInfo>,
        elementLabels: Map<Int, String>,
        packageName: String
    ): List<GeneratedCommand> {
        return elements.mapIndexedNotNull { index, element ->
            if (!element.isClickable && !element.isScrollable) return@mapIndexedNotNull null

            val label = elementLabels[index]
            if (label == null || label == element.className.substringAfterLast(".")) {
                return@mapIndexedNotNull null
            }

            val actionType = deriveActionType(element)
            val fingerprint = ElementFingerprint.generate(
                className = element.className,
                packageName = packageName,
                resourceId = element.resourceId,
                text = element.text,
                contentDesc = element.contentDescription
            )

            GeneratedCommand(
                phrase = "$actionType $label",
                alternates = listOf("press $label", "select $label", label),
                targetVuid = fingerprint,
                action = actionType,
                element = element,
                derivedLabel = label
            )
        }
    }

    private fun deriveActionType(element: ElementInfo): String {
        return when {
            element.isClickable && element.className.contains("Button") -> "tap"
            element.isClickable && element.className.contains("EditText") -> "focus"
            element.isClickable && element.className.contains("ImageView") -> "tap"
            element.isClickable && element.className.contains("CheckBox") -> "toggle"
            element.isClickable && element.className.contains("Switch") -> "toggle"
            element.isClickable -> "tap"
            element.isScrollable -> "scroll"
            else -> "interact"
        }
    }
}
