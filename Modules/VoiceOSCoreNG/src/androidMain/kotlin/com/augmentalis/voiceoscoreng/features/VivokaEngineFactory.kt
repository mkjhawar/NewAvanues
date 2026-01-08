/**
 * VivokaEngineFactory.kt - Android Vivoka factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-08 - SDK bundled via VivokaSDK wrapper module
 *
 * Android implementation of Vivoka engine factory.
 * Creates real Vivoka engine - SDK is always available (bundled).
 * Runtime errors (missing models) handled during initialization.
 */
package com.augmentalis.voiceoscoreng.features

import android.content.Context

/**
 * Android Vivoka engine factory.
 *
 * SDK is compiled into APK via SpeechRecognition library.
 * Models are loaded from external storage at runtime.
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
     * SDK is linked at compile time via SpeechRecognition library.
     */
    actual fun isAvailable(): Boolean {
        if (vivokaAvailable == null) {
            vivokaAvailable = checkVivokaAvailability()
        }
        return vivokaAvailable == true
    }

    /**
     * Create Vivoka engine instance.
     *
     * @return AndroidVivokaEngine if SDK available, StubVivokaEngine otherwise.
     *         Note: Engine may still need models - call checkModelStatus() before initialize().
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
     * Create Vivoka engine and check model status.
     * Use this when you need to know if models are ready before initialization.
     *
     * @return Pair of engine and model status
     */
    fun createWithModelCheck(config: VivokaConfig): Pair<IVivokaEngine, VivokaModelStatus> {
        val engine = create(config)
        val status = if (engine is AndroidVivokaEngine) {
            engine.checkModelStatus()
        } else {
            VivokaModelStatus.Error("Vivoka SDK not available")
        }
        return Pair(engine, status)
    }

    /**
     * Check if Vivoka SDK is available.
     * SDK is bundled via VivokaSDK wrapper module (implementation dependency).
     * Always returns true since SDK is compiled into the APK.
     * Runtime errors (missing models, native lib issues) are handled during initialization.
     */
    private fun checkVivokaAvailability(): Boolean {
        // SDK is always available - bundled via implementation dependency
        // Any runtime issues (models, native libs) are caught during engine initialization
        return true
    }
}
