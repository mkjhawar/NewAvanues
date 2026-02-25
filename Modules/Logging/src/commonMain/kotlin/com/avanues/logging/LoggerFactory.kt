/**
 * LoggerFactory.kt - Platform-specific logger factory (expect/actual pattern)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-02 (consolidated from duplicate implementations)
 */
package com.avanues.logging

import kotlin.concurrent.Volatile

/**
 * Platform-specific logger factory
 *
 * Provides platform-appropriate logger implementations:
 * - Android: Uses android.util.Log
 * - iOS: Uses OSLog/NSLog
 * - Desktop: Uses System.out/err with ANSI colors
 *
 * Usage:
 * ```kotlin
 * class MyClass {
 *     private val logger = LoggerFactory.getLogger("MyClass")
 *     // or
 *     private val logger = LoggerFactory.getLogger<MyClass>()
 * }
 * ```
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

/**
 * Global minimum log level.
 * Messages below this level will be ignored.
 * Default: LogLevel.VERBOSE (all messages)
 */
var globalMinLevel: LogLevel
    get() = LoggerFactoryConfig.minLevel
    set(value) { LoggerFactoryConfig.minLevel = value }

/**
 * Internal configuration holder for thread-safe access
 */
internal object LoggerFactoryConfig {
    @kotlin.concurrent.Volatile
    var minLevel: LogLevel = LogLevel.VERBOSE
}

/**
 * Extension function to get logger using reified type
 */
inline fun <reified T> LoggerFactory.getLogger(): Logger = getLogger(T::class.simpleName ?: "Unknown")
