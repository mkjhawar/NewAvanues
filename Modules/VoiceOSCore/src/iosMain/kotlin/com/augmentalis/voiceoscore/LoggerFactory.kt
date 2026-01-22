/**
 * LoggerFactory.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceoscore

/**
 * iOS logger factory implementation
 *
 * Creates IosLogger instances that use NSLog
 */
actual object LoggerFactory {
    /**
     * Create iOS logger for the specified tag
     *
     * @param tag Tag for NSLog
     * @return IosLogger instance
     */
    actual fun getLogger(tag: String): Logger {
        return IosLogger(tag)
    }
}
