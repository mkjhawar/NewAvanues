/**
 * PlatformUtils.js.kt - JS/Web-specific platform utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude (AI Assistant)
 * Created: 2026-01-18
 */
package com.augmentalis.speechrecognition

import kotlin.js.Date

/**
 * Get current time in milliseconds (JS)
 */
actual fun currentTimeMillis(): Long = Date.now().toLong()

/**
 * Log a debug message (JS)
 */
actual fun logDebug(tag: String, message: String) {
    console.log("[DEBUG] $tag: $message")
}

/**
 * Log an info message (JS)
 */
actual fun logInfo(tag: String, message: String) {
    console.info("[INFO] $tag: $message")
}

/**
 * Log a warning message (JS)
 */
actual fun logWarn(tag: String, message: String) {
    console.warn("[WARN] $tag: $message")
}

/**
 * Log an error message (JS)
 */
actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        console.error("[ERROR] $tag: $message", throwable)
    } else {
        console.error("[ERROR] $tag: $message")
    }
}

/**
 * Create a thread-safe mutable list (JS - single-threaded, no sync needed)
 */
actual fun <T> createSynchronizedList(): MutableList<T> {
    return mutableListOf()
}

/**
 * Create a thread-safe mutable map (JS - single-threaded, no sync needed)
 */
actual fun <K, V> createConcurrentMap(): MutableMap<K, V> {
    return mutableMapOf()
}
