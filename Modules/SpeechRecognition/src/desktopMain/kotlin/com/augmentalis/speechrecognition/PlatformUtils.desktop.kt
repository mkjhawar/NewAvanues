/**
 * PlatformUtils.desktop.kt - Desktop/JVM-specific platform utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude (AI Assistant)
 * Created: 2026-01-18
 */
package com.augmentalis.speechrecognition

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.logging.Logger

private val logger = Logger.getLogger("SpeechRecognition")

/**
 * Get current time in milliseconds (Desktop/JVM)
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Log a debug message (Desktop/JVM)
 */
actual fun logDebug(tag: String, message: String) {
    logger.fine("$tag: $message")
}

/**
 * Log an info message (Desktop/JVM)
 */
actual fun logInfo(tag: String, message: String) {
    logger.info("$tag: $message")
}

/**
 * Log a warning message (Desktop/JVM)
 */
actual fun logWarn(tag: String, message: String) {
    logger.warning("$tag: $message")
}

/**
 * Log an error message (Desktop/JVM)
 */
actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        logger.log(Level.SEVERE, "$tag: $message", throwable)
    } else {
        logger.severe("$tag: $message")
    }
}

/**
 * Create a thread-safe mutable list (Desktop/JVM)
 */
actual fun <T> createSynchronizedList(): MutableList<T> {
    return Collections.synchronizedList(mutableListOf<T>())
}

/**
 * Create a thread-safe mutable map (Desktop/JVM)
 */
actual fun <K, V> createConcurrentMap(): MutableMap<K, V> {
    return ConcurrentHashMap<K, V>()
}
