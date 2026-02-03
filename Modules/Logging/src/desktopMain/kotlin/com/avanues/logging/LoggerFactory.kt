/**
 * LoggerFactory.kt - Desktop/JVM actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 */
package com.avanues.logging

/**
 * Desktop/JVM logger factory implementation
 *
 * Creates DesktopLogger instances that use System.out/err with ANSI colors
 */
actual object LoggerFactory {
    /**
     * Create Desktop logger for the specified tag
     *
     * @param tag Tag for logging
     * @return DesktopLogger instance
     */
    actual fun getLogger(tag: String): Logger = DesktopLogger(tag)
}
