/**
 * IOSLlmFallbackHandler.kt - iOS LLM Fallback Handler Stub
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Stub implementation for iOS platform.
 * TODO: Implement using llama.cpp or CoreML for LLM fallback.
 */
package com.augmentalis.voiceoscoreng.llm

import com.augmentalis.voiceoscoreng.nlu.NluResult
import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * iOS stub implementation of [ILlmFallbackHandler].
 *
 * This is a placeholder that always returns NoMatch.
 * The stub reports itself as NOT available via [isAvailable],
 * indicating that LLM fallback functionality is not implemented on this platform.
 *
 * TODO: Implement using llama.cpp or MLX for on-device LLM inference.
 */
class IOSLlmFallbackHandler(
    private var config: FallbackConfig = FallbackConfig.DEFAULT
) : ILlmFallbackHandler {

    private var initialized = false

    override suspend fun initialize(): Result<Unit> {
        // Stub initialization always succeeds but does nothing
        // isAvailable() will still return false to indicate no real LLM capability
        initialized = true
        println("[IOSLlmFallbackHandler] Stub initialized - LLM fallback not available on iOS yet")
        return Result.success(Unit)
    }

    override suspend fun handleLowConfidence(
        utterance: String,
        nluResult: NluResult,
        candidateCommands: List<QuantizedCommand>
    ): FallbackResult {
        return FallbackResult.NoMatch(
            reason = "LLM fallback not available on iOS",
            attemptedProviders = emptyList()
        )
    }

    override suspend fun handleAmbiguous(
        utterance: String,
        ambiguousResult: NluResult.Ambiguous
    ): FallbackResult {
        // Return the highest confidence candidate as a fallback
        val bestCandidate = ambiguousResult.candidates.maxByOrNull { it.second }
        return if (bestCandidate != null) {
            FallbackResult.Clarified(
                command = bestCandidate.first,
                confidence = bestCandidate.second * 0.9f, // Slight penalty for ambiguity
                source = "heuristic",
                explanation = "Selected highest confidence candidate (LLM not available on iOS)"
            )
        } else {
            FallbackResult.NoMatch(
                reason = "No candidates available",
                attemptedProviders = emptyList()
            )
        }
    }

    override suspend fun generateAction(
        utterance: String,
        context: String?
    ): FallbackResult {
        return FallbackResult.NoMatch(
            reason = "Action generation not available on iOS",
            attemptedProviders = emptyList()
        )
    }

    /**
     * Returns false because LLM is not actually available on iOS.
     */
    override fun isLocalAvailable(): Boolean = false

    /**
     * Returns false because cloud LLM is not implemented on iOS.
     */
    override fun isCloudAvailable(): Boolean = false

    override fun getConfig(): FallbackConfig = config

    override fun setConfig(config: FallbackConfig) {
        this.config = config
    }

    override suspend fun dispose() {
        initialized = false
    }
}
