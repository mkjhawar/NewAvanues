/**
 * VivokaEngineFactory.kt - Android Vivoka factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Android implementation of Vivoka engine factory.
 * Creates real Vivoka engine when SDK is available.
 */
package com.augmentalis.voiceoscoreng.speech.vivoka

import android.content.Context

/**
 * Android Vivoka engine factory.
 */
actual object VivokaEngineFactory {

    private var applicationContext: Context? = null
    private var vivokaAvailable: Boolean? = null

    /**
     * Initialize with application context.
     * Must be called before create().
     */
    fun initialize(context: Context) {
        applicationContext = context.applicationContext
        vivokaAvailable = checkVivokaAvailability()
    }

    /**
     * Check if Vivoka SDK is available.
     */
    actual fun isAvailable(): Boolean {
        if (vivokaAvailable == null) {
            vivokaAvailable = checkVivokaAvailability()
        }
        return vivokaAvailable == true
    }

    /**
     * Create Vivoka engine instance.
     */
    actual fun create(config: VivokaConfig): IVivokaEngine {
        val context = applicationContext
            ?: throw IllegalStateException("VivokaEngineFactory not initialized. Call initialize(context) first.")

        return if (isAvailable()) {
            AndroidVivokaEngine(context, config)
        } else {
            // Return stub if Vivoka SDK not available
            StubVivokaEngine("Vivoka SDK not available on this device")
        }
    }

    /**
     * Check if Vivoka SDK classes are available.
     */
    private fun checkVivokaAvailability(): Boolean {
        return try {
            // Try to load Vivoka SDK class
            Class.forName("com.vivoka.asr.VoiceRecognizer")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}
