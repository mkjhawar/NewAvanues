package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.ISynonymProvider

/**
 * Command Matcher - Fuzzy matching for voice input to commands.
 *
 * Matches voice input against commands in a registry, supporting:
 * - Synonym expansion (e.g., "tap" → "click")
 * - Exact phrase matching
 * - Fuzzy word-based matching
 * - Action type filtering
 *
 * ## Matching Flow:
 * 1. Expand synonyms in input (if provider available)
 * 2. Try exact match with expanded input
 * 3. Try exact match with original input
 * 4. Calculate fuzzy similarity scores
 * 5. Return best match or ambiguous result
 */
object CommandMatcher {

    /**
     * Synonym provider for expanding voice input.
     * Set this to enable synonym-aware matching.
     */
    var synonymProvider: ISynonymProvider? = null

    /**
     * Default language for synonym expansion.
     */
    var defaultLanguage: String = "en"

    /**
     * Match voice input against commands in registry.
     *
     * @param voiceInput Raw voice input string
     * @param registry Command registry to search
     * @param threshold Minimum similarity score (0.0 - 1.0), default 0.7
     * @param actionFilter Optional filter for specific action type
     * @param language Language for synonym expansion (uses defaultLanguage if null)
     * @return MatchResult indicating match type and matched command(s)
     */
    fun match(
        voiceInput: String,
        registry: CommandRegistry,
        threshold: Float = 0.7f,
        actionFilter: CommandActionType? = null,
        language: String? = null
    ): MatchResult {
        val normalized = voiceInput.lowercase().trim()

        // Empty input = no match
        if (normalized.isBlank()) {
            return MatchResult.NoMatch
        }

        // Expand synonyms if provider is available
        val lang = language ?: defaultLanguage
        val expanded = synonymProvider?.expand(normalized, lang) ?: normalized

        // Get commands, optionally filtered by action type
        val commands = if (actionFilter != null) {
            registry.all().filter { it.actionType == actionFilter }
        } else {
            registry.all()
        }

        if (commands.isEmpty()) {
            return MatchResult.NoMatch
        }

        // Try exact match with expanded input first
        if (expanded != normalized) {
            commands.firstOrNull { cmd ->
                cmd.phrase.lowercase() == expanded
            }?.let {
                return MatchResult.Exact(it, synonymExpanded = true)
            }
        }

        // Try exact match with original input
        commands.firstOrNull { cmd ->
            cmd.phrase.lowercase() == normalized
        }?.let {
            return MatchResult.Exact(it)
        }

        // Try exact match with symbol normalization (e.g., "sound and vibration" = "Sound & vibration")
        val symbolNormalized = SymbolNormalizer.normalize(normalized, lang)
        if (symbolNormalized != normalized) {
            commands.firstOrNull { cmd ->
                SymbolNormalizer.normalize(cmd.phrase.lowercase(), lang) == symbolNormalized
            }?.let {
                return MatchResult.Exact(it, synonymExpanded = true)
            }
        }

        // Calculate similarity scores using expanded input
        val candidates = commands
            .map { cmd ->
                // Try both original and expanded, take better score
                val scoreOriginal = similarity(normalized, cmd.phrase.lowercase())
                val scoreExpanded = if (expanded != normalized) {
                    similarity(expanded, cmd.phrase.lowercase())
                } else {
                    scoreOriginal
                }
                cmd to maxOf(scoreOriginal, scoreExpanded)
            }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }

        return when {
            candidates.isEmpty() -> MatchResult.NoMatch

            candidates.size == 1 -> MatchResult.Fuzzy(
                candidates[0].first,
                candidates[0].second,
                synonymExpanded = expanded != normalized
            )

            // Check if top candidates have similar scores (ambiguous)
            candidates.size >= 2 && isAmbiguous(candidates[0].second, candidates[1].second) -> {
                MatchResult.Ambiguous(candidates.map { it.first })
            }

            else -> MatchResult.Fuzzy(
                candidates[0].first,
                candidates[0].second,
                synonymExpanded = expanded != normalized
            )
        }
    }

    /**
     * Match with explicit synonym provider (for testing or one-off use).
     */
    fun matchWithSynonyms(
        voiceInput: String,
        registry: CommandRegistry,
        provider: ISynonymProvider,
        language: String = "en",
        threshold: Float = 0.7f,
        actionFilter: CommandActionType? = null
    ): MatchResult {
        val originalProvider = synonymProvider
        val originalLanguage = defaultLanguage
        try {
            synonymProvider = provider
            defaultLanguage = language
            return match(voiceInput, registry, threshold, actionFilter, language)
        } finally {
            synonymProvider = originalProvider
            defaultLanguage = originalLanguage
        }
    }

    /**
     * Check if two scores are close enough to be considered ambiguous.
     */
    private fun isAmbiguous(score1: Float, score2: Float): Boolean {
        return (score1 - score2) < 0.1f // Within 10% is ambiguous
    }

    /**
     * Match voice input against a phrase with symbol alias support.
     * Handles bidirectional matching:
     * - "sound and vibration" matches "Sound & vibration"
     * - "display size ampersand text" also matches
     *
     * @param voiceInput User's voice input
     * @param phrase Command phrase to match
     * @param locale Locale for symbol normalization
     * @return True if inputs are equivalent after normalization
     */
    fun matchWithSymbolAliases(
        voiceInput: String,
        phrase: String,
        locale: String = "en"
    ): Boolean {
        return SymbolNormalizer.matchWithAliases(voiceInput, phrase, locale)
    }

    /**
     * Calculate similarity between two strings using Jaccard index on words.
     * Also considers partial word matches for better voice input handling.
     * Normalizes symbols before comparison for better matching.
     */
    private fun similarity(input: String, phrase: String): Float {
        val inputWords = input.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()
        val phraseWords = phrase.split(Regex("\\s+")).filter { it.isNotBlank() }.toSet()

        if (inputWords.isEmpty() || phraseWords.isEmpty()) {
            return 0f
        }

        // Exact word intersection
        val exactIntersection = inputWords.intersect(phraseWords).size

        // Partial matches (input word is substring of phrase word or vice versa)
        var partialMatches = 0f
        for (inputWord in inputWords) {
            for (phraseWord in phraseWords) {
                if (inputWord !in phraseWords && phraseWord !in inputWords) {
                    if (phraseWord.contains(inputWord) || inputWord.contains(phraseWord)) {
                        partialMatches += 0.5f
                        break
                    }
                }
            }
        }

        val union = inputWords.union(phraseWords).size
        val totalMatches = exactIntersection + partialMatches

        return if (union == 0) 0f else (totalMatches / union).coerceIn(0f, 1f)
    }

    /**
     * Result of a command match operation.
     */
    sealed class MatchResult {
        /**
         * Exact phrase match found.
         *
         * @property command The matched command
         * @property synonymExpanded True if match was found via synonym expansion
         */
        data class Exact(
            val command: QuantizedCommand,
            val synonymExpanded: Boolean = false
        ) : MatchResult()

        /**
         * Fuzzy match found with confidence score.
         *
         * @property command The matched command
         * @property confidence Similarity score (0.0 - 1.0)
         * @property synonymExpanded True if match was improved via synonym expansion
         */
        data class Fuzzy(
            val command: QuantizedCommand,
            val confidence: Float,
            val synonymExpanded: Boolean = false
        ) : MatchResult()

        /**
         * Multiple commands matched with similar scores.
         *
         * @property candidates List of ambiguous matches
         */
        data class Ambiguous(val candidates: List<QuantizedCommand>) : MatchResult()

        /**
         * No matching command found.
         */
        data object NoMatch : MatchResult()

        /**
         * Check if this result found a match.
         */
        fun isMatch(): Boolean = this !is NoMatch

        /**
         * Get the matched command, or null if no match.
         */
        fun matchedCommand(): QuantizedCommand? = when (this) {
            is Exact -> command
            is Fuzzy -> command
            is Ambiguous -> candidates.firstOrNull()
            is NoMatch -> null
        }
    }
}
