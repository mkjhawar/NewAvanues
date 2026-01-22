/**
 * LearningSystem.android.kt - Learning capabilities for speech recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-27
 * Updated: 2026-01-18 - Migrated to KMP androidMain
 */
package com.augmentalis.speechrecognition

import android.content.Context
import android.util.Log

/**
 * Provides learning capabilities for speech recognition.
 * Learns from user corrections and improves command matching over time.
 */
class LearningSystem(
    private val engineName: String,
    private val context: Context
) {

    companion object {
        private const val TAG = "LearningSystem"
        private const val DEFAULT_LEARNING_THRESHOLD = 0.7f
    }

    // Learned command mappings (spoken -> canonical)
    private val learnedCommands = mutableMapOf<String, LearnedCommand>()

    // Recognition confidence history
    private val confidenceHistory = mutableListOf<ConfidenceEntry>()
    private val maxHistorySize = 1000

    /**
     * Process text with learning enhancement
     */
    fun processWithLearning(
        spokenText: String,
        availableCommands: List<String>,
        threshold: Float = DEFAULT_LEARNING_THRESHOLD
    ): MatchResult {
        val normalized = spokenText.lowercase().trim()

        // Check learned commands first
        learnedCommands[normalized]?.let { learned ->
            Log.d(TAG, "[$engineName] Found learned mapping: '$normalized' -> '${learned.canonicalCommand}'")
            return MatchResult(
                matched = learned.canonicalCommand,
                confidence = learned.confidence,
                source = MatchSource.LEARNED_COMMAND
            )
        }

        // Check for exact match
        availableCommands.find { it.lowercase() == normalized }?.let { exact ->
            return MatchResult(
                matched = exact,
                confidence = 1.0f,
                source = MatchSource.EXACT_MATCH
            )
        }

        // Try fuzzy matching
        val bestMatch = findBestFuzzyMatch(normalized, availableCommands, threshold)
        return bestMatch ?: MatchResult(
            matched = normalized,
            confidence = 0f,
            source = MatchSource.NO_MATCH
        )
    }

    /**
     * Learn a command mapping from user correction
     */
    fun learnCommand(spokenText: String, canonicalCommand: String, confidence: Float) {
        val normalized = spokenText.lowercase().trim()

        val learned = LearnedCommand(
            spokenText = normalized,
            canonicalCommand = canonicalCommand,
            confidence = confidence,
            learnedAt = System.currentTimeMillis(),
            usageCount = 1
        )

        learnedCommands[normalized] = learned
        Log.i(TAG, "[$engineName] Learned command: '$normalized' -> '$canonicalCommand'")
    }

    /**
     * Record a successful recognition for confidence tracking
     */
    fun recordSuccess(command: String, confidence: Float) {
        val entry = ConfidenceEntry(
            command = command,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            success = true
        )
        addConfidenceEntry(entry)

        // Update usage count for learned commands
        learnedCommands[command.lowercase()]?.let {
            learnedCommands[command.lowercase()] = it.copy(usageCount = it.usageCount + 1)
        }
    }

    /**
     * Record a failed recognition
     */
    fun recordFailure(command: String, confidence: Float) {
        val entry = ConfidenceEntry(
            command = command,
            confidence = confidence,
            timestamp = System.currentTimeMillis(),
            success = false
        )
        addConfidenceEntry(entry)
    }

    /**
     * Find best fuzzy match using Levenshtein distance
     */
    private fun findBestFuzzyMatch(
        text: String,
        commands: List<String>,
        threshold: Float
    ): MatchResult? {
        var bestMatch: String? = null
        var bestSimilarity = 0f

        for (command in commands) {
            val similarity = calculateSimilarity(text, command.lowercase())
            if (similarity > bestSimilarity && similarity >= threshold) {
                bestSimilarity = similarity
                bestMatch = command
            }
        }

        return bestMatch?.let {
            MatchResult(
                matched = it,
                confidence = bestSimilarity,
                source = MatchSource.FUZZY_MATCH
            )
        }
    }

    /**
     * Calculate string similarity using Levenshtein distance
     */
    private fun calculateSimilarity(s1: String, s2: String): Float {
        val distance = levenshteinDistance(s1, s2)
        val maxLength = maxOf(s1.length, s2.length)
        return if (maxLength == 0) 1.0f else 1.0f - (distance.toFloat() / maxLength)
    }

    /**
     * Levenshtein distance calculation
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Add confidence entry with history management
     */
    private fun addConfidenceEntry(entry: ConfidenceEntry) {
        confidenceHistory.add(entry)
        if (confidenceHistory.size > maxHistorySize) {
            confidenceHistory.removeAt(0)
        }
    }

    /**
     * Get learning statistics
     */
    fun getStatistics(): LearningStats {
        val totalRecognitions = confidenceHistory.size
        val successfulRecognitions = confidenceHistory.count { it.success }
        val averageConfidence = if (confidenceHistory.isNotEmpty()) {
            confidenceHistory.map { it.confidence }.average().toFloat()
        } else {
            0f
        }

        return LearningStats(
            learnedCommandCount = learnedCommands.size,
            totalRecognitions = totalRecognitions,
            successRate = if (totalRecognitions > 0) {
                successfulRecognitions.toFloat() / totalRecognitions
            } else 0f,
            averageConfidence = averageConfidence
        )
    }

    /**
     * Get all learned commands
     */
    fun getLearnedCommands(): Map<String, LearnedCommand> = learnedCommands.toMap()

    /**
     * Clear learned commands
     */
    fun clearLearned() {
        learnedCommands.clear()
        Log.d(TAG, "[$engineName] Learned commands cleared")
    }

    /**
     * Destroy and clean up
     */
    fun destroy() {
        learnedCommands.clear()
        confidenceHistory.clear()
        Log.d(TAG, "[$engineName] Learning system destroyed")
    }

    /**
     * Match result
     */
    data class MatchResult(
        val matched: String,
        val confidence: Float,
        val source: MatchSource
    )

    /**
     * Match source enum
     */
    enum class MatchSource {
        EXACT_MATCH,
        LEARNED_COMMAND,
        FUZZY_MATCH,
        NO_MATCH
    }

    /**
     * Learned command entry
     */
    data class LearnedCommand(
        val spokenText: String,
        val canonicalCommand: String,
        val confidence: Float,
        val learnedAt: Long,
        val usageCount: Int
    )

    /**
     * Confidence history entry
     */
    data class ConfidenceEntry(
        val command: String,
        val confidence: Float,
        val timestamp: Long,
        val success: Boolean
    )

    /**
     * Learning statistics
     */
    data class LearningStats(
        val learnedCommandCount: Int,
        val totalRecognitions: Int,
        val successRate: Float,
        val averageConfidence: Float
    )
}
