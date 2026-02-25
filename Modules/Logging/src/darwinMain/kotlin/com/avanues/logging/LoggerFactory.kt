/**
 * LoggerFactory.kt - Apple/Darwin actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 * Updated: 2026-02-25 (moved to darwinMain for iOS + macOS support)
 */
package com.avanues.logging

/**
 * Darwin (iOS + macOS) logger factory implementation.
 *
 * Creates DarwinLogger instances that use NSLog.
 */
actual object LoggerFactory {
    /**
     * Create Darwin logger for the specified tag
     *
     * @param tag Tag for NSLog
     * @return DarwinLogger instance
     */
    actual fun getLogger(tag: String): Logger = DarwinLogger(tag)
}
