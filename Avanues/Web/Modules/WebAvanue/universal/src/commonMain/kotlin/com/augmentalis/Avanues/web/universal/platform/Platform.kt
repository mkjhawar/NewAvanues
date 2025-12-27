/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.Avanues.web.universal.platform

/**
 * KMP Platform utilities - expect declarations for platform-specific implementations
 */

/**
 * Simple KMP logger interface
 */
object Log {
    /**
     * Debug log
     */
    fun d(tag: String, message: String) {
        platformLog(LogLevel.DEBUG, tag, message)
    }

    /**
     * Info log
     */
    fun i(tag: String, message: String) {
        platformLog(LogLevel.INFO, tag, message)
    }

    /**
     * Warning log
     */
    fun w(tag: String, message: String) {
        platformLog(LogLevel.WARN, tag, message)
    }

    /**
     * Error log
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        platformLog(LogLevel.ERROR, tag, message, throwable)
    }
}

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}

/**
 * Platform-specific log implementation
 */
expect fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable? = null)

/**
 * Load a resource file as text
 * @param resourcePath Path to resource (e.g., "webactions.js")
 * @return Resource content as string, or null if not found
 */
expect fun loadResourceAsText(resourcePath: String): String?
