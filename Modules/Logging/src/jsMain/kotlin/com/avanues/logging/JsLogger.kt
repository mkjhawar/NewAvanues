package com.avanues.logging

/**
 * JavaScript/Browser implementation of [Logger] using console API.
 *
 * Maps log levels to the appropriate console methods:
 * - VERBOSE/DEBUG → console.log (with [V]/[D] prefix)
 * - INFO → console.info
 * - WARN → console.warn
 * - ERROR/ASSERT → console.error
 */
internal class JsLogger(private val tag: String) : Logger {

    override fun v(message: () -> String) {
        if (isLoggable(LogLevel.VERBOSE)) {
            console.log("[V/$tag] ${message()}")
        }
    }

    override fun d(message: () -> String) {
        if (isLoggable(LogLevel.DEBUG)) {
            console.log("[D/$tag] ${message()}")
        }
    }

    override fun i(message: () -> String) {
        if (isLoggable(LogLevel.INFO)) {
            console.info("[I/$tag] ${message()}")
        }
    }

    override fun w(message: () -> String) {
        if (isLoggable(LogLevel.WARN)) {
            console.warn("[W/$tag] ${message()}")
        }
    }

    override fun e(message: () -> String) {
        if (isLoggable(LogLevel.ERROR)) {
            console.error("[E/$tag] ${message()}")
        }
    }

    override fun e(message: () -> String, throwable: Throwable) {
        if (isLoggable(LogLevel.ERROR)) {
            console.error("[E/$tag] ${message()}")
            console.error(throwable.stackTraceToString())
        }
    }

    override fun wtf(message: () -> String) {
        if (isLoggable(LogLevel.ASSERT)) {
            console.error("[WTF/$tag] ${message()}")
        }
    }

    override fun isLoggable(level: LogLevel): Boolean {
        return level.priority >= globalMinLevel.priority
    }
}
