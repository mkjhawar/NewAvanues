package com.augmentalis.webavanue.voiceos

/**
 * Generates voice commands from DOM elements with flexible word-based matching.
 *
 * Matching Logic:
 * - Minimum 2 words required to trigger matching
 * - Progressive matching: more words = more specific match
 * - Returns all matches for NLU disambiguation when multiple matches exist
 *
 * Example:
 * ```kotlin
 * val generator = VoiceCommandGenerator()
 * generator.addElements(domScrapeResult.elements)
 *
 * // User says "Greenland flatly"
 * val matches = generator.findMatches("Greenland flatly")
 * when {
 *     matches.size == 1 -> execute(matches[0])
 *     matches.size > 1 -> askNluToDisambiguate(matches)
 *     else -> notFound()
 * }
 * ```
 */
class VoiceCommandGenerator {

    companion object {
        const val MIN_WORDS_FOR_MATCH = 2
        const val MAX_COMMAND_WORDS = 10
    }

    private val commands = mutableListOf<WebVoiceCommand>()
    private val wordIndex = mutableMapOf<String, MutableList<WebVoiceCommand>>()

    /**
     * A voice command for a web element.
     */
    data class WebVoiceCommand(
        val vosId: String,
        val elementType: String,
        val fullText: String,
        val words: List<String>,
        val selector: String,
        val xpath: String,
        val bounds: ElementBounds,
        val action: CommandAction,
        val metadata: Map<String, String> = emptyMap()
    )

    /**
     * Possible actions for a command.
     */
    enum class CommandAction {
        CLICK,
        FOCUS,
        INPUT,
        SCROLL_TO,
        TOGGLE,
        SELECT
    }

    /**
     * Result of a match operation.
     */
    data class MatchResult(
        val command: WebVoiceCommand,
        val matchedWords: Int,
        val confidence: Float
    )

    /**
     * Clear all commands.
     */
    fun clear() {
        commands.clear()
        wordIndex.clear()
    }

    /**
     * Add DOM elements and generate voice commands.
     */
    fun addElements(elements: List<DOMElement>) {
        elements.forEach { element ->
            val command = createCommand(element)
            if (command != null && command.words.size >= MIN_WORDS_FOR_MATCH) {
                commands.add(command)
                indexCommand(command)
            }
        }
    }

    /**
     * Create a voice command from a DOM element.
     */
    private fun createCommand(element: DOMElement): WebVoiceCommand? {
        val text = extractCommandText(element)
        if (text.isBlank()) return null

        val words = normalizeAndTokenize(text)
        if (words.size < MIN_WORDS_FOR_MATCH) return null

        val action = determineAction(element)

        return WebVoiceCommand(
            vosId = element.id,
            elementType = element.type,
            fullText = text,
            words = words.take(MAX_COMMAND_WORDS),
            selector = element.selector,
            xpath = element.xpath,
            bounds = element.bounds,
            action = action,
            metadata = mapOf(
                "tag" to element.tag,
                "role" to element.role,
                "href" to element.href,
                "inputType" to element.inputType
            )
        )
    }

    /**
     * Extract the best text for voice command from element.
     * Priority: ariaLabel > name > placeholder
     */
    private fun extractCommandText(element: DOMElement): String {
        return when {
            element.ariaLabel.isNotBlank() -> element.ariaLabel
            element.name.isNotBlank() -> element.name
            element.placeholder.isNotBlank() -> element.placeholder
            else -> ""
        }.trim()
    }

    /**
     * Normalize text and split into words for matching.
     * - Lowercase
     * - Remove punctuation
     * - Split on whitespace
     * - Filter empty strings
     */
    private fun normalizeAndTokenize(text: String): List<String> {
        return text
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.length > 1 }
    }

    /**
     * Index a command by its word prefixes for fast lookup.
     */
    private fun indexCommand(command: WebVoiceCommand) {
        // Index by first word
        val firstWord = command.words.firstOrNull() ?: return
        wordIndex.getOrPut(firstWord) { mutableListOf() }.add(command)

        // Also index by first two words combined
        if (command.words.size >= 2) {
            val twoWords = "${command.words[0]} ${command.words[1]}"
            wordIndex.getOrPut(twoWords) { mutableListOf() }.add(command)
        }
    }

    /**
     * Determine the action type based on element type.
     */
    private fun determineAction(element: DOMElement): CommandAction {
        return when (element.type) {
            "link", "button", "menuitem", "tab" -> CommandAction.CLICK
            "input" -> CommandAction.FOCUS
            "checkbox", "radio" -> CommandAction.TOGGLE
            "dropdown" -> CommandAction.SELECT
            else -> CommandAction.CLICK
        }
    }

    /**
     * Find matching commands for the spoken phrase.
     *
     * @param spokenPhrase The words the user said
     * @return List of matches sorted by confidence (best first)
     */
    fun findMatches(spokenPhrase: String): List<MatchResult> {
        val spokenWords = normalizeAndTokenize(spokenPhrase)

        if (spokenWords.size < MIN_WORDS_FOR_MATCH) {
            return emptyList()
        }

        val matches = mutableListOf<MatchResult>()

        for (command in commands) {
            val matchScore = calculateMatchScore(spokenWords, command.words)
            if (matchScore > 0) {
                val confidence = matchScore.toFloat() / spokenWords.size.coerceAtLeast(1)
                matches.add(MatchResult(
                    command = command,
                    matchedWords = matchScore,
                    confidence = confidence
                ))
            }
        }

        // Sort by: 1) matched words (desc), 2) confidence (desc), 3) shorter full text preferred
        return matches.sortedWith(
            compareByDescending<MatchResult> { it.matchedWords }
                .thenByDescending { it.confidence }
                .thenBy { it.command.fullText.length }
        )
    }

    /**
     * Calculate how many words match from the beginning.
     *
     * Returns 0 if less than MIN_WORDS_FOR_MATCH words match.
     */
    private fun calculateMatchScore(spokenWords: List<String>, commandWords: List<String>): Int {
        var matchCount = 0

        for (i in spokenWords.indices) {
            if (i >= commandWords.size) break

            if (commandWords[i].startsWith(spokenWords[i]) ||
                spokenWords[i].startsWith(commandWords[i])) {
                matchCount++
            } else {
                // Words must match sequentially from start
                break
            }
        }

        // Require minimum matches
        return if (matchCount >= MIN_WORDS_FOR_MATCH) matchCount else 0
    }

    /**
     * Get all commands (for debugging/display).
     */
    fun getAllCommands(): List<WebVoiceCommand> = commands.toList()

    /**
     * Get command count.
     */
    fun getCommandCount(): Int = commands.size

    /**
     * Generate disambiguation options for NLU.
     *
     * When multiple matches exist, this provides formatted options
     * for the NLU to present to the user.
     */
    fun generateDisambiguationOptions(matches: List<MatchResult>): List<DisambiguationOption> {
        return matches.take(5).mapIndexed { index, match ->
            val preview = generatePreview(match.command)
            DisambiguationOption(
                index = index + 1,
                preview = preview,
                fullText = match.command.fullText,
                elementType = match.command.elementType,
                command = match.command
            )
        }
    }

    /**
     * Generate a short preview of the command for disambiguation.
     * Uses approximately 3-5 words.
     */
    private fun generatePreview(command: WebVoiceCommand): String {
        val words = command.words
        return when {
            words.size <= 5 -> words.joinToString(" ")
            else -> words.take(4).joinToString(" ") + "..."
        }
    }

    /**
     * Option for NLU disambiguation.
     */
    data class DisambiguationOption(
        val index: Int,
        val preview: String,
        val fullText: String,
        val elementType: String,
        val command: WebVoiceCommand
    )

    /**
     * Quick check if a phrase could potentially match any command.
     * Uses the word index for fast lookup.
     */
    fun hasAnyPotentialMatch(spokenPhrase: String): Boolean {
        val words = normalizeAndTokenize(spokenPhrase)
        if (words.isEmpty()) return false

        val firstWord = words[0]

        // Check single word index
        if (wordIndex.containsKey(firstWord)) return true

        // Check two-word index
        if (words.size >= 2) {
            val twoWords = "${words[0]} ${words[1]}"
            if (wordIndex.containsKey(twoWords)) return true
        }

        return false
    }
}
