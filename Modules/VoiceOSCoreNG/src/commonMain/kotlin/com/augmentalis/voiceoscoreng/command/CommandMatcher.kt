package com.augmentalis.voiceoscoreng.command

import com.augmentalis.voiceoscoreng.avu.CommandActionType
import com.augmentalis.voiceoscoreng.avu.QuantizedCommand

/**
 * Command Matcher - Fuzzy matching for voice input to commands.
 *
 * Matches voice input against commands in a registry, supporting:
 * - Exact phrase matching
 * - Fuzzy word-based matching
 * - Action type filtering
 */
object CommandMatcher {

    /**
     * Match voice input against commands in registry.
     *
     * @param voiceInput Raw voice input string
     * @param registry Command registry to search
     * @param threshold Minimum similarity score (0.0 - 1.0), default 0.7
     * @param actionFilter Optional filter for specific action type
     * @return MatchResult indicating match type and matched command(s)
     */
    fun match(
        voiceInput: String,
        registry: CommandRegistry,
        threshold: Float = 0.7f,
        actionFilter: CommandActionType? = null
    ): MatchResult {
        val normalized = voiceInput.lowercase().trim()

        // Empty input = no match
        if (normalized.isBlank()) {
            return MatchResult.NoMatch
        }

        // Get commands, optionally filtered by action type
        val commands = if (actionFilter != null) {
            registry.all().filter { it.actionType == actionFilter }
        } else {
            registry.all()
        }

        if (commands.isEmpty()) {
            return MatchResult.NoMatch
        }

        // Try exact match first
        commands.firstOrNull { cmd ->
            cmd.phrase.lowercase() == normalized
        }?.let {
            return MatchResult.Exact(it)
        }

        // Calculate similarity scores for all commands
        val candidates = commands
            .map { cmd -> cmd to similarity(normalized, cmd.phrase.lowercase()) }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }

        return when {
            candidates.isEmpty() -> MatchResult.NoMatch

            candidates.size == 1 -> MatchResult.Fuzzy(candidates[0].first, candidates[0].second)

            // Check if top candidates have similar scores (ambiguous)
            candidates.size >= 2 && isAmbiguous(candidates[0].second, candidates[1].second) -> {
                MatchResult.Ambiguous(candidates.map { it.first })
            }

            else -> MatchResult.Fuzzy(candidates[0].first, candidates[0].second)
        }
    }

    /**
     * Check if two scores are close enough to be considered ambiguous.
     */
    private fun isAmbiguous(score1: Float, score2: Float): Boolean {
        return (score1 - score2) < 0.1f // Within 10% is ambiguous
    }

    /**
     * Calculate similarity between two strings using Jaccard index on words.
     * Also considers partial word matches for better voice input handling.
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
         */
        data class Exact(val command: QuantizedCommand) : MatchResult()

        /**
         * Fuzzy match found with confidence score.
         */
        data class Fuzzy(val command: QuantizedCommand, val confidence: Float) : MatchResult()

        /**
         * Multiple commands matched with similar scores.
         */
        data class Ambiguous(val candidates: List<QuantizedCommand>) : MatchResult()

        /**
         * No matching command found.
         */
        data object NoMatch : MatchResult()
    }
}
