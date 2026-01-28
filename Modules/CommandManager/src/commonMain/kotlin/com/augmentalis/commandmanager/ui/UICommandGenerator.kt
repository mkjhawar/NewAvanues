/**
 * UICommandGenerator.kt - Generate commands for UI display
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-20
 *
 * Generates DisplayCommand objects for UI display and exploration results.
 * These commands include full element references for rich presentation.
 *
 * For voice recognition commands, use CommandGenerator which produces
 * lightweight QuantizedCommand objects.
 */
package com.augmentalis.commandmanager

/**
 * Generates DisplayCommand objects for UI display.
 *
 * Single Responsibility: Generate command format for UI/exploration display.
 */
object UICommandGenerator {

    // Screen dimension limits for bounds validation
    private const val MAX_SCREEN_DIMENSION = 10000

    /**
     * Generate DisplayCommand objects for UI display.
     *
     * Includes bounds validation to ensure elements are within reasonable screen dimensions.
     *
     * @param elements All extracted UI elements
     * @param elementLabels Pre-derived labels for elements (index -> label)
     * @param packageName App package name
     * @return List of DisplayCommand objects for UI display
     */
    fun generate(
        elements: List<ElementInfo>,
        elementLabels: Map<Int, String>,
        packageName: String
    ): List<DisplayCommand> {
        return elements.mapIndexedNotNull { index, element ->
            if (!element.isClickable && !element.isScrollable) return@mapIndexedNotNull null

            // Validate element bounds are within reasonable screen dimensions
            if (!isValidBounds(element.bounds)) {
                return@mapIndexedNotNull null
            }

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

            DisplayCommand(
                phrase = "$actionType $label",
                alternates = listOf("press $label", "select $label", label),
                targetVuid = fingerprint,
                action = actionType,
                element = element,
                derivedLabel = label
            )
        }
    }

    /**
     * Validate that bounds are within reasonable screen dimensions.
     * Filters out elements with invalid or absurd bounds.
     */
    private fun isValidBounds(bounds: Bounds?): Boolean {
        if (bounds == null) return false

        // Check for negative coordinates (invalid)
        if (bounds.left < 0 || bounds.top < 0) return false

        // Check for inverted bounds (right < left or bottom < top)
        if (bounds.right < bounds.left || bounds.bottom < bounds.top) return false

        // Check for absurd dimensions (larger than any reasonable screen)
        if (bounds.right > MAX_SCREEN_DIMENSION || bounds.bottom > MAX_SCREEN_DIMENSION) return false

        // Check for zero-size elements (no area to interact with)
        val width = bounds.right - bounds.left
        val height = bounds.bottom - bounds.top
        if (width <= 0 || height <= 0) return false

        return true
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
