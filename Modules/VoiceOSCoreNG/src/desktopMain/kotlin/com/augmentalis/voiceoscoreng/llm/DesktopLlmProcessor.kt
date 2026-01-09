/**
 * DesktopLlmProcessor.kt - Desktop LLM Processor Stub
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Stub implementation for Desktop (JVM) platform.
 * TODO: Implement using llama.cpp JNI bindings for LLM inference.
 */
package com.augmentalis.voiceoscoreng.llm

/**
 * Desktop stub implementation of [ILlmProcessor].
 *
 * Currently returns NoMatch for all interpretations.
 * TODO: Implement using llama.cpp JNI bindings.
 */
class DesktopLlmProcessor(
    private val config: LlmConfig = LlmConfig.DEFAULT
) : ILlmProcessor {

    override suspend fun initialize(): Result<Unit> {
        // No-op for stub
        return Result.success(Unit)
    }

    override suspend fun interpretCommand(
        utterance: String,
        nluSchema: String,
        availableCommands: List<String>
    ): LlmResult {
        // Stub always returns NoMatch
        return LlmResult.NoMatch
    }

    override fun isAvailable(): Boolean = false

    override fun isModelLoaded(): Boolean = false

    override suspend fun dispose() {
        // No-op for stub
    }
}
