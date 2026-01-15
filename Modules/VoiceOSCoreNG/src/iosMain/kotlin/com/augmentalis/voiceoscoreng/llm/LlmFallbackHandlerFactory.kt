/**
 * LlmFallbackHandlerFactory.kt - iOS LLM Fallback Handler Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * iOS implementation of LlmFallbackHandlerFactory.
 */
package com.augmentalis.voiceoscoreng.llm

/**
 * iOS implementation of [LlmFallbackHandlerFactory].
 *
 * Creates stub fallback handlers that indicate LLM is not available.
 */
actual object LlmFallbackHandlerFactory {
    /**
     * Create an iOS LLM fallback handler (stub).
     *
     * @param config Fallback configuration
     * @return IOSLlmFallbackHandler instance (stub)
     */
    actual fun create(config: FallbackConfig): ILlmFallbackHandler {
        return IOSLlmFallbackHandler(config)
    }
}
