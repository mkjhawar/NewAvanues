/**
 * PIILoggingWrapper.kt - Cross-platform PII-safe logging wrapper
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.logging

/**
 * Centralized PII-safe logging wrapper
 *
 * Provides automatic PII redaction for all log messages to eliminate inconsistent
 * manual redaction across the codebase.
 *
 * ## Problem
 * Multiple files have inconsistent PII redaction:
 * - Some logs use PIIRedactionHelper.redactPII()
 * - Others log raw user data directly
 * - Manual redaction is error-prone and easy to forget
 *
 * ## Solution
 * All logging MUST go through PIILoggingWrapper which:
 * - Automatically redacts all PII before logging
 * - Preserves system identifiers (resource IDs, class names, package names)
 * - Provides type-safe logging API with lazy evaluation
 * - Zero chance of accidental PII leakage
 * - Cross-platform (Android, iOS, JVM, JS)
 *
 * ## Usage
 * ```kotlin
 * // Create logger instance
 * val log = PIILoggingWrapper.getLogger("MyClass")
 *
 * // Log with automatic PII redaction
 * log.d { "User input: $userText" }  // Lazy evaluation
 * log.e { "Error processing: $errorData" }
 *
 * // String API (eager evaluation)
 * PIILoggingWrapper.d("MyClass", "User: $userInput")
 * ```
 *
 * ## PII Detection
 * Automatically detects and redacts:
 * - Email addresses → [REDACTED-EMAIL]
 * - Phone numbers → [REDACTED-PHONE]
 * - SSN → [REDACTED-SSN]
 * - Credit cards → [REDACTED-CC]
 * - Names, addresses, ZIP codes
 *
 * ## Safe Identifiers (NOT redacted)
 * - Android resource IDs: com.example:id/button
 * - Class names: android.widget.Button
 * - Package names: com.augmentalis.voiceoscore
 */
object PIILoggingWrapper {

    /**
     * Get a PII-safe logger for a tag
     *
     * Returns a wrapped logger that automatically redacts PII from all messages.
     *
     * @param tag Log tag (typically class name)
     * @return PII-safe logger instance
     */
    fun getLogger(tag: String): PIISafeLogger {
        return PIISafeLogger(LoggerFactory.getLogger(tag))
    }

    // ===== Convenience methods (eager evaluation) =====

    /**
     * Log verbose message with automatic PII redaction
     *
     * @param tag Log tag
     * @param message Message to log (will be redacted automatically)
     */
    fun v(tag: String, message: String?) {
        getLogger(tag).v { message ?: "null" }
    }

    /**
     * Log debug message with automatic PII redaction
     *
     * @param tag Log tag
     * @param message Message to log (will be redacted automatically)
     */
    fun d(tag: String, message: String?) {
        getLogger(tag).d { message ?: "null" }
    }

    /**
     * Log info message with automatic PII redaction
     *
     * @param tag Log tag
     * @param message Message to log (will be redacted automatically)
     */
    fun i(tag: String, message: String?) {
        getLogger(tag).i { message ?: "null" }
    }

    /**
     * Log warning message with automatic PII redaction
     *
     * @param tag Log tag
     * @param message Message to log (will be redacted automatically)
     */
    fun w(tag: String, message: String?) {
        getLogger(tag).w { message ?: "null" }
    }

    /**
     * Log error message with automatic PII redaction
     *
     * @param tag Log tag
     * @param message Message to log (will be redacted automatically)
     */
    fun e(tag: String, message: String?) {
        getLogger(tag).e { message ?: "null" }
    }

    /**
     * Log error message with exception and automatic PII redaction
     *
     * @param tag Log tag
     * @param message Message to log (will be redacted automatically)
     * @param throwable Exception to log
     */
    fun e(tag: String, message: String?, throwable: Throwable) {
        getLogger(tag).e({ message ?: "null" }, throwable)
    }

    /**
     * Log assert message with automatic PII redaction
     *
     * @param tag Log tag
     * @param message Message to log (will be redacted automatically)
     */
    fun wtf(tag: String, message: String?) {
        getLogger(tag).wtf { message ?: "null" }
    }
}

/**
 * PII-safe logger implementation
 *
 * Wraps a platform logger and automatically redacts PII from all messages.
 * Uses lazy evaluation for optimal performance.
 */
class PIISafeLogger internal constructor(
    private val delegate: Logger
) : Logger {

    override fun v(message: () -> String) {
        delegate.v { redactPII(message()) }
    }

    override fun d(message: () -> String) {
        delegate.d { redactPII(message()) }
    }

    override fun i(message: () -> String) {
        delegate.i { redactPII(message()) }
    }

    override fun w(message: () -> String) {
        delegate.w { redactPII(message()) }
    }

    override fun e(message: () -> String) {
        delegate.e { redactPII(message()) }
    }

    override fun e(message: () -> String, throwable: Throwable) {
        delegate.e({ redactPII(message()) }, throwable)
    }

    override fun wtf(message: () -> String) {
        delegate.wtf { redactPII(message()) }
    }

    override fun isLoggable(level: LogLevel): Boolean {
        return delegate.isLoggable(level)
    }

    /**
     * Redact PII from message using PIIRedactionHelper
     *
     * @param message Message potentially containing PII
     * @return Redacted message safe for logging
     */
    private fun redactPII(message: String): String {
        if (message.isEmpty()) return message
        return PIIRedactionHelper.redactPII(message)
    }
}
