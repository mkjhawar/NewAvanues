package com.augmentalis.voiceoscore

/**
 * Command Generator - Creates voice commands from UI elements.
 *
 * Generates QuantizedCommand objects from ElementInfo during element extraction.
 * Designed for single-pass generation with minimal overhead.
 *
 * This is the main entry point for command generation. It delegates to specialized
 * generators for specific command types:
 * - [ListCommandGenerator] for list/dynamic navigation commands
 * - [IconCommandGenerator] for toolbar/navigation icon commands
 * - [CommandGeneratorHelpers] for shared utility functions
 *
 * @since 2026-01-16
 * @see ListCommandGenerator
 * @see IconCommandGenerator
 * @see CommandGeneratorHelpers
 */
object CommandGenerator {

    /**
     * Result of command generation containing both the command and persistence flag.
     */
    data class GeneratedCommandResult(
        val command: QuantizedCommand,
        val shouldPersist: Boolean,
        val listIndex: Int = -1  // Position in list for index-based commands
    )

    // ===== Core Command Generation =====

    /**
     * Generate command from element during extraction (single pass).
     * Returns null if element is not actionable or has no useful label.
     *
     * Commands are stored WITHOUT verbs - the element label only (e.g., "4", "More options").
     * User provides the verb at runtime: "click 4", "tap More options", or just "4".
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @return QuantizedCommand or null if element is not suitable for voice command
     */
    fun fromElement(
        element: ElementInfo,
        packageName: String
    ): QuantizedCommand? {
        @Suppress("DEPRECATION")
        return fromElementWithPersistence(element, packageName)?.command
    }

    /**
     * Generate command from element with full persistence decision using 4-layer system.
     *
     * This is the preferred method that uses PersistenceDecisionEngine to make intelligent
     * persistence decisions based on app category, container behavior, screen type, and content analysis.
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @param allElements All elements on screen (for ScreenClassifier)
     * @return GeneratedCommandResult with persistence decision from PersistenceDecisionEngine
     */
    fun fromElementWithPersistence(
        element: ElementInfo,
        packageName: String,
        allElements: List<ElementInfo>
    ): GeneratedCommandResult? {
        // Skip non-actionable elements
        if (!element.isActionable) return null
        if (!element.hasVoiceContent) return null

        val label = CommandGeneratorHelpers.deriveLabel(element)
        if (label.isBlank() || label == element.className.substringAfterLast(".")) {
            return null
        }

        // Use PersistenceDecisionEngine for 4-layer decision
        val decision = PersistenceDecisionEngine.decideForElement(element, packageName, allElements)

        val actionType = CommandGeneratorHelpers.deriveActionType(element)
        val elementHash = CommandGeneratorHelpers.deriveElementHash(element)
        val avid = CommandGeneratorHelpers.generateAvid(element, packageName)
        val currentTime = currentTimeMillis()

        val command = QuantizedCommand(
            avid = "",
            phrase = label,
            actionType = actionType,
            targetAvid = avid,
            confidence = decision.confidence,
            metadata = mapOf(
                "packageName" to packageName,
                "elementHash" to elementHash,
                "createdAt" to currentTime.toString(),
                "className" to element.className,
                "resourceId" to element.resourceId,
                "label" to label,
                "isDynamic" to (!decision.shouldPersist).toString(),
                "persistenceRule" to decision.ruleApplied.toString(),
                "persistenceReason" to decision.reason,
                "listIndex" to element.listIndex.toString(),
                "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                "containerId" to element.containerResourceId,
                "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
            )
        )

        return GeneratedCommandResult(
            command = command,
            shouldPersist = decision.shouldPersist,
            listIndex = element.listIndex
        )
    }

    /**
     * Generate command from element with persistence information using legacy detection.
     * Returns both the command and whether it should be persisted to database.
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @return GeneratedCommandResult or null if element is not suitable
     */
    @Deprecated(
        message = "Use fromElementWithPersistence(element, packageName, allElements) for 4-layer persistence decision",
        replaceWith = ReplaceWith("fromElementWithPersistence(element, packageName, allElements)")
    )
    fun fromElementWithPersistence(
        element: ElementInfo,
        packageName: String
    ): GeneratedCommandResult? {
        // Skip non-actionable elements
        if (!element.isActionable) return null
        if (!element.hasVoiceContent) return null

        val label = CommandGeneratorHelpers.deriveLabel(element)
        if (label.isBlank() || label == element.className.substringAfterLast(".")) {
            return null
        }

        val actionType = CommandGeneratorHelpers.deriveActionType(element)
        val elementHash = CommandGeneratorHelpers.deriveElementHash(element)
        val avid = CommandGeneratorHelpers.generateAvid(element, packageName)
        val currentTime = currentTimeMillis()
        val isDynamic = element.isDynamicContent

        val command = QuantizedCommand(
            avid = "",
            phrase = label,
            actionType = actionType,
            targetAvid = avid,
            confidence = CommandGeneratorHelpers.calculateConfidence(element),
            metadata = mapOf(
                "packageName" to packageName,
                "elementHash" to elementHash,
                "createdAt" to currentTime.toString(),
                "className" to element.className,
                "resourceId" to element.resourceId,
                "label" to label,
                "isDynamic" to isDynamic.toString(),
                "listIndex" to element.listIndex.toString(),
                "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                "containerId" to element.containerResourceId,
                "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
            )
        )

        return GeneratedCommandResult(
            command = command,
            shouldPersist = !isDynamic,
            listIndex = element.listIndex
        )
    }

    // ===== Delegated List Command Generation =====

    /**
     * Generate index-based commands for list items.
     * Creates commands like "first", "second", "item 3" for dynamic list navigation.
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of index commands (in-memory only, never persisted)
     * @see ListCommandGenerator.generateListIndexCommands
     */
    fun generateListIndexCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> = ListCommandGenerator.generateListIndexCommands(listItems, packageName)

    /**
     * Generate numeric commands for list items.
     * Creates commands with raw numbers "1", "2", "3"...
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of numeric commands (in-memory only, never persisted)
     * @see ListCommandGenerator.generateNumericCommands
     */
    fun generateNumericCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> = ListCommandGenerator.generateNumericCommands(listItems, packageName)

    /**
     * Generate label-based commands for list items.
     * Creates commands using the extracted label (sender name, title, etc.).
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of label commands (in-memory only, never persisted)
     * @see ListCommandGenerator.generateListLabelCommands
     */
    fun generateListLabelCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> = ListCommandGenerator.generateListLabelCommands(listItems, packageName)

    /**
     * Extract short sender/title from dynamic content for voice targeting.
     *
     * @param element Element with dynamic content
     * @return Short label suitable for voice command, or null if not extractable
     * @see ListCommandGenerator.extractShortLabel
     */
    fun extractShortLabel(element: ElementInfo): String? = ListCommandGenerator.extractShortLabel(element)

    // ===== Delegated Icon Command Generation =====

    /**
     * Check if an element is likely an icon button.
     *
     * @param element The element to check
     * @param locale The locale for navigation icon matching (default: "en")
     * @return true if the element appears to be an icon
     * @see IconCommandGenerator.isIconElement
     */
    fun isIconElement(element: ElementInfo, locale: String = "en"): Boolean =
        IconCommandGenerator.isIconElement(element, locale)

    /**
     * Get the label for an icon element.
     *
     * @param element The icon element
     * @return The icon label or null if not suitable
     * @see IconCommandGenerator.getIconLabel
     */
    fun getIconLabel(element: ElementInfo): String? = IconCommandGenerator.getIconLabel(element)

    /**
     * Generate commands for navigation/icon elements.
     *
     * @param elements All elements on screen
     * @param packageName Host application package name
     * @return Pair of (labeled icon commands, numbered icon commands for unlabeled icons)
     * @see IconCommandGenerator.generateIconCommands
     */
    fun generateIconCommands(
        elements: List<ElementInfo>,
        packageName: String
    ): Pair<List<QuantizedCommand>, List<QuantizedCommand>> =
        IconCommandGenerator.generateIconCommands(elements, packageName)

    // ===== Delegated Garbage Filtering =====

    /**
     * Check if text is garbage that should not be a voice command.
     *
     * @param text The text to check
     * @param locale The locale for language-specific garbage detection (default: "en")
     * @return true if the text is garbage and should be filtered out
     * @see CommandGeneratorHelpers.isGarbageText
     */
    fun isGarbageText(text: String, locale: String = "en"): Boolean =
        CommandGeneratorHelpers.isGarbageText(text, locale)

    /**
     * Clean and validate label text for voice commands.
     *
     * @param text Raw label text
     * @param locale The locale for garbage detection (default: "en")
     * @return Cleaned text or null if garbage
     * @see CommandGeneratorHelpers.cleanLabel
     */
    fun cleanLabel(text: String, locale: String = "en"): String? =
        CommandGeneratorHelpers.cleanLabel(text, locale)
}
