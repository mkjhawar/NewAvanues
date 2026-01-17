/**
 * LlmFallbackHandlerFactory.android.kt - Android actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * Android implementation of LLM fallback handler factory.
 * TODO: Integrate with LocalLLMProvider and CloudLLMProvider from Modules/LLM.
 */
package com.augmentalis.voiceoscore

/**
 * Android LLM fallback handler factory implementation.
 *
 * Creates stub handler until LLM provider integration is complete.
 */
actual object LlmFallbackHandlerFactory {
    /**
     * Create Android-specific LLM fallback handler.
     *
     * @param config Fallback configuration
     * @return ILlmFallbackHandler implementation (stub for now)
     */
    actual fun create(config: FallbackConfig): ILlmFallbackHandler = StubLlmFallbackHandler(config)
}

/**
 * Stub LLM fallback handler for Android.
 * Returns NoMatch for all operations until real implementation.
 */
internal class StubLlmFallbackHandler(private var config: FallbackConfig) : ILlmFallbackHandler {
    private var initialized = false

    override suspend fun initialize(): Result<Unit> {
        initialized = config.enabled
        return Result.success(Unit)
    }

    override suspend fun handleLowConfidence(
        utterance: String,
        nluResult: NluResult,
        candidateCommands: List<QuantizedCommand>
    ): FallbackResult {
        if (!initialized || !config.enabled) {
            return FallbackResult.Error("LLM fallback handler not initialized or disabled")
        }
        // Stub: Always return NoMatch
        return FallbackResult.NoMatch(
            reason = "LLM fallback not implemented",
            attemptedProviders = emptyList()
        )
    }

    override suspend fun handleAmbiguous(
        utterance: String,
        ambiguousResult: NluResult.Ambiguous
    ): FallbackResult {
        if (!initialized || !config.enabled) {
            return FallbackResult.Error("LLM fallback handler not initialized or disabled")
        }
        // Stub: Return first candidate if available
        return FallbackResult.NoMatch(
            reason = "LLM disambiguation not implemented",
            attemptedProviders = emptyList()
        )
    }

    override suspend fun generateAction(
        utterance: String,
        context: String?
    ): FallbackResult {
        if (!initialized || !config.enabled) {
            return FallbackResult.Error("LLM fallback handler not initialized or disabled")
        }
        // Stub: Always return NoMatch
        return FallbackResult.NoMatch(
            reason = "LLM action generation not implemented",
            attemptedProviders = emptyList()
        )
    }

    override fun isLocalAvailable(): Boolean = false

    override fun isCloudAvailable(): Boolean = false

    override fun getConfig(): FallbackConfig = config

    override fun setConfig(config: FallbackConfig) {
        this.config = config
    }

    override suspend fun dispose() {
        initialized = false
    }
}
