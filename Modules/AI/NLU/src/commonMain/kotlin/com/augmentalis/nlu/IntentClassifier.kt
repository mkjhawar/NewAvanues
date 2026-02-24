// filename: features/nlu/src/commonMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt
// created: 2025-11-02
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - KMP Migration

package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result

/**
 * Intent classification result
 */
data class IntentClassification(
    val intent: String,
    val confidence: Float,
    val inferenceTimeMs: Long,
    val allScores: Map<String, Float> = emptyMap()
)

/**
 * Cross-platform intent classifier interface
 *
 * Platform-specific implementations:
 * - Android: ONNX Runtime Mobile with MobileBERT/mALBERT
 * - iOS: Core ML or ONNX Runtime iOS (future)
 * - Desktop: ONNX Runtime Desktop (future)
 * - Web: TensorFlow.js (future)
 */
expect class IntentClassifier {
    /**
     * Initialize the classifier with model path
     * @param modelPath Path to the model file (platform-specific)
     * @return Result indicating success or failure
     */
    suspend fun initialize(modelPath: String): Result<Unit>

    /**
     * Classify user utterance into one of the candidate intents
     * @param utterance User input text
     * @param candidateIntents List of possible intents
     * @return Result with classification and confidence
     */
    suspend fun classifyIntent(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification>

    /**
     * Clean up resources
     */
    fun close()

    /**
     * Get all intents that have been loaded during initialization
     *
     * Returns the intent IDs for which embeddings have been pre-computed.
     * This includes all intents from .ava files loaded by IntentSourceCoordinator.
     *
     * @return List of loaded intent IDs, or empty list if not initialized
     */
    fun getLoadedIntents(): List<String>

    /**
     * Classify a voice command utterance against known command phrases.
     *
     * VoiceOS Integration:
     * This method is designed for VoiceOS command matching, supporting:
     * - Multiple matching strategies (exact, fuzzy, semantic, hybrid)
     * - Ambiguity detection when multiple commands score similarly
     * - Confidence thresholds for reliable command execution
     *
     * Algorithm:
     * 1. First attempts exact/fuzzy matching for performance
     * 2. Falls back to semantic similarity if no exact match
     * 3. Detects ambiguity when top candidates are within threshold
     * 4. Returns NoMatch if best score below confidence threshold
     *
     * @param utterance User's spoken command (e.g., "turn up the brightness")
     * @param commandPhrases List of known command phrases to match against
     * @param confidenceThreshold Minimum confidence for a valid match (default: 0.6)
     * @param ambiguityThreshold Max difference between top scores to be ambiguous (default: 0.15)
     * @return CommandClassificationResult indicating match, ambiguity, no match, or error
     *
     * @see CommandClassificationResult for possible return types
     */
    suspend fun classifyCommand(
        utterance: String,
        commandPhrases: List<String>,
        confidenceThreshold: Float = NluThresholds.SEMANTIC_CONFIDENCE_THRESHOLD,
        ambiguityThreshold: Float = NluThresholds.DEFAULT_AMBIGUITY_THRESHOLD
    ): CommandClassificationResult

    companion object {
        /**
         * Get singleton instance of IntentClassifier
         * @param context Platform-specific context (Any to be type-safe across platforms)
         * @return IntentClassifier instance
         */
        fun getInstance(context: Any): IntentClassifier
    }
}
