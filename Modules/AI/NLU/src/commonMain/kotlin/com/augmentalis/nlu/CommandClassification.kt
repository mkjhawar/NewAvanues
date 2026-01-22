/*
 * Copyright (c) 2024-2026 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * This file is part of AVA AI and is proprietary software.
 * See LICENSE file in the project root for license information.
 */

package com.augmentalis.nlu

/**
 * Result of command classification for VoiceOS integration.
 *
 * This sealed class hierarchy represents all possible outcomes when
 * classifying a voice command utterance against known command phrases.
 *
 * VoiceOS Integration:
 * - Used by CommandProcessor to match spoken commands to registered VoiceOS commands
 * - Supports multiple matching strategies (exact, fuzzy, semantic, hybrid, learned)
 * - Handles ambiguity detection when multiple commands have similar confidence
 *
 * @see IntentClassifier.classifyCommand for the classification method
 */
sealed class CommandClassificationResult {
    /**
     * A clear match was found with confidence above threshold.
     *
     * @property commandId The ID of the matched command (e.g., "voiceos.brightness.increase")
     * @property confidence Confidence score between 0.0 and 1.0
     * @property matchMethod The method used to achieve this match
     */
    data class Match(
        val commandId: String,
        val confidence: Float,
        val matchMethod: MatchMethod = MatchMethod.SEMANTIC
    ) : CommandClassificationResult()

    /**
     * Multiple commands match with similar confidence scores.
     * The caller should handle disambiguation (e.g., prompt user to clarify).
     *
     * @property candidates List of commands that are within the ambiguity threshold
     */
    data class Ambiguous(
        val candidates: List<CommandCandidate>
    ) : CommandClassificationResult()

    /**
     * No command matched with sufficient confidence.
     * The utterance may be a free-form request or unknown command.
     */
    data object NoMatch : CommandClassificationResult()

    /**
     * An error occurred during classification.
     *
     * @property message Description of what went wrong
     */
    data class Error(val message: String) : CommandClassificationResult()
}

/**
 * Represents a command candidate during classification.
 *
 * Used in Ambiguous results to provide ranked alternatives,
 * or internally for scoring multiple commands.
 *
 * @property commandId The command identifier
 * @property confidence Confidence score between 0.0 and 1.0
 * @property phrase Optional: the phrase that matched (useful for debugging)
 */
data class CommandCandidate(
    val commandId: String,
    val confidence: Float,
    val phrase: String? = null
)

/**
 * Classification method used to match a command.
 *
 * This enum indicates which matching strategy produced the result,
 * useful for debugging and quality analysis.
 */
enum class MatchMethod {
    /**
     * Exact string match after normalization (lowercasing, trimming).
     * Fastest and most reliable when available.
     */
    EXACT,

    /**
     * Fuzzy string matching (e.g., Levenshtein distance, n-gram similarity).
     * Handles typos and minor variations.
     */
    FUZZY,

    /**
     * Semantic similarity using embedding vectors.
     * Handles paraphrasing and natural language variations.
     */
    SEMANTIC,

    /**
     * Hybrid approach combining multiple methods.
     * Uses exact/fuzzy first, falls back to semantic.
     */
    HYBRID,

    /**
     * Learned from user corrections or feedback.
     * Highest confidence for personalized commands.
     */
    LEARNED
}
