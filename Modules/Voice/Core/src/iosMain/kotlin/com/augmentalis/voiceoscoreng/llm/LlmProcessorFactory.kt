/**
 * LlmProcessorFactory.kt - iOS LLM Processor Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * iOS implementation of LlmProcessorFactory.
 */
package com.augmentalis.voiceoscoreng.llm

/**
 * iOS implementation of [LlmProcessorFactory].
 */
actual object LlmProcessorFactory {
    /**
     * Create an iOS LLM processor (stub).
     *
     * @param config LLM configuration
     * @return IOSLlmProcessor stub instance
     */
    actual fun create(config: LlmConfig): ILlmProcessor {
        return IOSLlmProcessor(config)
    }
}
