package com.augmentalis.ava.platform

/**
 * Log level enumeration.
 */
enum class LogLevel {
    VERBOSE,
    DEBUG,
    INFO,
    WARN,
    ERROR
}

/**
 * Cross-platform logging abstraction.
 *
 * Platform implementations:
 * - Android: android.util.Log
 * - iOS: OSLog
 * - Desktop: java.util.logging.Logger
 */
expect object Logger {

    /**
     * Log verbose message.
     */
    fun v(tag: String, message: String)

    /**
     * Log debug message.
     */
    fun d(tag: String, message: String)

    /**
     * Log info message.
     */
    fun i(tag: String, message: String)

    /**
     * Log warning message.
     */
    fun w(tag: String, message: String)

    /**
     * Log warning with exception.
     */
    fun w(tag: String, message: String, throwable: Throwable)

    /**
     * Log error message.
     */
    fun e(tag: String, message: String)

    /**
     * Log error with exception.
     */
    fun e(tag: String, message: String, throwable: Throwable)

    /**
     * Set minimum log level. Messages below this level are ignored.
     */
    fun setMinLevel(level: LogLevel)
}
