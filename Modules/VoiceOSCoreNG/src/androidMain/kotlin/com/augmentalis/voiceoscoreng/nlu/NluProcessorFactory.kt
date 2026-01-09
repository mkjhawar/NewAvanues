/**
 * NluProcessorFactory.kt - Android NLU Processor Factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Android implementation of NluProcessorFactory.
 */
package com.augmentalis.voiceoscoreng.nlu

import android.content.Context

/**
 * Android implementation of [NluProcessorFactory].
 *
 * Must be initialized with application context before use.
 */
actual object NluProcessorFactory {
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
     * Create an Android NLU processor.
     *
     * @param config NLU configuration
     * @return AndroidNluProcessor instance
     * @throws IllegalStateException if factory not initialized
     */
    actual fun create(config: NluConfig): INluProcessor {
        val ctx = context ?: throw IllegalStateException(
            "NluProcessorFactory not initialized. Call initialize(context) first."
        )
        return AndroidNluProcessor(ctx, config)
    }
}
