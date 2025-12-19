package com.augmentalis.ava.core.data.util

/**
 * Cross-platform time utilities
 *
 * Provides platform-agnostic access to current time in milliseconds.
 */
expect object TimeHelper {
    /**
     * Get current time in milliseconds since Unix epoch
     *
     * @return Current time in milliseconds
     */
    fun currentTimeMillis(): Long
}
