/**
 * LogLevel.kt - Logging level enumeration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.commandmanager

/**
 * Log levels matching Android's Log levels
 *
 * Priority order: VERBOSE < DEBUG < INFO < WARN < ERROR < ASSERT
 */
enum class LogLevel(val priority: Int) {
    /** Verbose logging (lowest priority) */
    VERBOSE(2),

    /** Debug logging */
    DEBUG(3),

    /** Informational logging */
    INFO(4),

    /** Warning logging */
    WARN(5),

    /** Error logging */
    ERROR(6),

    /** Assert logging (highest priority) */
    ASSERT(7);

    companion object {
        /**
         * Get log level from priority value
         */
        fun fromPriority(priority: Int): LogLevel {
            return values().firstOrNull { it.priority == priority } ?: DEBUG
        }
    }
}
