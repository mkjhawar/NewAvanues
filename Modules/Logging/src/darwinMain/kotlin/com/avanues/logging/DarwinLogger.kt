/**
 * DarwinLogger.kt - Apple platform logger implementation (iOS + macOS)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 * Updated: 2026-02-25 (renamed from IosLogger, moved to darwinMain for macOS support)
 */
package com.avanues.logging

import platform.Foundation.NSLog

/**
 * Apple/Darwin implementation of Logger using NSLog.
 *
 * Works on all Apple platforms (iOS, macOS, tvOS, watchOS) since NSLog
 * is part of the Foundation framework available across all Darwin targets.
 *
 * Features:
 * - Lazy evaluation (message lambda only called if logging enabled)
 * - Uses NSLog for native Apple platform logging
 * - Respects globalMinLevel setting
 * - Full exception stack traces
 */
internal class DarwinLogger(private val tag: String) : Logger {

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
        return level.priority >= globalMinLevel.priority
    }
}
