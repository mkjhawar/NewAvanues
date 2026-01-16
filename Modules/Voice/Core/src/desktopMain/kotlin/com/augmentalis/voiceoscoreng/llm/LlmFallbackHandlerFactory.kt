/**
 * LlmFallbackHandlerFactory.kt - Desktop LLM Fallback Handler Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-16
 *
 * Desktop implementation of LlmFallbackHandlerFactory.
 */
package com.augmentalis.voiceoscoreng.llm

/**
 * Desktop implementation of [LlmFallbackHandlerFactory].
 *
 * Creates stub fallback handlers that indicate LLM is not available.
 */
actual object LlmFallbackHandlerFactory {
    /**
     * Create a Desktop LLM fallback handler (stub).
     *
     * @param config Fallback configuration
     * @return DesktopLlmFallbackHandler instance (stub)
     */
    actual fun create(config: FallbackConfig): ILlmFallbackHandler {
        return DesktopLlmFallbackHandler(config)
    }
}
