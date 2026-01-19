/**
 * VivokaEngineFactory.android.kt - Android actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 * Updated: 2026-01-19 - Add Context provider pattern for Android Context injection
 *
 * Android implementation of Vivoka engine factory.
 * Uses Context provider pattern for KMP-compatible Android Context injection.
 */
package com.augmentalis.voiceoscore

import android.content.Context

/**
 * Android Vivoka engine factory implementation.
 *
 * Uses a Context provider pattern to inject Android Context into the factory.
 * The app module must call [initialize] with Application context before creating engines.
 *
 * Usage in Application.onCreate():
 * ```kotlin
 * VivokaEngineFactory.initialize(applicationContext)
 * ```
 */
actual object VivokaEngineFactory {

    private var applicationContext: Context? = null

    /**
     * Initialize the factory with Android Application context.
     * Must be called once from Application.onCreate() before creating engines.
     *
     * @param context Application context (will be stored as applicationContext)
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    /**
     * Check if Vivoka is available on Android.
     * Returns true if Context is initialized and Vivoka SDK classes are available.
     */
    actual fun isAvailable(): Boolean {
        if (applicationContext == null) return false

        // Check if Vivoka SDK is available via reflection
        return try {
            Class.forName("com.vivoka.vsdk.asr.csdk.recognizer.Recognizer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Create Android-specific Vivoka engine.
     *
     * @param config Vivoka configuration
     * @return IVivokaEngine implementation (VivokaAndroidEngine if available, stub otherwise)
     * @throws IllegalStateException if initialize() was not called
     */
    actual fun create(config: VivokaConfig): IVivokaEngine {
        val context = applicationContext
            ?: throw IllegalStateException(
                "VivokaEngineFactory not initialized. " +
                "Call VivokaEngineFactory.initialize(context) in Application.onCreate()"
            )

        return if (isAvailable()) {
            VivokaAndroidEngine(context, config)
        } else {
            StubVivokaEngine("Vivoka SDK not available on Android")
        }
    }

    /**
     * Create engine with explicit context (for cases where app context isn't set).
     * Useful for testing or late initialization.
     *
     * @param context Android context
     * @param config Vivoka configuration
     * @return IVivokaEngine implementation
     */
    fun createWithContext(context: Context, config: VivokaConfig): IVivokaEngine {
        val appContext = context.applicationContext

        // Also store it for future calls
        if (applicationContext == null) {
            applicationContext = appContext
        }

        return if (isAvailable()) {
            VivokaAndroidEngine(appContext, config)
        } else {
            StubVivokaEngine("Vivoka SDK not available on Android")
        }
    }
}
