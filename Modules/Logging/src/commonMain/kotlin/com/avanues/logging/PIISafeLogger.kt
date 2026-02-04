/**
 * PIISafeLogger.kt - PII-safe logging wrapper
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from voiceos-logging)
 *
 * Provides automatic PII redaction for all log messages.
 */
package com.avanues.logging

/**
 * Centralized PII-safe logging wrapper
 *
 * All logging through this wrapper automatically redacts PII:
 * - Email addresses → [REDACTED-EMAIL]
 * - Phone numbers → [REDACTED-PHONE]
 * - SSN → [REDACTED-SSN]
 * - Credit cards → [REDACTED-CC]
 * - Names, addresses, ZIP codes
 *
 * Usage:
 * ```kotlin
 * val log = PIISafeLogger.getLogger("MyClass")
 * log.d { "User input: $userText" }  // Lazy evaluation
 * ```
 */
object PIISafeLoggerFactory {

    /**
     * Get a PII-safe logger for a tag
     *
     * @param tag Log tag (typically class name)
     * @return PII-safe logger instance
     */
    fun getLogger(tag: String): PIISafeLogger {
        return PIISafeLogger(LoggerFactory.getLogger(tag))
    }

    // Convenience methods (eager evaluation)

    fun v(tag: String, message: String?) = getLogger(tag).v { message ?: "null" }
    fun d(tag: String, message: String?) = getLogger(tag).d { message ?: "null" }
    fun i(tag: String, message: String?) = getLogger(tag).i { message ?: "null" }
    fun w(tag: String, message: String?) = getLogger(tag).w { message ?: "null" }
    fun e(tag: String, message: String?) = getLogger(tag).e { message ?: "null" }
    fun e(tag: String, message: String?, throwable: Throwable) = getLogger(tag).e({ message ?: "null" }, throwable)
    fun wtf(tag: String, message: String?) = getLogger(tag).wtf { message ?: "null" }
}

/**
 * PII-safe logger implementation
 *
 * Wraps a platform logger and automatically redacts PII from all messages.
 */
class PIISafeLogger internal constructor(
    private val delegate: Logger
) : Logger {

    override fun v(message: () -> String) = delegate.v { redact(message()) }
    override fun d(message: () -> String) = delegate.d { redact(message()) }
    override fun i(message: () -> String) = delegate.i { redact(message()) }
    override fun w(message: () -> String) = delegate.w { redact(message()) }
    override fun e(message: () -> String) = delegate.e { redact(message()) }
    override fun e(message: () -> String, throwable: Throwable) = delegate.e({ redact(message()) }, throwable)
    override fun wtf(message: () -> String) = delegate.wtf { redact(message()) }
    override fun isLoggable(level: LogLevel): Boolean = delegate.isLoggable(level)

    private fun redact(message: String): String {
        return if (message.isEmpty()) message else PIIRedactionHelper.redactPII(message)
    }
}

/**
 * Extension function to get a PII-safe logger for any class
 */
inline fun <reified T : Any> T.piiSafeLogger(): PIISafeLogger {
    return PIISafeLoggerFactory.getLogger(T::class.simpleName ?: "Unknown")
}

/**
 * Extension function to get a PII-safe logger by class reference
 */
inline fun <reified T : Any> piiSafeLoggerFor(): PIISafeLogger {
    return PIISafeLoggerFactory.getLogger(T::class.simpleName ?: "Unknown")
}
