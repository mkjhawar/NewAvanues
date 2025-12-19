/**
 * VoiceOSApplication.kt - Application class for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos

import android.app.Application
import android.util.Log
import com.augmentalis.voiceos.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

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

        // Initialize Koin for dependency injection
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@VoiceOSApplication)
            modules(appModule)
        }

        Log.i(TAG, "Koin dependency injection initialized")
    }
}
