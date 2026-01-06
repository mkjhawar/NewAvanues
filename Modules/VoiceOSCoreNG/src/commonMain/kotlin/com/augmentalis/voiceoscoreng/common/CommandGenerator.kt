package com.augmentalis.voiceoscoreng.common

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import com.augmentalis.voiceoscoreng.common.VUIDTypeCode

/**
 * Command Generator - Creates voice commands from UI elements.
 *
 * Generates QuantizedCommand objects from ElementInfo during element extraction.
 * Designed for single-pass generation with minimal overhead.
 */
object CommandGenerator {

    /**
     * Generate command from element during extraction (single pass).
     * Returns null if element is not actionable or has no useful label.
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @return QuantizedCommand or null if element is not suitable for voice command
     */
    fun fromElement(
        element: ElementInfo,
        packageName: String
    ): QuantizedCommand? {
        // Skip non-actionable elements
        if (!element.isActionable) return null

        // Skip elements without voice content
        if (!element.hasVoiceContent) return null

        // Get label and skip if it's just the class name
        val label = deriveLabel(element)
        if (label.isBlank() || label == element.className.substringAfterLast(".")) {
            return null
        }

        val actionType = deriveActionType(element)
        val verb = actionType.verb()
        val vuid = generateVuid(element, packageName)

        return QuantizedCommand(
            uuid = "", // Generated on persist if needed
            phrase = "$verb $label",
            actionType = actionType,
            targetVuid = vuid,
            confidence = calculateConfidence(element)
        )
    }

    /**
     * Derive the best label for voice recognition from element properties.
     */
    private fun deriveLabel(element: ElementInfo): String {
        return when {
            element.text.isNotBlank() -> element.text
            element.contentDescription.isNotBlank() -> element.contentDescription
            element.resourceId.isNotBlank() -> {
                element.resourceId
                    .substringAfterLast("/")
                    .replace("_", " ")
                    .replace("-", " ")
            }
            else -> ""
        }
    }

    /**
     * Derive action type based on element properties.
     */
    private fun deriveActionType(element: ElementInfo): CommandActionType {
        val className = element.className.lowercase()
        return when {
            className.contains("edittext") || className.contains("textfield") -> CommandActionType.TYPE
            className.contains("checkbox") || className.contains("switch") -> CommandActionType.CLICK
            className.contains("button") -> CommandActionType.CLICK
            element.isScrollable -> CommandActionType.CLICK // Keep as click for scrollable items
            element.isClickable -> CommandActionType.CLICK
            else -> CommandActionType.CLICK
        }
    }

    /**
     * Generate VUID for element.
     */
    private fun generateVuid(element: ElementInfo, packageName: String): String {
        val typeCode = VUIDGenerator.getTypeCode(element.className)

        // Create element hash from most stable identifier
        val elementHash = when {
            element.resourceId.isNotBlank() -> element.resourceId
            element.contentDescription.isNotBlank() -> element.contentDescription
            element.text.isNotBlank() -> element.text
            else -> "${element.className}:${element.bounds}"
        }

        return VUIDGenerator.generate(
            packageName = packageName,
            typeCode = typeCode,
            elementHash = elementHash
        )
    }

    /**
     * Calculate confidence score based on element identifiers.
     * Higher confidence for elements with more identifying information.
     */
    private fun calculateConfidence(element: ElementInfo): Float {
        var confidence = 0.5f

        // Boost for having resourceId (most reliable)
        if (element.resourceId.isNotBlank()) {
            confidence += 0.2f
        }

        // Boost for content description (accessibility info)
        if (element.contentDescription.isNotBlank()) {
            confidence += 0.15f
        }

        // Boost for reasonable text length (not too short, not too long)
        val label = deriveLabel(element)
        if (label.length in 2..20) {
            confidence += 0.1f
        }

        // Small boost for being clickable
        if (element.isClickable) {
            confidence += 0.05f
        }

        return confidence.coerceIn(0f, 1f)
    }

    /**
     * Extension function to get verb for action type.
     */
    private fun CommandActionType.verb(): String = when (this) {
        CommandActionType.CLICK -> "click"
        CommandActionType.TAP -> "tap"
        CommandActionType.LONG_CLICK -> "hold"
        CommandActionType.EXECUTE -> "execute"
        CommandActionType.TYPE -> "type"
        CommandActionType.FOCUS -> "focus"
        CommandActionType.SCROLL_DOWN -> "scroll down"
        CommandActionType.SCROLL_UP -> "scroll up"
        CommandActionType.SCROLL_LEFT -> "scroll left"
        CommandActionType.SCROLL_RIGHT -> "scroll right"
        CommandActionType.SCROLL -> "scroll"
        CommandActionType.NAVIGATE -> "go to"
        CommandActionType.CUSTOM -> "activate"
        else -> "activate"
    }
}
