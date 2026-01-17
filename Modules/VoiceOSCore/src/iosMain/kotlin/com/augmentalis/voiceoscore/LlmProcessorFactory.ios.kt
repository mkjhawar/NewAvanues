/**
 * LlmProcessorFactory.ios.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * iOS implementation of LLM processor factory.
 * TODO: Integrate with llama.cpp for on-device LLM.
 */
package com.augmentalis.voiceoscore

/**
 * iOS LLM processor factory implementation.
 *
 * Creates stub LLM processor until llama.cpp integration is complete.
 */
actual object LlmProcessorFactory {
    /**
     * Create iOS-specific LLM processor.
     *
     * @param config LLM configuration
     * @return ILlmProcessor implementation (stub for now)
     */
    actual fun create(config: LlmConfig): ILlmProcessor = StubLlmProcessorIOS(config)
}

/**
 * Stub LLM processor for iOS.
 * Returns NoMatch for all interpretations until real implementation.
 */
internal class StubLlmProcessorIOS(private val config: LlmConfig) : ILlmProcessor {
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
