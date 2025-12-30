/**
 * SimilarityMatcher.kt - Similarity matching algorithms for fuzzy command matching
 * Ported from legacy VoiceUtils.kt
 *
 * Provides Levenshtein distance-based similarity matching for speech recognition
 * commands, enabling fuzzy matching when exact matches are not found.
 *
 * Created: 2025-10-09 02:55:24 PDT
 */

package com.avanues.utils

/**
 * Similarity matching algorithms for fuzzy command matching
 *
 * Uses Levenshtein distance algorithm to calculate similarity between strings,
 * enabling intelligent command matching even when users make minor errors or
 * variations in pronunciation.
 *
 * Example:
 * ```
 * val result = SimilarityMatcher.findMostSimilarWithConfidence(
 *     input = "opn calcluator",
 *     commands = listOf("open calculator", "open camera", "open calendar"),
 *     threshold = 0.70f
 * )
 * // Returns: Pair("open calculator", 0.87f)
 * ```
 */
object SimilarityMatcher {

    /**
     * Find most similar command with confidence score
     *
     * Searches through a list of commands to find the best match for the input
     * string, returning both the command and its similarity score.
     *
     * @param input The user's input string (will be normalized to lowercase)
     * @param commands List of available commands to match against
     * @param threshold Minimum similarity score (0.0-1.0) required for a match
     * @return Pair of (matched command, similarity score) or null if no match above threshold
     *
     * Example:
     * ```
     * findMostSimilarWithConfidence("go bak", listOf("go back", "go home"), 0.70f)
     * // Returns: Pair("go back", 0.86f)
     * ```
     */
    fun findMostSimilarWithConfidence(
        input: String,
        commands: List<String>,
        threshold: Float = 0.70f
    ): Pair<String, Float>? {
        if (commands.isEmpty()) return null

        val normalizedInput = input.lowercase().trim()
        val similarities = commands.map { command ->
            val normalizedCommand = command.lowercase().trim()
            val similarity = calculateSimilarity(normalizedInput, normalizedCommand)
            command to similarity
        }

        val bestMatch = similarities.maxByOrNull { it.second } ?: return null
        return if (bestMatch.second >= threshold) bestMatch else null
    }

    /**
     * Calculate similarity between two strings (0.0-1.0)
     *
     * Uses Levenshtein distance with normalization to produce a similarity score
     * where 1.0 is identical and 0.0 is completely different.
     *
     * Formula: 1.0 - (distance / maxLength)
     *
     * @param s1 First string to compare
     * @param s2 Second string to compare
     * @return Similarity score from 0.0 (different) to 1.0 (identical)
     *
     * Examples:
     * ```
     * calculateSimilarity("hello", "hello") // 1.0
     * calculateSimilarity("hello", "helo")  // 0.8
     * calculateSimilarity("hello", "world") // 0.2
     * ```
     */
    fun calculateSimilarity(s1: String, s2: String): Float {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0f
        if (s1 == s2) return 1.0f

        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return 1.0f - (distance.toFloat() / maxLength.toFloat())
    }

    /**
     * Levenshtein distance algorithm
     *
     * Calculates the minimum number of single-character edits (insertions,
     * deletions, or substitutions) required to change one string into another.
     *
     * This is the classic dynamic programming implementation with O(m*n) time
     * and space complexity, where m and n are the lengths of the input strings.
     *
     * @param s1 Source string
     * @param s2 Target string
     * @return Minimum number of edits needed to transform s1 into s2
     *
     * Examples:
     * ```
     * levenshteinDistance("kitten", "sitting") // 3
     * levenshteinDistance("saturday", "sunday") // 3
     * levenshteinDistance("hello", "hello") // 0
     * ```
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Create DP table
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // Initialize first row and column
        // dp[i][0] = i represents deleting i characters from s1
        // dp[0][j] = j represents inserting j characters to get s2
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        // Fill the DP table
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,        // Deletion
                    dp[i][j - 1] + 1,        // Insertion
                    dp[i - 1][j - 1] + cost  // Substitution (or no change if cost=0)
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Find all commands within similarity threshold
     *
     * Returns multiple commands that match above the threshold, sorted by
     * similarity score in descending order. Useful for showing alternative
     * suggestions to the user.
     *
     * @param input The user's input string
     * @param commands List of available commands
     * @param threshold Minimum similarity score required (0.0-1.0)
     * @param maxResults Maximum number of results to return
     * @return List of (command, similarity) pairs sorted by similarity (highest first)
     *
     * Example:
     * ```
     * findAllSimilar("opn", listOf("open", "option", "opinion"), 0.60f, 3)
     * // Returns: [("open", 0.75), ("option", 0.67)]
     * ```
     */
    fun findAllSimilar(
        input: String,
        commands: List<String>,
        threshold: Float = 0.70f,
        maxResults: Int = 5
    ): List<Pair<String, Float>> {
        val normalizedInput = input.lowercase().trim()

        return commands
            .map { command ->
                val similarity = calculateSimilarity(normalizedInput, command.lowercase().trim())
                command to similarity
            }
            .filter { it.second >= threshold }
            .sortedByDescending { it.second }
            .take(maxResults)
    }

    /**
     * Check if two strings are similar within threshold
     *
     * Simple boolean check for similarity without returning the score.
     *
     * @param s1 First string
     * @param s2 Second string
     * @param threshold Minimum similarity required (0.0-1.0)
     * @return true if similarity >= threshold
     */
    fun isSimilar(s1: String, s2: String, threshold: Float = 0.70f): Boolean {
        return calculateSimilarity(s1, s2) >= threshold
    }
}
