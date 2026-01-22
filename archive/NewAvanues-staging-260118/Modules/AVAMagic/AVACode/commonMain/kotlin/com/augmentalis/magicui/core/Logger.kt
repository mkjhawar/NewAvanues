package com.augmentalis.avamagic.core

/**
 * Platform-agnostic logging interface for AvaUI.
 *
 * Implementations provide platform-specific logging:
 * - **Android**: Logcat via `android.util.Log`
 * - **iOS**: OSLog or `print()`
 * - **JVM**: SLF4J or `println()`
 * - **JS**: `console.log()`
 *
 * ## Usage
 *
 * ```kotlin
 * val logger = Logger.get("AvaUI.DSL")
 *
 * logger.debug("Parsing DSL layout")
 * logger.info("Loaded 50 components in 15ms")
 * logger.warn("Theme not found, using default")
 * logger.error("Failed to parse layout", exception)
 * ```
 *
 * @since 3.1.0
 */
interface Logger {
    /**
     * Log tag or category (e.g., "AvaUI.DSL", "AvaUI.Database").
     */
    val tag: String

    /**
     * Logs a debug message (verbose information for developers).
     */
    fun debug(message: String)

    /**
     * Logs an informational message.
     */
    fun info(message: String)

    /**
     * Logs a warning message.
     */
    fun warn(message: String, throwable: Throwable? = null)

    /**
     * Logs an error message.
     */
    fun error(message: String, throwable: Throwable? = null)

    companion object {
        /**
         * Gets a logger for the given tag.
         *
         * Implementation is platform-specific and should be provided via dependency injection.
         */
        fun get(tag: String): Logger {
            return ConsoleLogger(tag)  // Default implementation for KMP
        }
    }
}

/**
 * Default console-based logger for KMP (used when no platform-specific logger is configured).
 */
internal class ConsoleLogger(override val tag: String) : Logger {
    override fun debug(message: String) {
        println("DEBUG [$tag] $message")
    }

    override fun info(message: String) {
        println("INFO  [$tag] $message")
    }

    override fun warn(message: String, throwable: Throwable?) {
        println("WARN  [$tag] $message")
        throwable?.let { println(it.stackTraceToString()) }
    }

    override fun error(message: String, throwable: Throwable?) {
        println("ERROR [$tag] $message")
        throwable?.let { println(it.stackTraceToString()) }
    }
}
