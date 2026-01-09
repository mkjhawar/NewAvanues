/**
 * LlmProcessorFactory.kt - Android LLM Processor Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Android implementation of LlmProcessorFactory.
 */
package com.augmentalis.voiceoscoreng.llm

import android.content.Context

/**
 * Android implementation of [LlmProcessorFactory].
 *
 * Must be initialized with application context before use.
 */
actual object LlmProcessorFactory {
    private var context: Context? = null

    /**
     * Initialize the factory with application context.
     *
     * Must be called before [create], typically in Application.onCreate()
     * or when initializing VoiceOSCoreNG.
     *
     * @param context Application context (will be stored as applicationContext)
     */
    fun initialize(context: Context) {
        this.context = context.applicationContext
    }

    /**
     * Create an Android LLM processor.
     *
     * @param config LLM configuration
     * @return AndroidLlmProcessor instance
     * @throws IllegalStateException if factory not initialized
     */
    actual fun create(config: LlmConfig): ILlmProcessor {
        val ctx = context ?: throw IllegalStateException(
            "LlmProcessorFactory not initialized. Call initialize(context) first."
        )
        return AndroidLlmProcessor(ctx, config)
    }
}
