/**
 * LlmProcessorFactory.android.kt - Android actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * Android implementation of LLM processor factory.
 * TODO: Integrate with LocalLLMProvider from Modules/LLM.
 */
package com.augmentalis.voiceoscore

/**
 * Android LLM processor factory implementation.
 *
 * Creates stub LLM processor until LocalLLMProvider integration is complete.
 */
actual object LlmProcessorFactory {
    /**
     * Create Android-specific LLM processor.
     *
     * @param config LLM configuration
     * @return ILlmProcessor implementation (stub for now)
     */
    actual fun create(config: LlmConfig): ILlmProcessor = StubLlmProcessor(config)
}

/**
 * Stub LLM processor for Android.
 * Returns NoMatch for all interpretations until real implementation.
 */
internal class StubLlmProcessor(private val config: LlmConfig) : ILlmProcessor {
    private var initialized = false

    override suspend fun initialize(): Result<Unit> {
        initialized = config.enabled
        return Result.success(Unit)
    }

    override suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult {
        if (!initialized || !config.enabled) {
            return LlmResult.Error("LLM processor not initialized or disabled")
        }
        // Stub: Always return NoMatch
        return LlmResult.NoMatch
    }

    override suspend fun clarifyCommand(
        utterance: String,
        candidates: List<String>
    ): LlmResult {
        if (!initialized || !config.enabled) {
            return LlmResult.Error("LLM processor not initialized or disabled")
        }
        // Stub: Always return NoMatch
        return LlmResult.NoMatch
    }

    override fun isAvailable(): Boolean = initialized && config.enabled

    override fun isModelLoaded(): Boolean = initialized && config.enabled

    override suspend fun dispose() {
        initialized = false
    }
}
