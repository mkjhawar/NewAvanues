/**
 * IOSLlmProcessor.kt - iOS LLM Processor Stub
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Stub implementation for iOS platform.
 * TODO: Implement using llama.cpp or CoreML for LLM inference.
 */
package com.augmentalis.voiceoscoreng.llm

/**
 * iOS stub implementation of [ILlmProcessor].
 *
 * This is a placeholder that always returns NoMatch.
 * The stub reports itself as NOT available via [isAvailable],
 * indicating that LLM functionality is not implemented on this platform.
 *
 * TODO: Implement using llama.cpp or MLX for on-device LLM inference.
 */
class IOSLlmProcessor(
    private val config: LlmConfig = LlmConfig.DEFAULT
) : ILlmProcessor {

    private var initialized = false

    override suspend fun initialize(): Result<Unit> {
        // Stub initialization always succeeds but does nothing
        // isAvailable() will still return false to indicate no real LLM capability
        initialized = true
        println("[IOSLlmProcessor] Stub initialized - LLM not available on iOS yet")
        return Result.success(Unit)
    }

    override suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult {
        // Stub always returns NoMatch - no LLM capability on iOS yet
        return LlmResult.NoMatch
    }

    /**
     * Returns false because LLM is not actually available on iOS.
     * Callers should check this and skip LLM fallback.
     */
    override fun isAvailable(): Boolean = false

    override fun isModelLoaded(): Boolean = false

    override suspend fun dispose() {
        initialized = false
    }
}
