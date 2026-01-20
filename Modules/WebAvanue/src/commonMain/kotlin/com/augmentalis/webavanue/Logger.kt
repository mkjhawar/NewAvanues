package com.augmentalis.webavanue

import io.github.aakira.napier.Napier

/**
 * Logger - Centralized logging utility with PII filtering
 *
 * Security Features:
 * - Sanitizes URLs (removes query parameters, truncates length)
 * - Sanitizes filenames (keeps only extension for privacy)
 * - Filters sensitive data from logs
 * - Structured logging with tags and levels
 *
 * Usage:
 * ```kotlin
 * Logger.debug("TabViewModel", "Tab created")
 * Logger.info("DownloadViewModel", "Download started: ${Logger.sanitizeFilename(filename)}")
 * Logger.warn("SecurityViewModel", "Certificate validation warning", throwable)
 * Logger.error("BrowserRepository", "Database error", exception)
 * ```
 *
 * Log Levels:
 * - DEBUG: Development debugging (verbose)
 * - INFO: General information (user actions, state changes)
 * - WARN: Warnings and recoverable errors
 * - ERROR: Errors and exceptions
 *
 * PII Protection:
 * - URLs: Query params stripped, length limited to 50 chars
 * - Filenames: Only extension visible (e.g., "***pdf")
 * - User data: Never logged directly
 *
 * Production Configuration:
 * - Debug builds: All levels enabled
 * - Release builds: INFO+ only (configured in Application)
 */
object Logger {
    private const val MAX_URL_LENGTH = 50

    /**
     * Sanitize URL for logging (PII protection)
     * - Removes query parameters (may contain tokens, user data)
     * - Truncates to 50 characters
     * - Example: "https://example.com/page?token=secret" -> "https://example.com/page"
     */
    fun sanitizeUrl(url: String): String {
        return url.substringBefore("?").take(MAX_URL_LENGTH)
    }

    /**
     * Sanitize filename for logging (privacy protection)
     * - Keeps only file extension
     * - Example: "john_doe_resume.pdf" -> "***.pdf"
     */
    fun sanitizeFilename(filename: String): String {
        val extension = filename.substringAfterLast(".", "")
        return if (extension.isNotEmpty()) "***.$extension" else "***"
    }

    /**
     * Debug log - Development debugging only
     * Disabled in production builds
     */
    fun debug(tag: String, message: String) {
        Napier.d(tag = tag, message = message)
    }

    /**
     * Info log - General information
     * User actions, state changes, flow tracking
     */
    fun info(tag: String, message: String) {
        Napier.i(tag = tag, message = message)
    }

    /**
     * Warning log - Recoverable errors and warnings
     * Certificate issues, retry attempts, degraded performance
     */
    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Napier.w(tag = tag, message = message, throwable = throwable)
        } else {
            Napier.w(tag = tag, message = message)
        }
    }

    /**
     * Error log - Errors and exceptions
     * Database errors, network failures, crashes
     */
    fun error(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable != null) {
            Napier.e(tag = tag, message = message, throwable = throwable)
        } else {
            Napier.e(tag = tag, message = message)
        }
    }
}
