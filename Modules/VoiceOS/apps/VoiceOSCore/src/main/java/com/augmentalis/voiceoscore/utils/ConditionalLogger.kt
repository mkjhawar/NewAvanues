/**
 * ConditionalLogger.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.utils

import android.util.Log

/**
 * Conditional Logger
 *
 * Provides conditional logging based on configuration
 */
object ConditionalLogger {
    private var isEnabled = true
    private var verboseEnabled = false

    /**
     * Set logging enabled state
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    /**
     * Set verbose logging enabled state
     */
    fun setVerboseEnabled(enabled: Boolean) {
        verboseEnabled = enabled
    }

    /**
     * Log debug message
     */
    fun d(tag: String, message: String) {
        if (isEnabled) {
            Log.d(tag, message)
        }
    }

    /**
     * Log info message
     */
    fun i(tag: String, message: String) {
        if (isEnabled) {
            Log.i(tag, message)
        }
    }

    /**
     * Log warning message
     */
    fun w(tag: String, message: String) {
        if (isEnabled) {
            Log.w(tag, message)
        }
    }

    /**
     * Log error message
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (isEnabled) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    /**
     * Log verbose message
     */
    fun v(tag: String, message: String) {
        if (isEnabled && verboseEnabled) {
            Log.v(tag, message)
        }
    }
}
