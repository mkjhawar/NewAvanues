/**
 * SimilarityMatcher.kt - Similarity matching algorithms for fuzzy command matching
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-09
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Provides Levenshtein distance-based similarity matching for speech recognition
 * commands, enabling fuzzy matching when exact matches are not found.
 */
package com.augmentalis.speechrecognition

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
     * @param s1 First string to compare
     * @param s2 Second string to compare
     * @return Similarity score from 0.0 (different) to 1.0 (identical)
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
     * @param s1 Source string
     * @param s2 Target string
     * @return Minimum number of edits needed to transform s1 into s2
     */
    fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Create DP table
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        // Initialize first row and column
        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        // Fill the DP table
        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,        // Deletion
                    dp[i][j - 1] + 1,        // Insertion
                    dp[i - 1][j - 1] + cost  // Substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Find all commands within similarity threshold
     *
     * Returns multiple commands that match above the threshold, sorted by
     * similarity score in descending order.
     *
     * @param input The user's input string
     * @param commands List of available commands
     * @param threshold Minimum similarity score required (0.0-1.0)
     * @param maxResults Maximum number of results to return
     * @return List of (command, similarity) pairs sorted by similarity (highest first)
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
     * @param s1 First string
     * @param s2 Second string
     * @param threshold Minimum similarity required (0.0-1.0)
     * @return true if similarity >= threshold
     */
    fun isSimilar(s1: String, s2: String, threshold: Float = 0.70f): Boolean {
        return calculateSimilarity(s1, s2) >= threshold
    }
}
