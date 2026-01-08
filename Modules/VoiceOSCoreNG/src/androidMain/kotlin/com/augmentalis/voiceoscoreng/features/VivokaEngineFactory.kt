/**
 * VivokaEngineFactory.kt - Android Vivoka factory
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-07 - Direct SDK check (no reflection)
 *
 * Android implementation of Vivoka engine factory.
 * Creates real Vivoka engine when SDK is available.
 * Uses direct SDK class import to verify availability.
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
     * Check if Vivoka SDK classes are available.
     * SDK is included via Vivoka AAR dependencies in build.gradle.kts.
     */
    private fun checkVivokaAvailability(): Boolean {
        return try {
            // Direct class reference - SDK is compiled in via SpeechRecognition library
            // This will fail at class load time if SDK is not properly linked
            Class.forName("com.vivoka.vsdk.Vsdk")
            true
        } catch (e: ClassNotFoundException) {
            false
        } catch (e: NoClassDefFoundError) {
            false
        }
    }
}
