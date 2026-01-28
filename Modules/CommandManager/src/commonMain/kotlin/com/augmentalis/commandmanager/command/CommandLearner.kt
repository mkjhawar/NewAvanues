package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo

/**
 * Represents a voice command learned from a UI element.
 *
 * @property phrase The primary voice phrase to trigger this command
 * @property targetVuid The VUID of the target element
 * @property action The action type: "tap", "scroll", or "long_press"
 * @property confidence The confidence level of the learned command (0.0 to 1.0)
 * @property aliases Alternative phrases that can trigger this command
 * @property createdAt Timestamp when the command was learned
 */
data class LearnedCommand(
    val phrase: String,
    val targetVuid: String,
    val action: String,
    val confidence: Float = 1.0f,
    val aliases: List<String> = emptyList(),
    val createdAt: Long = com.augmentalis.commandmanager.currentTimeMillis()
)

/**
 * Learns and manages voice commands from UI elements.
 *
 * The CommandLearner is responsible for:
 * - Generating voice command phrases from UI element properties
 * - Mapping phrases and aliases to commands
 * - Determining appropriate actions based on element capabilities
 * - Providing command lookup and management
 *
 * This is a core component of the LearnApp system (Phase 14) that enables
 * dynamic voice command generation from screen exploration.
 */
class CommandLearner {

    private val learnedCommands = mutableMapOf<String, LearnedCommand>()

    /**
     * Learn a voice command from a UI element.
     *
     * Creates a LearnedCommand with appropriate phrase, action, and aliases
     * based on the element's properties. The command is registered for both
     * the primary phrase and all generated aliases.
     *
     * @param element The UI element to learn from
     * @param vuid The unique identifier for the element
     * @return The created LearnedCommand, or null if the element has no usable text
     */
    fun learnCommand(element: ElementInfo, vuid: String): LearnedCommand? {
        val phrase = generatePrimaryPhrase(element) ?: return null
        val action = determineAction(element)
        val aliases = generateAliases(element)

        val command = LearnedCommand(
            phrase = phrase,
            targetVuid = vuid,
            action = action,
            aliases = aliases
        )

        // Register primary phrase
        learnedCommands[phrase.lowercase()] = command

        // Register all aliases
        aliases.forEach { alias ->
            learnedCommands[alias.lowercase()] = command
        }

        return command
    }

    /**
     * Find a command by phrase or alias.
     *
     * @param phrase The voice phrase to search for (case-insensitive)
     * @return The matching LearnedCommand, or null if not found
     */
    fun findCommand(phrase: String): LearnedCommand? {
        return learnedCommands[phrase.lowercase()]
    }

    /**
     * Get all learned commands, deduplicated by target VUID.
     *
     * @return List of distinct LearnedCommands
     */
    fun getAllCommands(): List<LearnedCommand> {
        return learnedCommands.values.distinctBy { it.targetVuid }
    }

    /**
     * Remove a command and all its aliases by VUID.
     *
     * @param vuid The VUID of the command to remove
     * @return true if any command was removed, false otherwise
     */
    fun removeCommand(vuid: String): Boolean {
        val toRemove = learnedCommands.entries.filter { it.value.targetVuid == vuid }
        toRemove.forEach { learnedCommands.remove(it.key) }
        return toRemove.isNotEmpty()
    }

    /**
     * Generate voice labels from an element's properties.
     *
     * Extracts potential voice labels from:
     * 1. Text content (primary)
     * 2. Content description (secondary)
     * 3. Class name derivative (tertiary, excludes "view")
     *
     * @param element The UI element to extract labels from
     * @return List of distinct lowercase voice labels
     */
    fun generateVoiceLabels(element: ElementInfo): List<String> {
        val labels = mutableListOf<String>()

        // Primary: text content
        if (element.text.isNotBlank()) {
            labels.add(element.text.trim().lowercase())
        }

        // Secondary: content description
        if (element.contentDescription.isNotBlank()) {
            labels.add(element.contentDescription.trim().lowercase())
        }

        // Tertiary: derived from class name (e.g., "Button" -> "button")
        element.className.substringAfterLast('.').lowercase().let { className ->
            if (className.isNotBlank() && className != "view") {
                labels.add(className)
            }
        }

        return labels.distinct()
    }

    /**
     * Suggest alias phrases for a label.
     *
     * Provides common synonyms and alternative phrasings for
     * frequently used UI labels.
     *
     * @param label The label to find aliases for (case-insensitive)
     * @return List of suggested alias phrases
     */
    fun suggestAliases(label: String): List<String> {
        val aliases = mutableListOf<String>()
        val normalized = label.lowercase()

        // Common substitutions for frequently used labels
        val substitutions = mapOf(
            "settings" to listOf("preferences", "options", "config"),
            "search" to listOf("find", "lookup"),
            "share" to listOf("send"),
            "delete" to listOf("remove", "trash"),
            "edit" to listOf("modify", "change"),
            "add" to listOf("create", "new"),
            "close" to listOf("dismiss", "exit"),
            "back" to listOf("return", "previous"),
            "next" to listOf("forward", "continue"),
            "menu" to listOf("more", "options")
        )

        substitutions[normalized]?.let { aliases.addAll(it) }

        return aliases
    }

    /**
     * Get the count of distinct learned commands.
     *
     * @return Number of unique commands (by VUID)
     */
    fun getCommandCount(): Int = learnedCommands.values.distinctBy { it.targetVuid }.size

    /**
     * Clear all learned commands.
     */
    fun clear() {
        learnedCommands.clear()
    }

    /**
     * Generate the primary voice phrase from an element.
     *
     * Priority: text > contentDescription
     *
     * @param element The UI element
     * @return The primary phrase in lowercase, or null if no usable text
     */
    private fun generatePrimaryPhrase(element: ElementInfo): String? {
        return when {
            element.text.isNotBlank() -> element.text.trim().lowercase()
            element.contentDescription.isNotBlank() -> element.contentDescription.trim().lowercase()
            else -> null
        }
    }

    /**
     * Determine the appropriate action for an element.
     *
     * Priority: scroll > long_press > tap
     *
     * @param element The UI element
     * @return The action type: "scroll", "long_press", or "tap"
     */
    private fun determineAction(element: ElementInfo): String {
        return when {
            element.isScrollable -> "scroll"
            element.isLongClickable -> "long_press"
            element.isClickable -> "tap"
            else -> "tap"
        }
    }

    /**
     * Generate aliases for an element.
     *
     * Includes:
     * - Content description as alias if text is the primary phrase
     * - Suggested aliases based on the primary phrase
     *
     * @param element The UI element
     * @return List of distinct lowercase alias phrases
     */
    private fun generateAliases(element: ElementInfo): List<String> {
        val aliases = mutableListOf<String>()

        // Add content description as alias if text is primary
        if (element.text.isNotBlank() && element.contentDescription.isNotBlank()) {
            aliases.add(element.contentDescription.trim().lowercase())
        }

        // Add suggested aliases based on primary phrase
        generatePrimaryPhrase(element)?.let { phrase ->
            aliases.addAll(suggestAliases(phrase))
        }

        return aliases.distinct()
    }
}
