/**
 * LogLevel.kt - Log level enumeration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 */
package com.avanues.logging

/**
 * Log level enumeration for filtering log output.
 *
 * Levels in order of increasing severity:
 * VERBOSE < DEBUG < INFO < WARN < ERROR < ASSERT
 *
 * Priority values match Android's Log levels for compatibility.
 */
enum class LogLevel(val priority: Int) {
    /** Verbose logging - most detailed (priority 2) */
    VERBOSE(2),
    /** Debug logging - development diagnostics (priority 3) */
    DEBUG(3),
    /** Info logging - general information (priority 4) */
    INFO(4),
    /** Warning logging - potential issues (priority 5) */
    WARN(5),
    /** Error logging - recoverable errors (priority 6) */
    ERROR(6),
    /** Assert logging - critical failures (priority 7) */
    ASSERT(7);

    companion object {
        /**
         * Get log level from priority value
         *
         * @param priority Android Log priority value
         * @return LogLevel matching priority, or DEBUG as default
         */
        fun fromPriority(priority: Int): LogLevel {
            return entries.firstOrNull { it.priority == priority } ?: DEBUG
        }
    }
}
