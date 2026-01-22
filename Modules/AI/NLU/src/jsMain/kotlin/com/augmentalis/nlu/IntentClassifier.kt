// filename: features/nlu/src/jsMain/kotlin/com/augmentalis/ava/features/nlu/IntentClassifier.kt
// created: 2025-11-02
// author: Claude Code
// © Augmentalis Inc, Intelligent Devices LLC
// TCR: Phase 1 - KMP Migration (JS/Web stub)

package com.augmentalis.nlu

import com.augmentalis.ava.core.common.Result

/**
 * JS/Web stub implementation of IntentClassifier
 *
 * TODO Phase 2: Implement using TensorFlow.js
 */
actual class IntentClassifier private constructor() {

    actual suspend fun initialize(modelPath: String): Result<Unit> {
        return Result.Error(
            exception = NotImplementedError("Web NLU not yet implemented"),
            message = "Intent classification not available on Web yet. Will be implemented in Phase 2 using TensorFlow.js."
        )
    }

    actual suspend fun classifyIntent(
        utterance: String,
        candidateIntents: List<String>
    ): Result<IntentClassification> {
        return Result.Error(
            exception = NotImplementedError("Web NLU not yet implemented"),
            message = "Intent classification not available on Web yet"
        )
    }

    actual fun close() {
        // No-op for stub
    }

    actual fun getLoadedIntents(): List<String> {
        return emptyList() // Stub - not implemented for Web yet
    }

    /**
     * Classify a voice command utterance against known command phrases.
     *
     * TODO Phase 2: Implement using TensorFlow.js
     */
    actual suspend fun classifyCommand(
        utterance: String,
        commandPhrases: List<String>,
        confidenceThreshold: Float,
        ambiguityThreshold: Float
    ): CommandClassificationResult {
        return CommandClassificationResult.Error(
            "Command classification not available on Web yet. Will be implemented in Phase 2 using TensorFlow.js."
        )
    }

    actual companion object {
        private var INSTANCE: IntentClassifier? = null

        actual fun getInstance(context: Any): IntentClassifier {
            return INSTANCE ?: IntentClassifier().also {
                INSTANCE = it
            }
        }
    }
}
