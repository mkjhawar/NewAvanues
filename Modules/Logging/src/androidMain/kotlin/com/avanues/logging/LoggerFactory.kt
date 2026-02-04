/**
 * LoggerFactory.kt - Android actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 */
package com.avanues.logging

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
    actual fun getLogger(tag: String): Logger = AndroidLogger(tag)
}
