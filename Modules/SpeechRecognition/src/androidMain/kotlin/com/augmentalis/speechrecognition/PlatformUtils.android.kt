/**
 * PlatformUtils.android.kt - Android-specific platform utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Claude (AI Assistant)
 * Created: 2026-01-18
 */
package com.augmentalis.speechrecognition

import android.util.Log
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

/**
 * Get current time in milliseconds (Android)
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Log a debug message (Android)
 */
actual fun logDebug(tag: String, message: String) {
    Log.d(tag, message)
}

/**
 * Log an info message (Android)
 */
actual fun logInfo(tag: String, message: String) {
    Log.i(tag, message)
}

/**
 * Log a warning message (Android)
 */
actual fun logWarn(tag: String, message: String) {
    Log.w(tag, message)
}

/**
 * Log an error message (Android)
 */
actual fun logError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        Log.e(tag, message, throwable)
    } else {
        Log.e(tag, message)
    }
}

/**
 * Create a thread-safe mutable list (Android/JVM)
 */
actual fun <T> createSynchronizedList(): MutableList<T> {
    return Collections.synchronizedList(mutableListOf<T>())
}

/**
 * Create a thread-safe mutable map (Android/JVM)
 */
actual fun <K, V> createConcurrentMap(): MutableMap<K, V> {
    return ConcurrentHashMap<K, V>()
}
