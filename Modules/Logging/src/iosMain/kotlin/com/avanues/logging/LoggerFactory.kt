/**
 * LoggerFactory.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 */
package com.avanues.logging

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
    actual fun getLogger(tag: String): Logger = IosLogger(tag)
}
