package com.augmentalis.ava.platform

import platform.Foundation.NSLog

/**
 * iOS implementation of Logger using NSLog.
 */
actual object Logger {
    private var minLevel: LogLevel = LogLevel.VERBOSE

    actual fun v(tag: String, message: String) {
        if (shouldLog(LogLevel.VERBOSE)) {
            NSLog("V/$tag: $message")
        }
    }

    actual fun d(tag: String, message: String) {
        if (shouldLog(LogLevel.DEBUG)) {
            NSLog("D/$tag: $message")
        }
    }

    actual fun i(tag: String, message: String) {
        if (shouldLog(LogLevel.INFO)) {
            NSLog("I/$tag: $message")
        }
    }

    actual fun w(tag: String, message: String) {
        if (shouldLog(LogLevel.WARN)) {
            NSLog("W/$tag: $message")
        }
    }

    actual fun w(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.WARN)) {
            NSLog("W/$tag: $message")
            NSLog("W/$tag: ${throwable.stackTraceToString()}")
        }
    }

    actual fun e(tag: String, message: String) {
        if (shouldLog(LogLevel.ERROR)) {
            NSLog("E/$tag: $message")
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(LogLevel.ERROR)) {
            NSLog("E/$tag: $message")
            NSLog("E/$tag: ${throwable.stackTraceToString()}")
        }
    }

    actual fun setMinLevel(level: LogLevel) {
        minLevel = level
    }

    private fun shouldLog(level: LogLevel): Boolean {
        return level.ordinal >= minLevel.ordinal
    }
}
