/**
 * NluLogger.kt - Platform-specific logging (expect declarations)
 *
 * Provides structured logging for the NLU module across all platforms.
 * Mirrors the SpeechRecognition module's logging pattern.
 */
package com.augmentalis.nlu

/**
 * Log a debug message.
 * Platform-specific implementation required.
 */
expect fun nluLogDebug(tag: String, message: String)

/**
 * Log an info message.
 * Platform-specific implementation required.
 */
expect fun nluLogInfo(tag: String, message: String)

/**
 * Log a warning message.
 * Platform-specific implementation required.
 */
expect fun nluLogWarn(tag: String, message: String)

/**
 * Log an error message with optional throwable.
 * Platform-specific implementation required.
 */
expect fun nluLogError(tag: String, message: String, throwable: Throwable? = null)
