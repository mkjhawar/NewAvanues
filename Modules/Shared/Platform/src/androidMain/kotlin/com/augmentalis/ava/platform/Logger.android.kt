package com.augmentalis.ava.platform

import android.util.Log

/**
 * Android implementation of Logger using android.util.Log.
 */
actual object Logger {
    private var minLevel: LogLevel = LogLevel.VERBOSE

    actual fun v(tag: String, message: String) {
        if (minLevel <= LogLevel.VERBOSE) {
            Log.v(tag, message)
        }
    }

    actual fun d(tag: String, message: String) {
        if (minLevel <= LogLevel.DEBUG) {
            Log.d(tag, message)
        }
    }

    actual fun i(tag: String, message: String) {
        if (minLevel <= LogLevel.INFO) {
            Log.i(tag, message)
        }
    }

    actual fun w(tag: String, message: String) {
        if (minLevel <= LogLevel.WARN) {
            Log.w(tag, message)
        }
    }

    actual fun w(tag: String, message: String, throwable: Throwable) {
        if (minLevel <= LogLevel.WARN) {
            Log.w(tag, message, throwable)
        }
    }

    actual fun e(tag: String, message: String) {
        if (minLevel <= LogLevel.ERROR) {
            Log.e(tag, message)
        }
    }

    actual fun e(tag: String, message: String, throwable: Throwable) {
        if (minLevel <= LogLevel.ERROR) {
            Log.e(tag, message, throwable)
        }
    }

    actual fun setMinLevel(level: LogLevel) {
        minLevel = level
    }
}
