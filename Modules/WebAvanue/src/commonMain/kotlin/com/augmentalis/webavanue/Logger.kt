package com.augmentalis.webavanue

import com.avanues.logging.LoggerFactory

/**
 * Logger - Centralized logging utility with PII filtering for WebAvanue
 *
 * Delegates to the canonical Logging module (com.avanues.logging).
 *
 * Security Features:
 * - Sanitizes URLs (removes query parameters, truncates length)
 * - Sanitizes filenames (keeps only extension for privacy)
 *
 * Usage:
 * ```kotlin
 * Logger.debug("TabViewModel", "Tab created")
 * Logger.info("DownloadViewModel", "Download started: ${Logger.sanitizeFilename(filename)}")
 * Logger.warn("SecurityViewModel", "Certificate validation warning", throwable)
 * Logger.error("BrowserRepository", "Database error", exception)
 * ```
 */
object Logger {
    private const val MAX_URL_LENGTH = 50

    /**
     * Sanitize URL for logging (PII protection)
     * Removes query parameters, truncates to 50 characters.
     */
    fun sanitizeUrl(url: String): String {
        return url.substringBefore("?").take(MAX_URL_LENGTH)
    }

    /**
     * Sanitize filename for logging (privacy protection)
     * Keeps only file extension.
     */
    fun sanitizeFilename(filename: String): String {
        val extension = filename.substringAfterLast(".", "")
        return if (extension.isNotEmpty()) "***.$extension" else "***"
    }

    fun debug(tag: String, message: String) {
        LoggerFactory.getLogger(tag).d { message }
    }

    fun info(tag: String, message: String) {
        LoggerFactory.getLogger(tag).i { message }
    }

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        val logger = LoggerFactory.getLogger(tag)
        if (throwable != null) {
            logger.e({ message }, throwable)
        } else {
            logger.w { message }
        }
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val logger = LoggerFactory.getLogger(tag)
        if (throwable != null) {
            logger.e({ message }, throwable)
        } else {
            logger.e { message }
        }
    }
}
