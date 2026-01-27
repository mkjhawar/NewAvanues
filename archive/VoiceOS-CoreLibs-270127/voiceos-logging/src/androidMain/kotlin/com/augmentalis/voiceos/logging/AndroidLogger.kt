/**
 * AndroidLogger.kt - Android-specific logger implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.logging

import android.util.Log

/**
 * Android implementation of Logger using android.util.Log
 *
 * Features:
 * - Lazy evaluation (message lambda only called if logging enabled)
 * - Respects Android's Log.isLoggable() settings
 * - Full exception stack traces
 */
class AndroidLogger(private val tag: String) : Logger {

    override fun v(message: () -> String) {
        if (isLoggable(LogLevel.VERBOSE)) {
            Log.v(tag, message())
        }
    }

    override fun d(message: () -> String) {
        if (isLoggable(LogLevel.DEBUG)) {
            Log.d(tag, message())
        }
    }

    override fun i(message: () -> String) {
        if (isLoggable(LogLevel.INFO)) {
            Log.i(tag, message())
        }
    }

    override fun w(message: () -> String) {
        if (isLoggable(LogLevel.WARN)) {
            Log.w(tag, message())
        }
    }

    override fun e(message: () -> String) {
        if (isLoggable(LogLevel.ERROR)) {
            Log.e(tag, message())
        }
    }

    override fun e(message: () -> String, throwable: Throwable) {
        if (isLoggable(LogLevel.ERROR)) {
            Log.e(tag, message(), throwable)
        }
    }

    override fun wtf(message: () -> String) {
        if (isLoggable(LogLevel.ASSERT)) {
            Log.wtf(tag, message())
        }
    }

    override fun isLoggable(level: LogLevel): Boolean {
        return Log.isLoggable(tag, level.priority)
    }
}
