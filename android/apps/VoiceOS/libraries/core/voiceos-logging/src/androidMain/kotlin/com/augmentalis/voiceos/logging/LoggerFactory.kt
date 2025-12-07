/**
 * LoggerFactory.kt - Android actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceos.logging

/**
 * Android logger factory implementation
 *
 * Creates AndroidLogger instances that use android.util.Log
 */
actual object LoggerFactory {
    /**
     * Create Android logger for the specified tag
     *
     * @param tag Tag for android.util.Log
     * @return AndroidLogger instance
     */
    actual fun getLogger(tag: String): Logger {
        return AndroidLogger(tag)
    }
}
