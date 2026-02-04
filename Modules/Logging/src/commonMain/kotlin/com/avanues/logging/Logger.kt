/**
 * Logger.kt - Cross-platform logging interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 *
 * This is the canonical logging interface for all Avanues modules.
 * Provides platform-agnostic logging with lazy evaluation for performance.
 */
package com.avanues.logging

/**
 * Cross-platform logger interface
 *
 * Provides platform-agnostic logging with lazy evaluation for performance.
 * Implementations use platform-specific logging mechanisms:
 * - Android: android.util.Log
 * - iOS: OSLog/NSLog
 * - Desktop: java.util.logging.Logger
 *
 * Usage:
 * ```kotlin
 * val logger = LoggerFactory.getLogger("MyClass")
 * logger.d { "Debug message" }
 * logger.e({ "Error occurred" }, exception)
 * ```
 */
interface Logger {
    /**
     * Log verbose message
     *
     * @param message Lazy message provider (only evaluated if logging enabled)
     */
    fun v(message: () -> String)

    /**
     * Log debug message
     *
     * @param message Lazy message provider
     */
    fun d(message: () -> String)

    /**
     * Log info message
     *
     * @param message Lazy message provider
     */
    fun i(message: () -> String)

    /**
     * Log warning message
     *
     * @param message Lazy message provider
     */
    fun w(message: () -> String)

    /**
     * Log error message
     *
     * @param message Lazy message provider
     */
    fun e(message: () -> String)

    /**
     * Log error with exception
     *
     * @param message Lazy message provider
     * @param throwable Exception to log
     */
    fun e(message: () -> String, throwable: Throwable)

    /**
     * Log assert message (highest priority)
     *
     * @param message Lazy message provider
     */
    fun wtf(message: () -> String)

    /**
     * Check if logging is enabled for given level
     *
     * @param level Log level to check
     * @return true if logging enabled for this level
     */
    fun isLoggable(level: LogLevel): Boolean
}
