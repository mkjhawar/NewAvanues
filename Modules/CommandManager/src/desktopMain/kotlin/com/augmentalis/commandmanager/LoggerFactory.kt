/**
 * LoggerFactory.kt - JVM actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.commandmanager

/**
 * JVM logger factory implementation
 *
 * Creates JvmLogger instances that use System.out/err
 */
actual object LoggerFactory {
    /**
     * Create JVM logger for the specified tag
     *
     * @param tag Tag for logging
     * @return JvmLogger instance
     */
    actual fun getLogger(tag: String): Logger {
        return JvmLogger(tag)
    }
}
