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
import java.lang.ref.WeakReference

/**
 * Android implementation of [NluProcessorFactory].
 *
 * Uses WeakReference to prevent memory leaks from static Context storage.
 * The applicationContext is stored weakly; if garbage collected, the factory
 * must be re-initialized before creating processors.
 */
actual object NluProcessorFactory {
    /**
     * Weak reference to application context to prevent memory leaks.
     * Application context itself won't be GC'd, but this pattern ensures
     * we don't accidentally hold Activity/Service contexts.
     */
    private var contextRef: WeakReference<Context>? = null

    /**
     * Initialize the factory with application context.
     *
     * Must be called before [create], typically in Application.onCreate()
     * or when initializing VoiceOSCoreNG.
     *
     * @param context Application context (will be stored as weak reference to applicationContext)
     */
    fun initialize(context: Context) {
        contextRef = WeakReference(context.applicationContext)
    }

    /**
     * Clear the factory state.
     * Call when the application is being destroyed or during cleanup.
     */
    fun clear() {
        contextRef?.clear()
        contextRef = null
    }

    /**
     * Create an Android NLU processor.
     *
     * @param config NLU configuration
     * @return AndroidNluProcessor instance
     * @throws IllegalStateException if factory not initialized or context was garbage collected
     */
    actual fun create(config: NluConfig): INluProcessor {
        val ctx = contextRef?.get() ?: throw IllegalStateException(
            "NluProcessorFactory not initialized or context was garbage collected. " +
            "Call initialize(context) first."
        )
        return AndroidNluProcessor(ctx, config)
    }
}
