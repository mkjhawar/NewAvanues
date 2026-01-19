package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.currentTimeMillis

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
     * Commands are stored WITHOUT verbs - the element label only (e.g., "4", "More options").
     * User provides the verb at runtime: "click 4", "tap More options", or just "4".
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @return QuantizedCommand or null if element is not suitable for voice command
     */
    /**
     * Result of command generation containing both the command and persistence flag.
     */
    data class GeneratedCommandResult(
        val command: QuantizedCommand,
        val shouldPersist: Boolean,
        val listIndex: Int = -1  // Position in list for index-based commands
    )

    fun fromElement(
        element: ElementInfo,
        packageName: String
    ): QuantizedCommand? {
        return fromElementWithPersistence(element, packageName)?.command
    }

    /**
     * Generate command from element with persistence information.
     * Returns both the command and whether it should be persisted to database.
     *
     * @param element Source element info
     * @param packageName Host application package name
     * @return GeneratedCommandResult or null if element is not suitable
     */
    fun fromElementWithPersistence(
        element: ElementInfo,
        packageName: String
    ): GeneratedCommandResult? {
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
        // Note: verb is NOT included in phrase - user provides it at runtime

        // Generate element hash for database FK reference
        val elementHash = deriveElementHash(element)
        val vuid = generateVuid(element, packageName)
        val currentTime = currentTimeMillis()

        // Determine if this is dynamic content (should NOT be persisted)
        val isDynamic = element.isDynamicContent

        val command = QuantizedCommand(
            avid = "", // Generated on persist if needed
            phrase = label,  // No verb - just the element label
            actionType = actionType,
            targetAvid = vuid,
            confidence = calculateConfidence(element),
            metadata = mapOf(
                "packageName" to packageName,
                "elementHash" to elementHash,
                "createdAt" to currentTime.toString(),
                "className" to element.className,
                "resourceId" to element.resourceId,
                "label" to label,
                "isDynamic" to isDynamic.toString(),
                "listIndex" to element.listIndex.toString(),
                "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}"
            )
        )

        return GeneratedCommandResult(
            command = command,
            shouldPersist = !isDynamic,
            listIndex = element.listIndex
        )
    }

    /**
     * Generate index-based commands for list items.
     * Creates commands like "first", "second", "item 3" for dynamic list navigation.
     *
     * IMPORTANT: Generates exactly ONE command per unique listIndex to prevent duplicates.
     * When multiple elements share the same listIndex (e.g., a row and its children),
     * we keep the best representative element (prefer clickable, in dynamic container).
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of index commands (in-memory only, never persisted)
     */
    fun generateListIndexCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val ordinals = listOf("first", "second", "third", "fourth", "fifth",
            "sixth", "seventh", "eighth", "ninth", "tenth")

        // Group by listIndex and keep only the best element per index
        // This prevents duplicates when multiple elements share the same listIndex
        val bestElementPerIndex = listItems
            .filter { it.listIndex >= 0 }
            .groupBy { it.listIndex }
            .mapValues { (_, elements) ->
                // Prefer: clickable > has content > in dynamic container > first
                elements.maxByOrNull { element ->
                    var score = 0
                    if (element.isClickable) score += 100
                    if (element.isInDynamicContainer) score += 50
                    if (element.text.isNotBlank() || element.contentDescription.isNotBlank()) score += 25
                    if (element.resourceId.isNotBlank()) score += 10
                    score
                }
            }
            .values
            .filterNotNull()

        return bestElementPerIndex.map { element ->
            val index = element.listIndex
            val phrase = when {
                index < ordinals.size -> ordinals[index]
                else -> "item ${index + 1}"
            }

            val vuid = generateVuid(element, packageName)

            QuantizedCommand(
                avid = "",
                phrase = phrase,
                actionType = CommandActionType.CLICK,
                targetAvid = vuid,
                confidence = 0.7f,
                metadata = mapOf(
                    "packageName" to packageName,
                    "elementHash" to deriveElementHash(element),
                    "isIndexCommand" to "true",
                    "listIndex" to index.toString(),
                    "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}"
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

        // Email pattern: "Unread, , , SenderName, , Subject..."
        if (text.startsWith("Unread,")) {
            val parts = text.split(",").map { it.trim() }
            // Find first non-empty part after "Unread"
            for (i in 1 until parts.size) {
                if (parts[i].isNotBlank() && parts[i].length in 2..30) {
                    return parts[i]
                }
            }
        }

        // First word if reasonably short
        val firstWord = text.split(Regex("[,\\s]+")).firstOrNull { it.length in 2..20 }
        return firstWord
    }

    /**
     * Generate label-based commands for list items.
     * Creates commands using the extracted label (sender name, title, etc.)
     * so users can say "Lifemiles" instead of just "first".
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

        return listItems.filter { it.listIndex >= 0 }.mapNotNull { element ->
            val label = extractShortLabel(element)

            // Skip if no label extracted or label already seen (avoid duplicates)
            if (label.isNullOrBlank()) return@mapNotNull null

            val normalizedLabel = label.lowercase().trim()
            if (normalizedLabel in seenLabels) return@mapNotNull null
            seenLabels.add(normalizedLabel)

            // Skip very short labels (likely noise) or very long ones
            if (label.length < 2 || label.length > 30) return@mapNotNull null

            val vuid = generateVuid(element, packageName)

            QuantizedCommand(
                avid = "",
                phrase = label,  // The sender name/title as command
                actionType = CommandActionType.CLICK,
                targetAvid = vuid,
                confidence = 0.8f,  // Higher confidence for label matches
                metadata = mapOf(
                    "packageName" to packageName,
                    "elementHash" to deriveElementHash(element),
                    "isLabelCommand" to "true",
                    "listIndex" to element.listIndex.toString(),
                    "originalLabel" to label,
                    "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}"
                )
            )
        }
    }

    /**
     * Derive a stable element hash for database FK reference.
     * Uses the same logic as generateVuid's elementHash for consistency.
     */
    private fun deriveElementHash(element: ElementInfo): String {
        val hashInput = when {
            element.resourceId.isNotBlank() -> element.resourceId
            element.contentDescription.isNotBlank() -> element.contentDescription
            element.text.isNotBlank() -> element.text
            else -> "${element.className}:${element.bounds}"
        }
        // Return a truncated hash suitable for database storage
        return hashInput.hashCode().toUInt().toString(16).padStart(8, '0')
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
     * Generate element fingerprint for targeting.
     * Format: {TypeCode}:{hash8} e.g., "BTN:a3f2e1c9"
     */
    private fun generateVuid(element: ElementInfo, packageName: String): String {
        return ElementFingerprint.fromElementInfo(element, packageName)
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
