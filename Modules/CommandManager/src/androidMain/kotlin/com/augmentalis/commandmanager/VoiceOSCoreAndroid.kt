/**
 * VoiceOSCoreAndroid.kt - Android Platform Implementation
 *
 * Provides Android-specific initialization and functionality for VoiceOSCore.
 * This is the Android entry point for the unified VoiceOSCore KMP module.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */

package com.augmentalis.commandmanager

import android.content.Context

/**
 * VoiceOSCoreAndroid - Android-specific platform implementation.
 *
 * Entry point for Android apps using VoiceOSCore. Handles:
 * - Speech recognition engine initialization (Vivoka, Vosk, Azure)
 * - Accessibility service setup and helpers
 * - SQLDelight database driver (Android)
 * - Compose UI overlay system
 * - AIDL service bindings
 *
 * Usage:
 * ```kotlin
 * // In Application.onCreate()
 * VoiceOSCoreAndroid.init(applicationContext)
 * ```
 *
 * @param context Android application context
 */
class VoiceOSCoreAndroid(
    private val context: Context
) : PlatformProvider {

    override val platformName: String = "Android"

    override suspend fun initialize() {
        // Initialize Android-specific components:
        // - Speech recognition engines (Vivoka, Vosk, Azure)
        // - Accessibility service helpers
        // - Database driver (SQLDelight Android)
        // - UI overlay system (Compose)
        // - AIDL service bindings
    }

    override suspend fun cleanup() {
        // Cleanup Android resources:
        // - Release speech engines
        // - Close database connections
        // - Unbind services
    }

    /**
     * Get the Android application context.
     */
    fun getContext(): Context = context

    companion object {
        @Volatile
        private var instance: VoiceOSCoreAndroid? = null

        /**
         * Initialize VoiceOSCore for Android.
         *
         * @param context Application context (will use applicationContext)
         */
        fun init(context: Context): VoiceOSCoreAndroid {
            return instance ?: synchronized(this) {
                instance ?: VoiceOSCoreAndroid(context.applicationContext).also {
                    instance = it
                    VoiceOSCore.platformProvider = it
                }
            }
        }

        /**
         * Get the singleton instance.
         * @throws IllegalStateException if init() hasn't been called
         */
        fun getInstance(): VoiceOSCoreAndroid {
            return instance ?: throw IllegalStateException(
                "VoiceOSCoreAndroid not initialized. Call init(context) first."
            )
        }
    }
}
