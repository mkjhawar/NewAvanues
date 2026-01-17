/**
 * PlatformUtils.kt - Platform-specific utilities (expect declarations)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude (AI Assistant)
 * Created: 2026-01-18
 *
 * Provides expect declarations for platform-specific functionality.
 */
package com.augmentalis.speechrecognition

/**
 * Get current time in milliseconds.
 * Platform-specific implementation required.
 */
expect fun currentTimeMillis(): Long

/**
 * Log a debug message.
 * Platform-specific implementation required.
 */
expect fun logDebug(tag: String, message: String)

/**
 * Log an info message.
 * Platform-specific implementation required.
 */
expect fun logInfo(tag: String, message: String)

/**
 * Log a warning message.
 * Platform-specific implementation required.
 */
expect fun logWarn(tag: String, message: String)

/**
 * Log an error message.
 * Platform-specific implementation required.
 */
expect fun logError(tag: String, message: String, throwable: Throwable? = null)

/**
 * Create a thread-safe mutable list.
 * Platform-specific implementation required.
 */
expect fun <T> createSynchronizedList(): MutableList<T>

/**
 * Create a thread-safe mutable map.
 * Platform-specific implementation required.
 */
expect fun <K, V> createConcurrentMap(): MutableMap<K, V>
