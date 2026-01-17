/**
 * LlmProcessorFactory.kt - Factory for creating LLM processors
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Platform-specific factory for creating ILlmProcessor instances.
 */
package com.augmentalis.voiceoscore

/**
 * Factory for creating platform-specific LLM processors.
 *
 * Platform implementations:
 * - Android: AndroidLlmProcessor (wraps LocalLLMProvider)
 * - iOS: IOSLlmProcessor (stub, TODO: llama.cpp)
 * - Desktop: DesktopLlmProcessor (stub, TODO: llama.cpp JNI)
 */
expect object LlmProcessorFactory {
    /**
     * Create a platform-specific LLM processor.
     *
     * @param config LLM configuration
     * @return Platform-specific ILlmProcessor implementation
     */
    fun create(config: LlmConfig = LlmConfig.DEFAULT): ILlmProcessor
}
