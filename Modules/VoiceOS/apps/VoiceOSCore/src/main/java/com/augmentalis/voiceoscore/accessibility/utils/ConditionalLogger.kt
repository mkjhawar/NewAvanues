/**
 * ConditionalLogger.kt - Conditional logging utility for VoiceOS
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Provides conditional logging that can be enabled/disabled at runtime
 * without removing log statements from the code. Useful for debugging
 * performance-sensitive code paths.
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Conditional Logger
 *
 * Logging utility that allows enabling/disabling logs per tag at runtime.
 * Useful for debugging specific components without flooding logcat.
 *
 * Features:
 * - Per-tag enable/disable
 * - Global enable/disable
 * - Log level filtering
 * - PII redaction support
 */
object ConditionalLogger {
    private const val TAG = "ConditionalLogger"

    // Global logging state
    private val globalEnabled = AtomicBoolean(true)

    // Per-tag enabled state
    private val tagEnabled = ConcurrentHashMap<String, Boolean>()

    // Minimum log level (default: DEBUG)
    private var minLogLevel = Log.DEBUG

    // PII patterns to redact
    private val piiPatterns = listOf(
        Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), // Email
        Regex("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b"), // Phone
        Regex("\\b\\d{4}[- ]?\\d{4}[- ]?\\d{4}[- ]?\\d{4}\\b") // Credit card
    )

    /**
     * Enable or disable global logging.
     */
    fun setGlobalEnabled(enabled: Boolean) {
        globalEnabled.set(enabled)
        Log.i(TAG, "Global logging ${if (enabled) "enabled" else "disabled"}")
    }

    /**
     * Check if global logging is enabled.
     */
    fun isGlobalEnabled(): Boolean = globalEnabled.get()

    /**
     * Enable logging for a specific tag.
     */
    fun enableTag(tag: String) {
        tagEnabled[tag] = true
    }

    /**
     * Disable logging for a specific tag.
     */
    fun disableTag(tag: String) {
        tagEnabled[tag] = false
    }

    /**
     * Check if logging is enabled for a tag.
     */
    fun isTagEnabled(tag: String): Boolean {
        return globalEnabled.get() && tagEnabled.getOrDefault(tag, true)
    }

    /**
     * Set minimum log level.
     * @param level One of Log.VERBOSE, Log.DEBUG, Log.INFO, Log.WARN, Log.ERROR
     */
    fun setMinLogLevel(level: Int) {
        minLogLevel = level.coerceIn(Log.VERBOSE, Log.ASSERT)
    }

    /**
     * Log verbose message (conditional).
     */
    fun v(tag: String, message: String) {
        if (shouldLog(tag, Log.VERBOSE)) {
            Log.v(tag, redactPii(message))
        }
    }

    /**
     * Log verbose message with throwable (conditional).
     */
    fun v(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(tag, Log.VERBOSE)) {
            Log.v(tag, redactPii(message), throwable)
        }
    }

    /**
     * Log debug message (conditional).
     */
    fun d(tag: String, message: String) {
        if (shouldLog(tag, Log.DEBUG)) {
            Log.d(tag, redactPii(message))
        }
    }

    /**
     * Log debug message with throwable (conditional).
     */
    fun d(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(tag, Log.DEBUG)) {
            Log.d(tag, redactPii(message), throwable)
        }
    }

    /**
     * Log info message (conditional).
     */
    fun i(tag: String, message: String) {
        if (shouldLog(tag, Log.INFO)) {
            Log.i(tag, redactPii(message))
        }
    }

    /**
     * Log info message with throwable (conditional).
     */
    fun i(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(tag, Log.INFO)) {
            Log.i(tag, redactPii(message), throwable)
        }
    }

    /**
     * Log warning message (conditional).
     */
    fun w(tag: String, message: String) {
        if (shouldLog(tag, Log.WARN)) {
            Log.w(tag, redactPii(message))
        }
    }

    /**
     * Log warning message with throwable (conditional).
     */
    fun w(tag: String, message: String, throwable: Throwable) {
        if (shouldLog(tag, Log.WARN)) {
            Log.w(tag, redactPii(message), throwable)
        }
    }

    /**
     * Log error message (always logs if tag enabled, ignores level).
     */
    fun e(tag: String, message: String) {
        if (isTagEnabled(tag)) {
            Log.e(tag, redactPii(message))
        }
    }

    /**
     * Log error message with throwable (always logs if tag enabled).
     */
    fun e(tag: String, message: String, throwable: Throwable) {
        if (isTagEnabled(tag)) {
            Log.e(tag, redactPii(message), throwable)
        }
    }

    /**
     * Log with a lambda that's only evaluated if logging is enabled.
     * Use for expensive string operations.
     */
    inline fun dLazy(tag: String, messageProvider: () -> String) {
        if (shouldLog(tag, Log.DEBUG)) {
            Log.d(tag, redactPii(messageProvider()))
        }
    }

    /**
     * Log with a lambda that's only evaluated if logging is enabled.
     */
    inline fun vLazy(tag: String, messageProvider: () -> String) {
        if (shouldLog(tag, Log.VERBOSE)) {
            Log.v(tag, redactPii(messageProvider()))
        }
    }

    /**
     * Check if a message should be logged.
     * Made internal (not private) to support inline functions.
     */
    @PublishedApi
    internal fun shouldLog(tag: String, level: Int): Boolean {
        return globalEnabled.get() &&
                tagEnabled.getOrDefault(tag, true) &&
                level >= minLogLevel
    }

    /**
     * Redact PII from log messages.
     */
    fun redactPii(message: String): String {
        var result = message
        piiPatterns.forEach { pattern ->
            result = pattern.replace(result, "[REDACTED]")
        }
        return result
    }

    /**
     * Create a logger instance for a specific tag.
     */
    fun getLogger(tag: String): TagLogger {
        return TagLogger(tag)
    }

    /**
     * Tag-specific logger instance.
     */
    class TagLogger(private val tag: String) {
        fun v(message: String) = ConditionalLogger.v(tag, message)
        fun v(message: String, t: Throwable) = ConditionalLogger.v(tag, message, t)
        fun d(message: String) = ConditionalLogger.d(tag, message)
        fun d(message: String, t: Throwable) = ConditionalLogger.d(tag, message, t)
        fun i(message: String) = ConditionalLogger.i(tag, message)
        fun i(message: String, t: Throwable) = ConditionalLogger.i(tag, message, t)
        fun w(message: String) = ConditionalLogger.w(tag, message)
        fun w(message: String, t: Throwable) = ConditionalLogger.w(tag, message, t)
        fun e(message: String) = ConditionalLogger.e(tag, message)
        fun e(message: String, t: Throwable) = ConditionalLogger.e(tag, message, t)

        fun isEnabled(): Boolean = isTagEnabled(tag)
        fun enable() = enableTag(tag)
        fun disable() = disableTag(tag)
    }

    /**
     * Performance timing utility.
     */
    inline fun <T> timed(tag: String, operation: String, block: () -> T): T {
        if (!shouldLog(tag, Log.DEBUG)) {
            return block()
        }

        val start = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val elapsed = System.currentTimeMillis() - start
            d(tag, "$operation completed in ${elapsed}ms")
        }
    }
}
