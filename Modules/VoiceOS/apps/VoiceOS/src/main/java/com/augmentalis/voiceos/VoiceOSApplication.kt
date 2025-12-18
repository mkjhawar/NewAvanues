/**
 * VoiceOSApplication.kt - Application class for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos

import android.app.Application
import android.util.Log

/**
 * Main Application class for VoiceOS.
 *
 * Initializes core components and provides application-wide resources.
 */
class VoiceOSApplication : Application() {

    companion object {
        private const val TAG = "VoiceOSApplication"

        @Volatile
        private var instance: VoiceOSApplication? = null

        fun getInstance(): VoiceOSApplication {
            return instance ?: throw IllegalStateException("VoiceOSApplication not initialized")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        Log.i(TAG, "VoiceOS Application started")
    }
}
