/**
 * LlmFallbackHandlerFactory.ios.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * iOS implementation of LLM fallback handler factory.
 * TODO: Integrate with llama.cpp for on-device LLM.
 */
package com.augmentalis.voiceoscore

/**
 * iOS LLM fallback handler factory implementation.
 *
 * Creates stub handler until llama.cpp integration is complete.
 */
actual object LlmFallbackHandlerFactory {
    /**
     * Create iOS-specific LLM fallback handler.
     *
     * @param config Fallback configuration
     * @return ILlmFallbackHandler implementation (stub for now)
     */
    actual fun create(config: FallbackConfig): ILlmFallbackHandler = StubLlmFallbackHandlerIOS(config)
}

/**
 * Stub LLM fallback handler for iOS.
 * Returns NoMatch for all operations until real implementation.
 */
internal class StubLlmFallbackHandlerIOS(private var config: FallbackConfig) : ILlmFallbackHandler {
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
        return FallbackResult.NoMatch(
            reason = "LLM fallback not implemented on iOS",
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
        return FallbackResult.NoMatch(
            reason = "LLM disambiguation not implemented on iOS",
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
        return FallbackResult.NoMatch(
            reason = "LLM action generation not implemented on iOS",
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
