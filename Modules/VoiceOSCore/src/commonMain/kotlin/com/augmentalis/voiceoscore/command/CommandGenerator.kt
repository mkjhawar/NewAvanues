package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.ElementInfo
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.currentTimeMillis
import com.augmentalis.foundation.util.NumberToWords

/**
 * Command Generator - Creates voice commands from UI elements.
 *
 * Generates QuantizedCommand objects from ElementInfo during element extraction.
 * Designed for single-pass generation with minimal overhead.
 */
object CommandGenerator {

    private val PARSE_DESCRIPTION_DELIMITERS = listOf(":", "|", ",", ".")

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

    @Suppress("DEPRECATION")
    fun fromElement(
        element: ElementInfo,
        packageName: String
    ): QuantizedCommand? {
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

        val label = deriveLabel(element)
        if (label.isBlank() || label == element.className.substringAfterLast(".")) {
            return null
        }

        // Use PersistenceDecisionEngine for 4-layer decision
        val decision = PersistenceDecisionEngine.decideForElement(element, packageName, allElements)

        val actionType = deriveActionType(element)
        val elementHash = deriveElementHash(element)
        val avid = generateAvid(element, packageName)
        val currentTime = currentTimeMillis()

        val command = QuantizedCommand(
            avid = "",
            phrase = label,
            actionType = actionType,
            targetAvid = avid,
            confidence = decision.confidence,  // Use decision confidence
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
                // NAV-500 Fix #2: Scroll tracking metadata for BoundsResolver
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
        val avid = generateAvid(element, packageName)
        val currentTime = currentTimeMillis()

        // Determine if this is dynamic content (should NOT be persisted)
        @Suppress("DEPRECATION")
        val isDynamic = element.isDynamicContent

        val command = QuantizedCommand(
            avid = "", // Generated on persist if needed
            phrase = label,  // No verb - just the element label
            actionType = actionType,
            targetAvid = avid,
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
                "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                // NAV-500 Fix #2: Scroll tracking metadata for BoundsResolver
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

    /**
     * Generate ordinal commands for screen elements.
     * Creates commands like "first", "second", "item 3" for voice navigation.
     *
     * Dual-path strategy:
     * - **Target apps** (elements with listIndex >= 0): Groups by listIndex, keeps best
     *   representative per group, sorts by listIndex. Prevents duplicates from child elements.
     * - **Non-target apps** (all listIndex = -1): Sorts all clickable elements spatially
     *   (top-to-bottom, left-to-right) and assigns ordinals by visual position.
     *
     * Both paths use visualIndex (position in sorted order) for ordinal assignment,
     * matching the overlay badge numbering.
     *
     * @param allElements All screen elements (not pre-filtered by listIndex)
     * @param packageName Host application package name
     * @return List of ordinal commands (in-memory only, never persisted)
     */
    fun generateListIndexCommands(
        allElements: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        val ordinals = listOf(
            "first", "second", "third", "fourth", "fifth",
            "sixth", "seventh", "eighth", "ninth", "tenth"
        )

        val hasTargetAppIndices = allElements.any { it.listIndex >= 0 }

        val candidateElements = if (hasTargetAppIndices) {
            // Target app path: group by listIndex, keep best per group
            allElements
                .filter { it.listIndex >= 0 }
                .groupBy { it.listIndex }
                .mapValues { (_, elements) ->
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
                .sortedBy { it.listIndex }
        } else {
            // Non-target app path: spatial ordering for all clickable elements
            allElements
                .filter { it.isClickable || it.isLongClickable }
                .sortedWith(compareBy({ it.bounds.top }, { it.bounds.left }))
        }

        return candidateElements.take(ordinals.size).mapIndexed { visualIndex, element ->
            val phrase = ordinals[visualIndex]
            val avid = generateAvid(element, packageName)

            val label = when {
                element.text.isNotBlank() -> element.text
                element.contentDescription.isNotBlank() -> element.contentDescription
                else -> ""
            }

            QuantizedCommand(
                avid = "",
                phrase = phrase,
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.7f,
                metadata = mapOf(
                    "packageName" to packageName,
                    "elementHash" to deriveElementHash(element),
                    "isIndexCommand" to "true",
                    "visualIndex" to visualIndex.toString(),
                    "listIndex" to element.listIndex.toString(),
                    "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                    "label" to label,
                    "contentDescription" to element.contentDescription,
                    "resourceId" to element.resourceId,
                    "className" to element.className,
                    "containerId" to element.containerResourceId,
                    "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
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
        } else if (text.startsWith(", , ,")) {// Email pattern: ", , , SenderName, , Subject..."
            val parts = text.split(",").map { it.trim() }
            // Find first non-empty part after "Unread"
            for (i in 0 until parts.size) {
                if (parts[i].isNotBlank() && parts[i].length in 2..30) {
                    return parts[i]
                }
            }
        }

        val normalizeRealWearMlScript = normalizeRealWearMlScript(text)
        return if (SymbolNormalizer.containsSymbols(normalizeRealWearMlScript)) {
            SymbolNormalizer.normalize(normalizeRealWearMlScript)
        } else {
            normalizeRealWearMlScript
        }
    }

    /**
     * Generate numeric commands for list items.
     * Creates commands with BOTH digit ("1") and word ("one") forms so users can say
     * either the number shown in the overlay badge or the spoken word to click that item.
     *
     * Examples:
     *   Item 1: registers "1" and "one"
     *   Item 21: registers "21" and "twenty one"
     *   Item 100: registers "100" and "one hundred"
     *
     * @param listItems Elements that are list items (have listIndex >= 0)
     * @param packageName Host application package name
     * @return List of numeric commands (in-memory only, never persisted)
     */
    fun generateNumericCommands(
        listItems: List<ElementInfo>,
        packageName: String
    ): List<QuantizedCommand> {
        // Group by listIndex and keep only the best element per index
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
            .sortedBy { it.listIndex }

        return bestElementPerIndex.flatMapIndexed { visualIndex, element ->
            // Visual index is 1-based (matches overlay badge numbers)
            val number = visualIndex + 1
            val avid = generateAvid(element, packageName)

            // Derive label for fallback search
            val label = when {
                element.text.isNotBlank() -> element.text
                element.contentDescription.isNotBlank() -> element.contentDescription
                else -> ""
            }

            // Generate both digit and word forms
            val digitPhrase = number.toString()  // "1", "21", "100"
            val wordPhrase = NumberToWords.convert(number)  // "one", "twenty one", "one hundred"

            val baseMetadata = mapOf(
                "packageName" to packageName,
                "elementHash" to deriveElementHash(element),
                "isNumericCommand" to "true",
                "numericIndex" to number.toString(),
                "digitForm" to digitPhrase,
                "wordForm" to wordPhrase,
                "listIndex" to element.listIndex.toString(),
                "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                "label" to label,
                "contentDescription" to element.contentDescription,
                "resourceId" to element.resourceId,
                "className" to element.className,
                "containerId" to element.containerResourceId,
                "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
            )

            // Create commands for both forms
            listOf(digitPhrase, wordPhrase).map { phrase ->
                QuantizedCommand(
                    avid = "",
                    phrase = phrase,
                    actionType = CommandActionType.CLICK,
                    targetAvid = avid,
                    confidence = 0.9f,  // High confidence for numeric commands
                    metadata = baseMetadata
                )
            }
        }
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
            println("[generateListLabelCommands] label = $label")

            // Skip if no label extracted or label already seen (avoid duplicates)
            if (label.isNullOrBlank() || !element.canPerformAction()) return@mapNotNull null
            println("[generateListLabelCommands] label = $label")

            val normalizedLabel = label.lowercase().trim()
            if (normalizedLabel in seenLabels) return@mapNotNull null
            seenLabels.add(normalizedLabel)

            // Skip very short labels (likely noise) or very long ones
            if (label.length < 2 || label.length > 30) return@mapNotNull null

            val avid = generateAvid(element, packageName)

            QuantizedCommand(
                avid = "",
                phrase = label,  // The sender name/title as command
                actionType = CommandActionType.CLICK,
                targetAvid = avid,
                confidence = 0.8f,  // Higher confidence for label matches
                metadata = mapOf(
                    "packageName" to packageName,
                    "elementHash" to deriveElementHash(element),
                    "isLabelCommand" to "true",
                    "listIndex" to element.listIndex.toString(),
                    "originalLabel" to label,
                    "bounds" to "${element.bounds.left},${element.bounds.top},${element.bounds.right},${element.bounds.bottom}",
                    // NAV-500 Fix #2: Scroll tracking metadata
                    "containerId" to element.containerResourceId,
                    "scrollOffset" to "${element.scrollOffsetX},${element.scrollOffsetY}"
                )
            )
        }
    }

    /**
     * Derive a stable element hash for database FK reference.
     * Uses the same logic as generateAvid's elementHash for consistency.
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
     * Normalizes special characters to speech-friendly equivalents.
     *
     * @param element Source element
     * @param locale Locale for symbol normalization (default: "en")
     * @return Normalized label suitable for voice recognition
     */
    private fun deriveLabel(element: ElementInfo, locale: String = "en"): String {
        val rawLabel = when {
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

        val normalizeRealWearMlScript = normalizeRealWearMlScript(rawLabel)
        // Normalize special characters (e.g., "&" → "and", "#" → "pound")
        return if (SymbolNormalizer.containsSymbols(normalizeRealWearMlScript)) {
            SymbolNormalizer.normalize(normalizeRealWearMlScript, locale)
        } else {
            normalizeRealWearMlScript
        }
    }

    /**
     * Two-layer label normalizer for voice command derivation.
     *
     * **Layer 1: Universal Voice Hint Convention**
     * Checks for `(Voice: phrase)` pattern in the text. This is a framework-agnostic convention:
     * any Android app (Compose, Flutter, React Native, Unity) can embed explicit voice phrases
     * in contentDescription. Example: `"Back (Voice: go back)"` → returns `"go back"`.
     *
     * **Layer 2: Delimiter-Based Parsing (RealWear ML Script)**
     * Falls back to splitting on the first found delimiter from [PARSE_DESCRIPTION_DELIMITERS].
     * - If `"hf_"` is present → returns the part after the delimiter
     * - Otherwise → returns the part before the delimiter
     *
     * If neither layer matches, the input is returned unchanged.
     *
     * @param text Raw accessibility label or ML script text to normalize.
     * @return Normalized label suitable for voice command generation.
     */
    /**
     * Regex to extract explicit voice phrase from the universal `(Voice: ...)` convention.
     * Any Android app framework (Compose, Flutter, React Native, Unity) can embed
     * `(Voice: phrase)` in contentDescription to provide explicit voice command mapping.
     * Example: "Back (Voice: go back)" → extracts "go back"
     */
    private val VOICE_HINT_PATTERN = Regex("\\(Voice:\\s*(.+?)\\)\\s*$")

    private fun normalizeRealWearMlScript(text: String): String {
        // Layer 1: Check for explicit (Voice: ...) hint — universal convention for any framework
        val voiceMatch = VOICE_HINT_PATTERN.find(text)
        if (voiceMatch != null) {
            return voiceMatch.groupValues[1].trim()
        }

        // Layer 2: Delimiter-based parsing for RealWear ML scripts and general labels
        val foundDelimiter = PARSE_DESCRIPTION_DELIMITERS.firstOrNull { text.contains(it) }

        if (foundDelimiter != null) {
            val parts = text.split(foundDelimiter, limit = 2)

            val candidate = if ("hf_" in text) {
                parts[1]
            } else {
                parts[0]
            }

            // Guard: don't return broken labels from aggressive splitting.
            // If the candidate is too short (< 2 chars, e.g. time "3:45" → "3"),
            // return the original text instead. This protects third-party app labels
            // that naturally contain delimiters (time formats, compound labels).
            val trimmed = candidate.trim()
            return if (trimmed.length >= 2) trimmed else text
        }
        return text
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
    private fun generateAvid(element: ElementInfo, packageName: String): String {
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
