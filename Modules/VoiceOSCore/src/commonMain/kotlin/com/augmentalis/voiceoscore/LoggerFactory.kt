/**
 * LoggerFactory.kt - Platform-specific logger factory (expect/actual pattern)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceoscore

/**
 * Platform-specific logger factory
 *
 * Provides platform-appropriate logger implementations:
 * - Android: Uses Android Log
 * - iOS: Uses NSLog
 * - JVM: Uses System.out/err
 * - JS: Uses console.log
 */
expect object LoggerFactory {
    /**
     * Create a logger for the specified tag
     *
     * @param tag Tag for the logger (typically class name)
     * @return Platform-specific logger implementation
     */
    fun getLogger(tag: String): Logger
}
