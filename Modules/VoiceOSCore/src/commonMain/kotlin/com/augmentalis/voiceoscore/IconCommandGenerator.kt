package com.augmentalis.voiceoscore

/**
 * Icon Command Generator - Creates voice commands for toolbar/navigation icons.
 *
 * Handles toolbar icons, action buttons, FABs, clickable ImageViews, and other
 * small clickable elements that have contentDescription but no visible text.
 * These get single-word commands like "Meet", "More", "Search".
 *
 * Recognizes icons by:
 * - Known navigation icon labels (localized via FilterFileLoader)
 * - Small size (< 150px)
 * - Class name patterns: ImageButton, IconButton, ActionButton, ImageView, FAB
 *
 * Icons without valid contentDescription get numbered commands for consistency.
 *
 * @since 2026-02-02
 * @see CommandGenerator for core command generation
 */
object IconCommandGenerator {

    // Maximum size for icon detection (pixels, roughly 100dp)
    private const val MAX_ICON_SIZE = 150

    /**
     * Check if an element is likely an icon button (small, clickable, has contentDescription).
     * Icon buttons should allow single-word commands.
     *
     * @param element The element to check
     * @param locale The locale for navigation icon matching (default: "en")
     * @return true if the element appears to be an icon
     */
    fun isIconElement(element: ElementInfo, locale: String = "en"): Boolean {
        // Must be clickable
        if (!element.isClickable) return false

        // Must have contentDescription but no visible text
        if (element.contentDescription.isBlank()) return false
        if (element.text.isNotBlank() && element.text != element.contentDescription) return false

        // Check if it's a recognized navigation icon (localized)
        val desc = element.contentDescription.lowercase().trim()
        val navIcons = FilterFileLoader.getNavigationIcons(locale)
        if (navIcons.contains(desc)) return true

        // Check size - icons are typically small (< 100dp on each side)
        val bounds = element.bounds
        if (bounds.width < MAX_ICON_SIZE && bounds.height < MAX_ICON_SIZE) return true

        // Check class name patterns (language-agnostic)
        // Includes ImageView for clickable images used as icon buttons
        val className = element.className.lowercase()
        if (className.contains("imagebutton") ||
            className.contains("iconbutton") ||
            className.contains("actionbutton") ||
            className.contains("imageview") ||
            className.contains("appcompatimageview") ||
            className.contains("fab")) return true

        return false
    }

    /**
     * Get the label for an icon element.
     * For icons, we use contentDescription directly without minimum word requirements.
     *
     * @param element The icon element
     * @param locale The locale for garbage filtering (default: "en")
     * @return The icon label or null if not suitable
     */
    fun getIconLabel(element: ElementInfo, locale: String = "en"): String? {
        if (!isIconElement(element, locale)) return null

        val label = element.contentDescription.trim()
        if (label.isBlank() || CommandGeneratorHelpers.isGarbageText(label, locale)) return null

        return CommandGeneratorHelpers.cleanLabel(label, locale)
    }

    /**
     * Generate commands for navigation/icon elements.
     * These are toolbar icons, action buttons, etc. that have contentDescription
     * but no visible text.
     *
     * Icons without valid contentDescription get numbered commands for consistency.
     *
     * @param elements All elements on screen
     * @param packageName Host application package name
     * @param locale The locale for icon detection (default: "en")
     * @return Pair of (labeled icon commands, numbered icon commands for unlabeled icons)
     */
    fun generateIconCommands(
        elements: List<ElementInfo>,
        packageName: String,
        locale: String = "en"
    ): Pair<List<QuantizedCommand>, List<QuantizedCommand>> {
        // Find icon elements
        val iconElements = elements.filter { isIconElement(it, locale) }

        val labeledCommands = mutableListOf<QuantizedCommand>()
        val unlabeledIcons = mutableListOf<ElementInfo>()

        iconElements.forEach { element ->
            val label = getIconLabel(element, locale)

            if (label != null) {
                // Create labeled command for this icon
                val avid = CommandGeneratorHelpers.generateAvid(element, packageName)
                labeledCommands.add(
                    QuantizedCommand(
                        avid = "",
                        phrase = label,
                        actionType = CommandActionType.CLICK,
                        targetAvid = avid,
                        confidence = 0.85f,
                        metadata = buildIconCommandMetadata(
                            element = element,
                            packageName = packageName,
                            isLabeled = true
                        )
                    )
                )
            } else {
                // Icon without valid label - will get numbered
                unlabeledIcons.add(element)
            }
        }

        // Generate numbered commands for unlabeled icons
        // Sort by position (left to right, top to bottom for toolbar consistency)
        val sortedUnlabeled = unlabeledIcons.sortedWith(
            compareBy({ it.bounds.top / 50 }, { it.bounds.left })  // Group by row
        )

        val numberedCommands = sortedUnlabeled.mapIndexed { index, element ->
            val number = index + 1
            val avid = CommandGeneratorHelpers.generateAvid(element, packageName)

            QuantizedCommand(
                avid = "",
                phrase = "icon $number",  // "icon 1", "icon 2" for unlabeled icons
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.7f,
                metadata = buildIconCommandMetadata(
                    element = element,
                    packageName = packageName,
                    isLabeled = false,
                    iconNumber = number
                )
            )
        }

        return Pair(labeledCommands, numberedCommands)
    }

    /**
     * Generate all icon commands (both labeled and numbered) as a single list.
     *
     * @param elements All elements on screen
     * @param packageName Host application package name
     * @param locale The locale for icon detection (default: "en")
     * @return Combined list of all icon commands
     */
    fun generateAllIconCommands(
        elements: List<ElementInfo>,
        packageName: String,
        locale: String = "en"
    ): List<QuantizedCommand> {
        val (labeled, numbered) = generateIconCommands(elements, packageName, locale)
        return labeled + numbered
    }

    // ===== Private Helper Functions =====

    /**
     * Build metadata map for icon commands with consistent structure.
     */
    private fun buildIconCommandMetadata(
        element: ElementInfo,
        packageName: String,
        isLabeled: Boolean,
        iconNumber: Int? = null
    ): Map<String, String> {
        val baseMetadata = mutableMapOf(
            "packageName" to packageName,
            "elementHash" to CommandGeneratorHelpers.deriveElementHash(element),
            "contentDescription" to element.contentDescription,
            "resourceId" to element.resourceId,
            "className" to element.className,
            "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}"
        )

        if (isLabeled) {
            baseMetadata["isIconCommand"] = "true"
        } else {
            baseMetadata["isUnlabeledIconCommand"] = "true"
            iconNumber?.let { baseMetadata["iconNumber"] = it.toString() }
        }

        return baseMetadata
    }
}
