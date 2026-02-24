/**
 * PatternMatcher - Exact pattern matching for intents
 *
 * Fast O(1) lookup for exact phrase matches using HashMap.
 * First stage in the hybrid classification pipeline.
 *
 * Created: 2025-12-07
 */

package com.augmentalis.nlu.matcher

import com.augmentalis.nlu.NluThresholds
import com.augmentalis.nlu.model.IntentMatch
import com.augmentalis.nlu.model.MatchMethod
import com.augmentalis.nlu.model.UnifiedIntent

/**
 * Exact pattern matcher using normalized phrase lookup.
 *
 * Performance: O(1) average for lookups.
 */
class PatternMatcher {

    // Normalized phrase -> Intent mapping
    private val patternIndex = mutableMapOf<String, MutableList<UnifiedIntent>>()

    /**
     * Index intents for fast pattern lookup
     */
    fun index(intents: List<UnifiedIntent>) {
        patternIndex.clear()
        for (intent in intents) {
            for (phrase in intent.allPhrases) {
                val normalized = normalize(phrase)
                patternIndex.getOrPut(normalized) { mutableListOf() }.add(intent)
            }
        }
    }

    /**
     * Match input against indexed patterns
     *
     * @param input User input text
     * @return List of matches sorted by priority, empty if no exact match
     */
    fun match(input: String): List<IntentMatch> {
        val normalized = normalize(input)

        // Direct match
        patternIndex[normalized]?.let { matches ->
            return matches
                .sortedByDescending { it.priority }
                .map { intent ->
                    IntentMatch(
                        intent = intent,
                        score = 1.0f,
                        matchedPhrase = intent.allPhrases.find { normalize(it) == normalized },
                        method = MatchMethod.EXACT
                    )
                }
        }

        // Check for prefix matches (e.g., "go back" matches "go back to home")
        val prefixMatches = patternIndex.entries
            .filter { (pattern, _) -> normalized.startsWith(pattern) || pattern.startsWith(normalized) }
            .flatMap { (pattern, intents) ->
                intents.map { intent ->
                    val similarity = minOf(pattern.length, normalized.length).toFloat() /
                            maxOf(pattern.length, normalized.length)
                    IntentMatch(
                        intent = intent,
                        score = similarity,
                        matchedPhrase = pattern,
                        method = MatchMethod.EXACT
                    )
                }
            }
            .filter { it.score >= NluThresholds.PREFIX_MATCH_MIN_SIMILARITY }
            .sortedByDescending { it.score * it.intent.priority }

        return prefixMatches
    }

    /**
     * Check if exact match exists
     */
    fun hasExactMatch(input: String): Boolean {
        return patternIndex.containsKey(normalize(input))
    }

    /**
     * Normalize text for matching
     */
    private fun normalize(text: String): String {
        return text.lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
            .replace(Regex("[^a-z0-9 ]"), "")
    }

    /**
     * Get indexed pattern count
     */
    fun patternCount(): Int = patternIndex.size

    /**
     * Clear index
     */
    fun clear() {
        patternIndex.clear()
    }
}
