/**
 * INluProcessor.kt - NLU Processor Interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Platform-agnostic interface for NLU (Natural Language Understanding) processing.
 * Enables semantic intent classification using BERT embeddings.
 */
package com.augmentalis.voiceoscoreng.nlu

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * Interface for NLU (Natural Language Understanding) processing.
 *
 * Provides semantic intent classification using BERT embeddings.
 * Android implementation wraps IntentClassifier from Shared/NLU module.
 * iOS/Desktop implementations are stubs until native ONNX support is added.
 */
interface INluProcessor {
    /**
     * Initialize the NLU processor.
     * Loads the BERT model and vocabulary.
     *
     * @return Result indicating success or failure
     */
    suspend fun initialize(): Result<Unit>

    /**
     * Classify an utterance against candidate commands.
     *
     * @param utterance The voice input text to classify
     * @param candidateCommands Available commands to match against
     * @return NluResult with match, ambiguous, no match, or error
     */
    suspend fun classify(
        utterance: String,
        candidateCommands: List<QuantizedCommand>
    ): NluResult

    /**
     * Check if the NLU processor is available and initialized.
     */
    fun isAvailable(): Boolean

    /**
     * Dispose resources and cleanup.
     */
    suspend fun dispose()
}

/**
 * Result of NLU classification.
 */
sealed class NluResult {
    /**
     * A confident match was found.
     */
    data class Match(
        val command: QuantizedCommand,
        val confidence: Float,
        val intent: String? = null
    ) : NluResult()

    /**
     * Multiple potential matches with similar confidence.
     * UI should prompt for disambiguation.
     */
    data class Ambiguous(
        val candidates: List<Pair<QuantizedCommand, Float>>
    ) : NluResult()

    /**
     * No matching command found.
     */
    data object NoMatch : NluResult()

    /**
     * An error occurred during classification.
     */
    data class Error(val message: String) : NluResult()
}

/**
 * Configuration for NLU processing.
 */
data class NluConfig(
    /** Minimum confidence threshold for a match (0.0-1.0) */
    val confidenceThreshold: Float = 0.6f,
    /** Whether NLU is enabled */
    val enabled: Boolean = true,
    /** Path to ONNX model in assets */
    val modelPath: String = "models/nlu/malbert-intent-v1.onnx",
    /** Path to vocabulary file in assets */
    val vocabPath: String = "models/nlu/vocab.txt",
    /** Maximum sequence length for tokenization */
    val maxSequenceLength: Int = 64
) {
    companion object {
        val DEFAULT = NluConfig()
        val DISABLED = NluConfig(enabled = false)
    }
}
