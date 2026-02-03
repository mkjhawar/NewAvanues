/**
 * AndroidLogger.kt - Android-specific logger implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 */
package com.avanues.logging

import android.util.Log

/**
 * Android implementation of Logger using android.util.Log
 *
 * Features:
 * - Lazy evaluation (message lambda only called if logging enabled)
 * - Respects both globalMinLevel and Android's Log.isLoggable() settings
 * - Full exception stack traces
 */
internal class AndroidLogger(private val tag: String) : Logger {

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
        // Check global min level first, then Android's Log.isLoggable
        return level.priority >= globalMinLevel.priority && Log.isLoggable(tag, level.priority)
    }
}
