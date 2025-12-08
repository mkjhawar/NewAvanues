/**
 * CommandGenerator.kt - Voice command generation from learned UI elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/generation/CommandGenerator.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 *
 * Generates voice commands from learned UI elements with semantic analysis
 */

package com.augmentalis.learnapp.generation

import com.augmentalis.learnapp.models.ElementInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Command Generator
 *
 * Generates voice commands from learned UI elements.
 * Uses semantic analysis to create meaningful, natural-language commands.
 * Supports synonym generation and conflict resolution.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val generator = CommandGenerator()
 *
 * // Generate commands from element
 * val commands = generator.generateCommands(elementInfo)
 * // Result: ["tap like button", "click like", "like", "press like button"]
 *
 * // Check for conflicts
 * val hasConflict = generator.hasConflict("like")
 *
 * // Resolve conflicts
 * val resolved = generator.resolveConflict("like", elementInfo)
 * // Result: "like button in feed"
 *
 * // Validate command
 * val isValid = generator.validateCommand("tap like button")
 * ```
 *
 * @since 1.0.0
 */
class CommandGenerator {

    /**
     * Generated commands registry
     * Maps command phrase -> element UUID
     */
    private val _commandRegistry = MutableStateFlow<Map<String, String>>(emptyMap())
    val commandRegistry: StateFlow<Map<String, String>> = _commandRegistry.asStateFlow()

    /**
     * Command conflicts
     * Maps ambiguous command -> list of element UUIDs
     */
    private val _commandConflicts = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val commandConflicts: StateFlow<Map<String, List<String>>> = _commandConflicts.asStateFlow()

    /**
     * Stop words (common words to filter out)
     */
    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "from", "by", "as", "is", "was", "are", "were", "be",
        "been", "being", "have", "has", "had", "do", "does", "did", "will",
        "would", "should", "could", "may", "might", "must", "can"
    )

    /**
     * Action verbs for command generation
     */
    private val actionVerbs = listOf(
        "tap", "click", "press", "select", "open", "activate", "touch"
    )

    /**
     * Generate voice commands from UI element
     *
     * Creates multiple command variations using semantic analysis.
     *
     * @param element Element to generate commands for
     * @return List of generated commands
     */
    fun generateCommands(element: ElementInfo): List<GeneratedCommand> {
        val commands = mutableListOf<GeneratedCommand>()

        // Extract meaningful text
        val meaningfulText = extractMeaningfulText(element)
        if (meaningfulText.isBlank()) {
            return emptyList()
        }

        // 1. Generate primary command (most natural)
        val primaryCommand = generatePrimaryCommand(element, meaningfulText)
        commands.add(primaryCommand)

        // 2. Generate synonyms (alternative phrasings)
        val synonyms = generateSynonyms(element, meaningfulText)
        commands.addAll(synonyms)

        // 3. Generate short forms (abbreviated versions)
        val shortForms = generateShortForms(meaningfulText)
        commands.addAll(shortForms.map { text ->
            GeneratedCommand(
                phrase = text,
                elementUuid = element.uuid ?: "",
                confidence = 0.7f,
                type = CommandType.SHORT_FORM
            )
        })

        // 4. Register commands
        commands.forEach { command ->
            registerCommand(command.phrase, element.uuid ?: "")
        }

        return commands
    }

    /**
     * Generate primary command (most natural phrasing)
     *
     * @param element Element
     * @param meaningfulText Extracted meaningful text
     * @return Primary command
     */
    private fun generatePrimaryCommand(element: ElementInfo, meaningfulText: String): GeneratedCommand {
        val elementType = element.extractElementType()
        val normalizedText = normalizeText(meaningfulText)

        // Determine best action verb based on element type
        val actionVerb = when (elementType) {
            "button" -> "tap"
            "input" -> "focus"
            "checkbox", "switch" -> "toggle"
            "image" -> "view"
            else -> "select"
        }

        // Create natural phrase
        val phrase = when {
            elementType == "button" -> "$actionVerb $normalizedText"
            elementType == "input" -> "$actionVerb $normalizedText field"
            else -> "$actionVerb $normalizedText"
        }

        return GeneratedCommand(
            phrase = phrase.lowercase(Locale.getDefault()),
            elementUuid = element.uuid ?: "",
            confidence = 1.0f,
            type = CommandType.PRIMARY
        )
    }

    /**
     * Generate command synonyms (alternative phrasings)
     *
     * @param element Element
     * @param meaningfulText Meaningful text
     * @return List of synonym commands
     */
    private fun generateSynonyms(element: ElementInfo, meaningfulText: String): List<GeneratedCommand> {
        val synonyms = mutableListOf<GeneratedCommand>()
        val normalizedText = normalizeText(meaningfulText)
        val elementType = element.extractElementType()

        // Generate variations with different action verbs
        val alternativeVerbs = when (elementType) {
            "button" -> listOf("click", "press", "activate")
            "input" -> listOf("select", "open", "tap")
            "checkbox" -> listOf("check", "select", "enable")
            "switch" -> listOf("switch", "turn on", "enable")
            else -> listOf("tap", "select", "open")
        }

        alternativeVerbs.forEach { verb ->
            val phrase = "$verb $normalizedText".lowercase(Locale.getDefault())
            synonyms.add(
                GeneratedCommand(
                    phrase = phrase,
                    elementUuid = element.uuid ?: "",
                    confidence = 0.8f,
                    type = CommandType.SYNONYM
                )
            )
        }

        // Generate variation without verb (direct reference)
        if (normalizedText.split(" ").size <= 3) {
            synonyms.add(
                GeneratedCommand(
                    phrase = normalizedText.lowercase(Locale.getDefault()),
                    elementUuid = element.uuid ?: "",
                    confidence = 0.6f,
                    type = CommandType.DIRECT
                )
            )
        }

        return synonyms
    }

    /**
     * Generate short forms (abbreviated commands)
     *
     * @param text Text to abbreviate
     * @return List of short forms
     */
    private fun generateShortForms(text: String): List<String> {
        val shortForms = mutableListOf<String>()
        val words = text.split(" ").filter { it.isNotBlank() }

        // Remove stop words
        val meaningfulWords = words.filter { word ->
            word.lowercase(Locale.getDefault()) !in stopWords
        }

        // Single word (if available)
        if (meaningfulWords.size == 1) {
            shortForms.add(meaningfulWords[0])
        }

        // First and last word (if multi-word)
        if (meaningfulWords.size > 2) {
            shortForms.add("${meaningfulWords.first()} ${meaningfulWords.last()}")
        }

        // Acronym (if 2-4 words)
        if (meaningfulWords.size in 2..4) {
            val acronym = meaningfulWords.joinToString("") { it.first().toString() }
            shortForms.add(acronym)
        }

        return shortForms.map { it.lowercase(Locale.getDefault()) }
    }

    /**
     * Extract meaningful text from element
     *
     * @param element Element
     * @return Meaningful text
     */
    private fun extractMeaningfulText(element: ElementInfo): String {
        return when {
            element.text.isNotBlank() -> element.text
            element.contentDescription.isNotBlank() -> element.contentDescription
            element.resourceId.isNotBlank() -> {
                // Extract human-readable name from resource ID
                // e.g., "com.app:id/like_button" -> "like button"
                element.resourceId
                    .substringAfterLast('/')
                    .replace('_', ' ')
                    .replace(Regex("([a-z])([A-Z])"), "$1 $2")
                    .lowercase(Locale.getDefault())
            }
            else -> ""
        }
    }

    /**
     * Normalize text (clean up and standardize)
     *
     * @param text Text to normalize
     * @return Normalized text
     */
    private fun normalizeText(text: String): String {
        return text
            .trim()
            .replace(Regex("\\s+"), " ")  // Multiple spaces -> single space
            .replace(Regex("[^a-zA-Z0-9\\s]"), "")  // Remove special chars
            .lowercase(Locale.getDefault())
    }

    /**
     * Register command in registry
     *
     * @param phrase Command phrase
     * @param elementUuid Element UUID
     */
    private fun registerCommand(phrase: String, elementUuid: String) {
        val currentRegistry = _commandRegistry.value.toMutableMap()
        val currentConflicts = _commandConflicts.value.toMutableMap()

        // Check for conflicts
        if (currentRegistry.containsKey(phrase)) {
            val existingUuid = currentRegistry[phrase]!!

            // Move to conflicts
            if (currentConflicts.containsKey(phrase)) {
                val existingConflicts = currentConflicts[phrase]!!.toMutableList()
                if (!existingConflicts.contains(elementUuid)) {
                    existingConflicts.add(elementUuid)
                    currentConflicts[phrase] = existingConflicts
                }
            } else {
                currentConflicts[phrase] = listOf(existingUuid, elementUuid)
            }

            // Remove from registry (ambiguous)
            currentRegistry.remove(phrase)
        } else {
            // No conflict, register normally
            currentRegistry[phrase] = elementUuid
        }

        _commandRegistry.value = currentRegistry
        _commandConflicts.value = currentConflicts
    }

    /**
     * Check if command has conflicts
     *
     * @param phrase Command phrase
     * @return true if command is ambiguous
     */
    fun hasConflict(phrase: String): Boolean {
        return _commandConflicts.value.containsKey(phrase)
    }

    /**
     * Resolve command conflict by adding context
     *
     * @param phrase Conflicting phrase
     * @param element Element to disambiguate
     * @return Disambiguated command
     */
    fun resolveConflict(phrase: String, element: ElementInfo): String {
        // Add contextual information to disambiguate
        val context = when {
            element.resourceId.contains("toolbar") -> "in toolbar"
            element.resourceId.contains("menu") -> "in menu"
            element.resourceId.contains("dialog") -> "in dialog"
            element.bounds.top < 200 -> "at top"
            element.bounds.top > 1000 -> "at bottom"
            else -> ""
        }

        return if (context.isNotBlank()) {
            "$phrase $context"
        } else {
            // Use element type as fallback
            "$phrase ${element.extractElementType()}"
        }
    }

    /**
     * Validate command
     *
     * Checks if command is unique and follows best practices.
     *
     * @param phrase Command phrase
     * @return Validation result
     */
    fun validateCommand(phrase: String): CommandValidationResult {
        val normalizedPhrase = normalizeText(phrase)

        // Check if blank
        if (normalizedPhrase.isBlank()) {
            return CommandValidationResult(
                isValid = false,
                reason = "Command is blank"
            )
        }

        // Check length
        if (normalizedPhrase.length < 2) {
            return CommandValidationResult(
                isValid = false,
                reason = "Command too short (minimum 2 characters)"
            )
        }

        if (normalizedPhrase.length > 100) {
            return CommandValidationResult(
                isValid = false,
                reason = "Command too long (maximum 100 characters)"
            )
        }

        // Check for conflicts
        if (hasConflict(normalizedPhrase)) {
            return CommandValidationResult(
                isValid = false,
                reason = "Command is ambiguous (multiple elements match)",
                conflicts = _commandConflicts.value[normalizedPhrase] ?: emptyList()
            )
        }

        // Check if command exists
        if (!_commandRegistry.value.containsKey(normalizedPhrase)) {
            return CommandValidationResult(
                isValid = false,
                reason = "Command not found in registry"
            )
        }

        return CommandValidationResult(isValid = true)
    }

    /**
     * Get element UUID for command
     *
     * @param phrase Command phrase
     * @return Element UUID (or null if not found/ambiguous)
     */
    fun getElementUuid(phrase: String): String? {
        return _commandRegistry.value[normalizeText(phrase)]
    }

    /**
     * Get all commands for element UUID
     *
     * @param elementUuid Element UUID
     * @return List of command phrases
     */
    fun getCommandsForElement(elementUuid: String): List<String> {
        return _commandRegistry.value
            .filter { it.value == elementUuid }
            .keys
            .toList()
    }

    /**
     * Clear all generated commands
     */
    fun clear() {
        _commandRegistry.value = emptyMap()
        _commandConflicts.value = emptyMap()
    }

    /**
     * Get statistics
     *
     * @return Command generation statistics
     */
    fun getStats(): CommandGenerationStats {
        return CommandGenerationStats(
            totalCommands = _commandRegistry.value.size,
            totalConflicts = _commandConflicts.value.size,
            uniqueElements = _commandRegistry.value.values.toSet().size,
            averageCommandsPerElement = if (_commandRegistry.value.isEmpty()) 0f else {
                _commandRegistry.value.size.toFloat() / _commandRegistry.value.values.toSet().size.toFloat()
            }
        )
    }
}

/**
 * Generated command
 */
data class GeneratedCommand(
    val phrase: String,
    val elementUuid: String,
    val confidence: Float,
    val type: CommandType
)

/**
 * Command type
 */
enum class CommandType {
    PRIMARY,      // Most natural phrasing
    SYNONYM,      // Alternative phrasing
    SHORT_FORM,   // Abbreviated version
    DIRECT        // Direct reference (no verb)
}

/**
 * Command validation result
 */
data class CommandValidationResult(
    val isValid: Boolean,
    val reason: String = "",
    val conflicts: List<String> = emptyList()
)

/**
 * Command generation statistics
 */
data class CommandGenerationStats(
    val totalCommands: Int,
    val totalConflicts: Int,
    val uniqueElements: Int,
    val averageCommandsPerElement: Float
)
