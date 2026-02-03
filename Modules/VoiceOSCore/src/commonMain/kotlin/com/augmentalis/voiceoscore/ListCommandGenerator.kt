package com.augmentalis.voiceoscore

/**
 * List Command Generator - Creates voice commands for dynamic list navigation.
 *
 * Generates commands for list items including:
 * - Index-based commands: "first", "second", "third"...
 * - Numeric commands: "1", "2", "3"... (matching overlay badge numbers)
 * - Label-based commands: sender names, titles for direct targeting
 *
 * All generated commands are in-memory only (never persisted to database)
 * as list content is inherently dynamic.
 *
 * @since 2026-02-02
 * @see CommandGenerator for core command generation
 */
object ListCommandGenerator {

    private val ORDINALS = listOf(
        "first", "second", "third", "fourth", "fifth",
        "sixth", "seventh", "eighth", "ninth", "tenth"
    )

    /**
     * Generate index-based commands for list items.
     * Creates commands like "first", "second", "item 3" for dynamic list navigation.
     *
     * IMPORTANT: Generates exactly ONE command per unique listIndex to prevent duplicates.
     * When multiple elements share the same listIndex (e.g., a row and its children),
     * we keep the best representative element (prefer clickable, in dynamic container).
     *
     * Only generates commands for ACTIONABLE elements. Elements that are just
     * labels or text views without click handlers are skipped.
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of index commands (in-memory only, never persisted)
     */
    fun generateListIndexCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        // Group by listIndex and keep only the best ACTIONABLE element per index
        val bestElementPerIndex = listItems
            .filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }
            .groupBy { it.listIndex }
            .mapValues { (_, elements) -> selectBestElement(elements) }
            .values
            .filterNotNull()

        return bestElementPerIndex.map { element ->
            val index = element.listIndex
            val phrase = when {
                index < ORDINALS.size -> ORDINALS[index]
                else -> "item ${index + 1}"
            }

            val avid = CommandGeneratorHelpers.generateAvid(element, packageName)
            val label = deriveLabel(element)

            QuantizedCommand(
                avid = "",
                phrase = phrase,
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.7f,
                metadata = buildListCommandMetadata(
                    element = element,
                    packageName = packageName,
                    commandType = "isIndexCommand",
                    label = label,
                    extraMetadata = mapOf("listIndex" to index.toString())
                )
            )
        }
    }

    /**
     * Generate numeric commands for list items.
     * Creates commands with raw numbers "1", "2", "3"... so users can simply say
     * the number shown in the overlay badge to click that item.
     *
     * This is SEPARATE from generateListIndexCommands() which creates
     * ordinal words ("first", "second"). Numeric commands match overlay badges.
     *
     * Only generates commands for ACTIONABLE elements.
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of numeric commands (in-memory only, never persisted)
     */
    fun generateNumericCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        // Group by listIndex and keep only the best ACTIONABLE element per index
        val bestElementPerIndex = listItems
            .filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }
            .groupBy { it.listIndex }
            .mapValues { (_, elements) -> selectBestElement(elements) }
            .values
            .filterNotNull()
            .sortedBy { it.listIndex }

        return bestElementPerIndex.mapIndexed { visualIndex, element ->
            // Visual index is 1-based (matches overlay badge numbers)
            val number = visualIndex + 1
            val avid = CommandGeneratorHelpers.generateAvid(element, packageName)
            val label = deriveLabel(element)

            QuantizedCommand(
                avid = "",
                phrase = number.toString(),  // Raw number: "1", "2", "3"
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.9f,  // High confidence for numeric commands
                metadata = buildListCommandMetadata(
                    element = element,
                    packageName = packageName,
                    commandType = "isNumericCommand",
                    label = label,
                    extraMetadata = mapOf(
                        "numericIndex" to number.toString(),
                        "listIndex" to element.listIndex.toString()
                    )
                )
            )
        }
    }

    /**
     * Generate label-based commands for list items.
     * Creates commands using the extracted label (sender name, title, etc.)
     * so users can say "Lifemiles" instead of just "first".
     *
     * Only generates commands for ACTIONABLE elements.
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of label commands (in-memory only, never persisted)
     */
    fun generateListLabelCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val seenLabels = mutableSetOf<String>()

        return listItems
            .filter { it.listIndex >= 0 && (it.isClickable || it.isLongClickable) }
            .mapNotNull { element ->
                val label = extractShortLabel(element)

                // Skip if no label extracted or label already seen (avoid duplicates)
                if (label.isNullOrBlank()) return@mapNotNull null

                val normalizedLabel = label.lowercase().trim()
                if (normalizedLabel in seenLabels) return@mapNotNull null
                seenLabels.add(normalizedLabel)

                // Skip very short labels (likely noise) or very long ones
                if (label.length < 2 || label.length > 30) return@mapNotNull null

                val avid = CommandGeneratorHelpers.generateAvid(element, packageName)

                QuantizedCommand(
                    avid = "",
                    phrase = label,  // The sender name/title as command
                    actionType = CommandActionType.CLICK,
                    targetAvid = avid,
                    confidence = 0.8f,  // Higher confidence for label matches
                    metadata = buildListCommandMetadata(
                        element = element,
                        packageName = packageName,
                        commandType = "isLabelCommand",
                        label = label,
                        extraMetadata = mapOf(
                            "listIndex" to element.listIndex.toString(),
                            "originalLabel" to label
                        )
                    )
                )
            }
    }

    /**
     * Extract short sender/title from dynamic content for voice targeting.
     * E.g., "Unread, , , Arby's, , BOGO..." → "Arby's"
     *
     * @param element Element with dynamic content
     * @return Short label suitable for voice command, or null if not extractable
     */
    fun extractShortLabel(element: ElementInfo): String? {
        val text = element.text.ifBlank { element.contentDescription }
        if (text.isBlank()) return null

        // Early garbage check
        if (CommandGeneratorHelpers.isGarbageText(text)) return null

        // Email pattern: "Unread, , , SenderName, , Subject..."
        if (text.startsWith("Unread,")) {
            val parts = text.split(",").map { it.trim() }
            // Find first non-empty part after "Unread"
            for (i in 1 until parts.size) {
                if (parts[i].isNotBlank() && parts[i].length in 2..30 &&
                    !CommandGeneratorHelpers.isGarbageText(parts[i])) {
                    return CommandGeneratorHelpers.cleanLabel(parts[i])
                }
            }
        }

        val normalized = CommandGeneratorHelpers.normalizeRealWearMlScript(text)

        // Check garbage after normalization
        if (CommandGeneratorHelpers.isGarbageText(normalized)) return null

        val result = if (SymbolNormalizer.containsSymbols(normalized)) {
            SymbolNormalizer.normalize(normalized)
        } else {
            normalized
        }

        return CommandGeneratorHelpers.cleanLabel(result)
    }

    // ===== Private Helper Functions =====

    /**
     * Select the best element from a group sharing the same listIndex.
     * Prefers: clickable > has content > in dynamic container
     */
    private fun selectBestElement(elements: List<ElementInfo>): ElementInfo? {
        return elements.maxByOrNull { element ->
            var score = 0
            if (element.isClickable) score += 100
            if (element.isInDynamicContainer) score += 50
            if (element.text.isNotBlank() || element.contentDescription.isNotBlank()) score += 25
            if (element.resourceId.isNotBlank()) score += 10
            score
        }
    }

    /**
     * Derive label for fallback search - prioritize text, then contentDescription.
     */
    private fun deriveLabel(element: ElementInfo): String {
        return when {
            element.text.isNotBlank() -> element.text
            element.contentDescription.isNotBlank() -> element.contentDescription
            else -> ""
        }
    }

    /**
     * Build metadata map for list commands with consistent structure.
     */
    private fun buildListCommandMetadata(
        element: ElementInfo,
        packageName: String,
        commandType: String,
        label: String,
        extraMetadata: Map<String, String> = emptyMap()
    ): Map<String, String> {
        val baseMetadata = mapOf(
            "packageName" to packageName,
            "elementHash" to CommandGeneratorHelpers.deriveElementHash(element),
            commandType to "true",
            "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
            "label" to label,
            "contentDescription" to element.contentDescription,
            "resourceId" to element.resourceId,
            "className" to element.className,
            // NAV-500 Fix #2: Scroll tracking metadata
            "containerId" to element.containerResourceId,
            "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
        )
        return baseMetadata + extraMetadata
    }
}
