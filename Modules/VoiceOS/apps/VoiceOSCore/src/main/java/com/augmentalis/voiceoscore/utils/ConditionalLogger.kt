/**
 * ConditionalLogger.kt - BuildConfig-aware logging wrapper
 *
 * YOLO Phase 3 - Medium Priority Issue #25: Excessive Debug Logging
 *
 * Problem Solved:
 * - 1494 Log statements throughout codebase causing performance overhead
 * - Debug logs shipped in release builds exposing internal details
 * - No centralized control over logging levels
 * - Performance impact from string concatenation in logs
 *
 * Solution:
 * - Wraps Android Log with BuildConfig checks
 * - Automatically strips verbose/debug logs in release builds
 * - Lazy evaluation prevents string concatenation overhead
 * - Maintains critical error/warning logs in all builds
 *
 * Usage:
 * ```kotlin
 * // Instead of:
 * Log.d(TAG, "Processing command: $command")
 *
 * // Use:
 * ConditionalLogger.d(TAG) { "Processing command: $command" }
 * ```
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude Code (Code Quality Expert Agent)
 * Created: 2025-11-09
 */
package com.augmentalis.voiceoscore.utils

import android.util.Log
import com.augmentalis.speechrecognition.BuildConfig
import com.augmentalis.voiceos.constants.VoiceOSConstants

/**
 * ConditionalLogger - BuildConfig-aware logging with lazy evaluation
 *
 * Automatically strips verbose and debug logs in release builds while
 * preserving error and warning logs for production diagnostics.
 *
 * Features:
 * - Zero overhead in release builds for V/D logs (inlined and stripped by R8)
 * - Lazy message evaluation prevents string concatenation overhead
 * - Consistent API with Android Log
 * - Thread-safe
 * - Exception logging support
 */
object ConditionalLogger {

    /**
     * Send a VERBOSE log message
     *
     * Only logged in debug builds. Completely stripped in release.
     *
     * @param tag Used to identify the source of a log message
     * @param message Lazy message provider (only evaluated in debug builds)
     */
    inline fun v(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.v(tag, message())
        }
    }

    /**
     * Send a DEBUG log message
     *
     * Only logged in debug builds. Completely stripped in release.
     *
     * @param tag Used to identify the source of a log message
     * @param message Lazy message provider (only evaluated in debug builds)
     */
    inline fun d(tag: String, message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message())
        }
    }

    /**
     * Send an INFO log message
     *
     * Logged in all builds for important runtime information.
     *
     * @param tag Used to identify the source of a log message
     * @param message Lazy message provider
     */
    inline fun i(tag: String, message: () -> String) {
        Log.i(tag, message())
    }

    /**
     * Send a WARN log message
     *
     * Logged in all builds for recoverable errors and important warnings.
     *
     * @param tag Used to identify the source of a log message
     * @param message Lazy message provider
     */
    inline fun w(tag: String, message: () -> String) {
        Log.w(tag, message())
    }

    /**
     * Send a WARN log message with exception
     *
     * @param tag Used to identify the source of a log message
     * @param message Lazy message provider
     * @param throwable Exception to log
     */
    inline fun w(tag: String, message: () -> String, throwable: Throwable) {
        Log.w(tag, message(), throwable)
    }

    /**
     * Send a WARN log message with exception only
     *
     * @param tag Used to identify the source of a log message
     * @param throwable Exception to log
     */
    inline fun w(tag: String, throwable: Throwable) {
        Log.w(tag, throwable)
    }

    /**
     * Send an ERROR log message
     *
     * Logged in all builds for critical errors requiring attention.
     *
     * @param tag Used to identify the source of a log message
     * @param message Lazy message provider
     */
    inline fun e(tag: String, message: () -> String) {
        Log.e(tag, message())
    }

    /**
     * Send an ERROR log message with exception (natural parameter order)
     *
     * Preferred overload with natural parameter ordering that follows Kotlin idioms.
     * Exception comes before message, enabling trailing lambda syntax.
     *
     * @param tag Used to identify the source of a log message
     * @param throwable Exception to log
     * @param message Lazy message provider for additional context
     *
     * Example:
     * ```kotlin
     * ConditionalLogger.e(TAG, exception) { "Error occurred during initialization" }
     * ```
     */
    inline fun e(tag: String, throwable: Throwable, message: () -> String) {
        Log.e(tag, message(), throwable)
    }

    /**
     * Send an ERROR log message with exception (explicit lambda parameter order)
     *
     * Alternative overload with message before exception.
     * Use when lambda syntax is not desired or when migrating old code.
     *
     * @param tag Used to identify the source of a log message
     * @param message Lazy message provider
     * @param throwable Exception to log
     */
    inline fun e(tag: String, message: () -> String, throwable: Throwable) {
        Log.e(tag, message(), throwable)
    }

    /**
     * Send an ERROR log message with exception only
     *
     * Use when exception message is sufficient and no additional context needed.
     *
     * @param tag Used to identify the source of a log message
     * @param throwable Exception to log
     */
    inline fun e(tag: String, throwable: Throwable) {
        Log.e(tag, "", throwable)
    }

    /**
     * Check if VERBOSE logging is enabled
     *
     * @return true if verbose logs will be shown
     */
    inline fun isVerboseEnabled(): Boolean = BuildConfig.DEBUG

    /**
     * Check if DEBUG logging is enabled
     *
     * @return true if debug logs will be shown
     */
    inline fun isDebugEnabled(): Boolean = BuildConfig.DEBUG

    /**
     * Performance-critical section logging
     *
     * Only logs if both DEBUG and performance tracking enabled.
     * Use for high-frequency operations that need optional tracing.
     *
     * @param tag Used to identify the source
     * @param enablePerformanceLogging Feature flag for performance logging
     * @param message Lazy message provider
     */
    inline fun performance(
        tag: String,
        enablePerformanceLogging: Boolean = false,
        message: () -> String
    ) {
        if (BuildConfig.DEBUG && enablePerformanceLogging) {
            Log.d(tag, "[PERF] ${message()}")
        }
    }

    /**
     * Log with automatic truncation for long messages
     *
     * Android Log has a 4000 character limit. This handles truncation gracefully.
     *
     * @param level Log level (v/d/i/w/e)
     * @param tag Used to identify the source
     * @param message Message to log (will be truncated if needed)
     */
    fun logLarge(level: String, tag: String, message: String) {
        val maxLength = VoiceOSConstants.Logging.MAX_LOG_LENGTH

        if (message.length <= maxLength) {
            when (level.lowercase()) {
                "v" -> v(tag) { message }
                "d" -> d(tag) { message }
                "i" -> i(tag) { message }
                "w" -> w(tag) { message }
                "e" -> e(tag) { message }
            }
        } else {
            // Split into chunks
            var index = 0
            while (index < message.length) {
                val end = minOf(index + maxLength, message.length)
                val chunk = message.substring(index, end)
                val prefix = if (index > 0) "[cont] " else ""

                when (level.lowercase()) {
                    "v" -> v(tag) { prefix + chunk }
                    "d" -> d(tag) { prefix + chunk }
                    "i" -> i(tag) { prefix + chunk }
                    "w" -> w(tag) { prefix + chunk }
                    "e" -> e(tag) { prefix + chunk }
                }

                index = end
            }
        }
    }

    /**
     * Security-sensitive logging with PII redaction
     *
     * Use for any logs that might contain user data.
     * Currently logs as INFO - integrate with PIILoggingWrapper when available.
     *
     * @param tag Used to identify the source
     * @param message Lazy message provider (may contain PII)
     */
    inline fun secure(tag: String, message: () -> String) {
        // TODO: Integrate with PIILoggingWrapper when available
        i(tag, message)
    }
}

/**
 * Extension functions for easier migration from Log.X to ConditionalLogger
 */

/**
 * Log verbose message with automatic BuildConfig check
 */
inline fun String.logV(tag: String) {
    ConditionalLogger.v(tag) { this }
}

/**
 * Log debug message with automatic BuildConfig check
 */
inline fun String.logD(tag: String) {
    ConditionalLogger.d(tag) { this }
}

/**
 * Log info message
 */
inline fun String.logI(tag: String) {
    ConditionalLogger.i(tag) { this }
}

/**
 * Log warning message
 */
inline fun String.logW(tag: String) {
    ConditionalLogger.w(tag) { this }
}

/**
 * Log error message
 */
inline fun String.logE(tag: String) {
    ConditionalLogger.e(tag) { this }
}
