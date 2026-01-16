/**
 * FuzzyMatcher - Levenshtein distance-based fuzzy matching
 *
 * Second stage in hybrid classification for handling typos
 * and speech recognition errors.
 *
 * Created: 2025-12-07
 */

package com.augmentalis.nlu.matcher

import com.augmentalis.nlu.model.IntentMatch
import com.augmentalis.nlu.model.MatchMethod
import com.augmentalis.nlu.model.UnifiedIntent
import kotlin.math.max
import kotlin.math.min

/**
 * Fuzzy matcher using normalized Levenshtein distance.
 *
 * Performance: O(n * m) where n = input length, m = pattern length
 * For each intent phrase comparison.
 */
class FuzzyMatcher(
    private val minSimilarity: Float = 0.7f,
    private val maxCandidates: Int = 5
) {

    private var indexedIntents: List<UnifiedIntent> = emptyList()

    /**
     * Index intents for fuzzy matching
     */
    fun index(intents: List<UnifiedIntent>) {
        indexedIntents = intents
    }

    /**
     * Find fuzzy matches for input
     *
     * @param input User input text
     * @return List of matches above similarity threshold
     */
    fun match(input: String): List<IntentMatch> {
        val normalized = normalize(input)
        val candidates = mutableListOf<IntentMatch>()

        for (intent in indexedIntents) {
            var bestScore = 0f
            var bestPhrase: String? = null

            for (phrase in intent.allPhrases) {
                val normalizedPhrase = normalize(phrase)
                val similarity = calculateSimilarity(normalized, normalizedPhrase)

                if (similarity > bestScore) {
                    bestScore = similarity
                    bestPhrase = phrase
                }
            }

            if (bestScore >= minSimilarity) {
                candidates.add(
                    IntentMatch(
                        intent = intent,
                        score = bestScore,
                        matchedPhrase = bestPhrase,
                        method = MatchMethod.FUZZY
                    )
                )
            }
        }

        return candidates
            .sortedByDescending { it.score * (1 + it.intent.priority * 0.1f) }
            .take(maxCandidates)
    }

    /**
     * Calculate normalized similarity between two strings
     *
     * @return Similarity score 0.0-1.0
     */
    fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1 == s2) return 1.0f
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f

        val distance = levenshteinDistance(s1, s2)
        val maxLength = max(s1.length, s2.length)

        return 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Calculate Levenshtein edit distance
     *
     * Uses optimized single-row algorithm for memory efficiency.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Optimization: shorter string as s2
        if (len1 < len2) {
            return levenshteinDistance(s2, s1)
        }

        // Single row optimization
        var previousRow = IntArray(len2 + 1) { it }
        var currentRow = IntArray(len2 + 1)

        for (i in 1..len1) {
            currentRow[0] = i

            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currentRow[j] = min(
                    min(currentRow[j - 1] + 1, previousRow[j] + 1),
                    previousRow[j - 1] + cost
                )
            }

            // Swap rows
            val temp = previousRow
            previousRow = currentRow
            currentRow = temp
        }

        return previousRow[len2]
    }

    /**
     * Calculate word-level Jaccard similarity
     *
     * Useful for multi-word commands where word order may vary.
     */
    fun wordSimilarity(s1: String, s2: String): Float {
        val words1 = s1.split(" ").toSet()
        val words2 = s2.split(" ").toSet()

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        return if (union == 0) 0f else intersection.toFloat() / union
    }

    /**
     * Normalize text for matching
     */
    private fun normalize(text: String): String {
        return text.lowercase()
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    /**
     * Get indexed intent count
     */
    fun intentCount(): Int = indexedIntents.size

    /**
     * Clear index
     */
    fun clear() {
        indexedIntents = emptyList()
    }
}
