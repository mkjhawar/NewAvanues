/**
 * ConfidenceScorer.kt - Real-time confidence scoring system for speech recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-09
 * Updated: 2026-01-27 - Migrated to KMP androidMain
 *
 * Provides unified confidence scoring across all speech engines:
 * - Vivoka SDK (0-100 scale)
 * - VOSK (acoustic log-likelihood)
 * - Google Cloud (0-1 scale)
 * - Android STT (0-1 scale)
 * - Whisper (0-1 scale)
 */
package com.augmentalis.speechrecognition

import kotlin.math.exp

/**
 * Confidence result with scoring details
 */
data class ConfidenceResult(
    val text: String,
    val confidence: Float,  // 0.0 to 1.0 normalized
    val level: ConfidenceLevel,
    val alternates: List<Alternate>,
    val scoringMethod: ScoringMethod,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Alternative recognition result
 */
data class Alternate(
    val text: String,
    val confidence: Float,
    val rank: Int
)

/**
 * Scoring method used for this result
 */
enum class ScoringMethod {
    VIVOKA_SDK,
    VOSK_ACOUSTIC,
    GOOGLE_CLOUD,
    ANDROID_STT,
    WHISPER,
    SIMILARITY,
    COMBINED
}

/**
 * Confidence level classification
 */
enum class ConfidenceLevel {
    HIGH,    // >85% - Execute immediately (green)
    MEDIUM,  // 70-85% - Ask confirmation (yellow)
    LOW,     // 50-70% - Show alternatives (orange)
    REJECT   // <50% - Command not recognized (red)
}

/**
 * Recognition engine type
 */
enum class RecognitionEngine {
    VIVOKA,
    VOSK,
    GOOGLE,
    ANDROID,
    WHISPER
}

/**
 * Real-time confidence scorer for speech recognition
 * Normalizes confidence scores from different engines to 0-1 scale
 */
class ConfidenceScorer {
    companion object {
        // Confidence thresholds
        const val THRESHOLD_HIGH = 0.85f
        const val THRESHOLD_MEDIUM = 0.70f
        const val THRESHOLD_LOW = 0.50f

        // VOSK acoustic score normalization parameters
        private const val VOSK_ACOUSTIC_SCALE = 1.0f

        // Vivoka SDK score scale
        private const val VIVOKA_MAX_SCORE = 100f
    }

    /**
     * Normalize confidence score from engine-specific scale to 0-1
     *
     * @param rawScore Raw confidence score from the engine
     * @param engine Recognition engine type
     * @return Normalized confidence (0.0 to 1.0)
     */
    fun normalizeConfidence(rawScore: Float, engine: RecognitionEngine): Float {
        return when (engine) {
            RecognitionEngine.VIVOKA -> {
                // Vivoka uses 0-100 scale
                (rawScore / VIVOKA_MAX_SCORE).coerceIn(0f, 1f)
            }

            RecognitionEngine.VOSK -> {
                // VOSK returns acoustic log-likelihood (negative values)
                // Convert using sigmoid function: 1 / (1 + e^(-x))
                val normalized = 1f / (1f + exp(-rawScore * VOSK_ACOUSTIC_SCALE))
                normalized.coerceIn(0f, 1f)
            }

            RecognitionEngine.GOOGLE,
            RecognitionEngine.ANDROID,
            RecognitionEngine.WHISPER -> {
                // Already 0-1 scale, just ensure bounds
                rawScore.coerceIn(0f, 1f)
            }
        }
    }

    /**
     * Get confidence level classification from normalized score
     *
     * @param confidence Normalized confidence (0.0 to 1.0)
     * @return Confidence level classification
     */
    fun getConfidenceLevel(confidence: Float): ConfidenceLevel {
        return when {
            confidence >= THRESHOLD_HIGH -> ConfidenceLevel.HIGH
            confidence >= THRESHOLD_MEDIUM -> ConfidenceLevel.MEDIUM
            confidence >= THRESHOLD_LOW -> ConfidenceLevel.LOW
            else -> ConfidenceLevel.REJECT
        }
    }

    /**
     * Combine acoustic and language model scores with weighting
     *
     * @param acousticScore Acoustic model confidence
     * @param languageModelScore Language model confidence
     * @param weight Weight for acoustic score (0-1), default 0.7
     * @return Combined confidence score
     */
    fun combineScores(
        acousticScore: Float,
        languageModelScore: Float,
        weight: Float = 0.7f
    ): Float {
        val normalizedWeight = weight.coerceIn(0f, 1f)
        val combined = (acousticScore * normalizedWeight) +
                      (languageModelScore * (1f - normalizedWeight))
        return combined.coerceIn(0f, 1f)
    }

    /**
     * Create a complete confidence result from recognition output
     *
     * @param text Recognized text
     * @param rawConfidence Raw confidence from engine
     * @param engine Recognition engine type
     * @param alternates Alternative recognition results
     * @return Complete confidence result
     */
    fun createResult(
        text: String,
        rawConfidence: Float,
        engine: RecognitionEngine,
        alternates: List<Alternate> = emptyList()
    ): ConfidenceResult {
        val normalized = normalizeConfidence(rawConfidence, engine)
        val level = getConfidenceLevel(normalized)

        val scoringMethod = when (engine) {
            RecognitionEngine.VIVOKA -> ScoringMethod.VIVOKA_SDK
            RecognitionEngine.VOSK -> ScoringMethod.VOSK_ACOUSTIC
            RecognitionEngine.GOOGLE -> ScoringMethod.GOOGLE_CLOUD
            RecognitionEngine.ANDROID -> ScoringMethod.ANDROID_STT
            RecognitionEngine.WHISPER -> ScoringMethod.WHISPER
        }

        return ConfidenceResult(
            text = text,
            confidence = normalized,
            level = level,
            alternates = alternates,
            scoringMethod = scoringMethod
        )
    }

    /**
     * Calculate similarity-based confidence between recognized text and known commands
     *
     * @param recognized Recognized text
     * @param command Known command
     * @return Similarity score (0.0 to 1.0)
     */
    fun calculateSimilarityScore(recognized: String, command: String): Float {
        return SimilarityMatcher.calculateSimilarity(recognized, command)
    }

    /**
     * Find best matching command from a list based on similarity
     *
     * @param recognized Recognized text
     * @param commands List of known commands
     * @param minConfidence Minimum confidence threshold (default: THRESHOLD_LOW)
     * @return Pair of (best match, confidence) or null if no good match
     */
    fun findBestMatch(
        recognized: String,
        commands: List<String>,
        minConfidence: Float = THRESHOLD_LOW
    ): Pair<String, Float>? {
        return SimilarityMatcher.findMostSimilarWithConfidence(
            input = recognized,
            commands = commands,
            threshold = minConfidence
        )
    }

    /**
     * Find all similar commands above threshold
     *
     * @param recognized Recognized text
     * @param commands List of known commands
     * @param minConfidence Minimum confidence threshold (default: THRESHOLD_LOW)
     * @param maxResults Maximum results to return
     * @return List of (command, confidence) pairs sorted by confidence
     */
    fun findAllSimilar(
        recognized: String,
        commands: List<String>,
        minConfidence: Float = THRESHOLD_LOW,
        maxResults: Int = 5
    ): List<Pair<String, Float>> {
        return SimilarityMatcher.findAllSimilar(
            input = recognized,
            commands = commands,
            threshold = minConfidence,
            maxResults = maxResults
        )
    }

    /**
     * Boost confidence for commands that appear frequently in history
     *
     * @param baseConfidence Base confidence from recognition
     * @param commandUsageCount How many times this command was used
     * @param totalCommandCount Total commands in history
     * @return Boosted confidence
     */
    fun applyHistoryBoost(
        baseConfidence: Float,
        commandUsageCount: Int,
        totalCommandCount: Int,
        maxBoost: Float = 0.1f
    ): Float {
        if (totalCommandCount == 0) return baseConfidence

        val frequency = commandUsageCount.toFloat() / totalCommandCount.toFloat()
        val boost = frequency * maxBoost

        return (baseConfidence + boost).coerceIn(0f, 1f)
    }

    /**
     * Apply time-based decay to confidence for stale results
     *
     * @param confidence Base confidence
     * @param ageMs Age of result in milliseconds
     * @param decayHalfLife Half-life for decay in milliseconds (default 5 seconds)
     * @return Decayed confidence
     */
    fun applyTimeDecay(
        confidence: Float,
        ageMs: Long,
        decayHalfLife: Long = 5000L
    ): Float {
        if (ageMs <= 0) return confidence

        val decayFactor = exp(-(ageMs.toFloat() / decayHalfLife.toFloat()) * 0.693f)
        return (confidence * decayFactor).coerceIn(0f, 1f)
    }
}
