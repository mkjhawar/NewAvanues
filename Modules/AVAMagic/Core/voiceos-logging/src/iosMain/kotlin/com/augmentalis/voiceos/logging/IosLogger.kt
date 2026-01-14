/**
 * IosLogger.kt - iOS-specific logger implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.logging

import platform.Foundation.NSLog

/**
 * iOS implementation of Logger using NSLog
 *
 * Features:
 * - Lazy evaluation (message lambda only called if logging enabled)
 * - Uses NSLog for native iOS logging
 * - Full exception stack traces
 */
class IosLogger(private val tag: String) : Logger {

    override fun v(message: () -> String) {
        if (isLoggable(LogLevel.VERBOSE)) {
            NSLog("[$tag] VERBOSE: ${message()}")
        }
    }

    override fun d(message: () -> String) {
        if (isLoggable(LogLevel.DEBUG)) {
            NSLog("[$tag] DEBUG: ${message()}")
        }
    }

    override fun i(message: () -> String) {
        if (isLoggable(LogLevel.INFO)) {
            NSLog("[$tag] INFO: ${message()}")
        }
    }

    override fun w(message: () -> String) {
        if (isLoggable(LogLevel.WARN)) {
            NSLog("[$tag] WARN: ${message()}")
        }
    }

    override fun e(message: () -> String) {
        if (isLoggable(LogLevel.ERROR)) {
            NSLog("[$tag] ERROR: ${message()}")
        }
    }

    override fun e(message: () -> String, throwable: Throwable) {
        if (isLoggable(LogLevel.ERROR)) {
            NSLog("[$tag] ERROR: ${message()}\n${throwable.stackTraceToString()}")
        }
    }

    override fun wtf(message: () -> String) {
        if (isLoggable(LogLevel.ASSERT)) {
            NSLog("[$tag] WTF: ${message()}")
        }
    }

    override fun isLoggable(level: LogLevel): Boolean {
        // iOS logs everything by default (can be filtered in Console.app)
        return true
    }
}
