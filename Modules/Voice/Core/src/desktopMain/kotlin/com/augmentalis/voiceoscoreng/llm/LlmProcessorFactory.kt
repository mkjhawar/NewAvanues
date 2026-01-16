/**
 * LlmProcessorFactory.kt - Desktop LLM Processor Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Desktop (JVM) implementation of LlmProcessorFactory.
 */
package com.augmentalis.voiceoscoreng.llm

/**
 * Desktop implementation of [LlmProcessorFactory].
 */
actual object LlmProcessorFactory {
    /**
     * Create a Desktop LLM processor (stub).
     *
     * @param config LLM configuration
     * @return DesktopLlmProcessor stub instance
     */
    actual fun create(config: LlmConfig): ILlmProcessor {
        return DesktopLlmProcessor(config)
    }
}
