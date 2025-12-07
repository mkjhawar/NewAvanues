/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.Avanues.web.universal.platform

import android.util.Log as AndroidLog

/**
 * Android implementation of platform utilities
 */

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    when (level) {
        LogLevel.DEBUG -> {
            if (throwable != null) AndroidLog.d(tag, message, throwable)
            else AndroidLog.d(tag, message)
        }
        LogLevel.INFO -> {
            if (throwable != null) AndroidLog.i(tag, message, throwable)
            else AndroidLog.i(tag, message)
        }
        LogLevel.WARN -> {
            if (throwable != null) AndroidLog.w(tag, message, throwable)
            else AndroidLog.w(tag, message)
        }
        LogLevel.ERROR -> {
            if (throwable != null) AndroidLog.e(tag, message, throwable)
            else AndroidLog.e(tag, message)
        }
    }
}

actual fun loadResourceAsText(resourcePath: String): String? {
    return try {
        Thread.currentThread().contextClassLoader
            ?.getResourceAsStream(resourcePath)
            ?.bufferedReader()
            ?.use { it.readText() }
    } catch (e: Exception) {
        AndroidLog.e("Platform", "Failed to load resource: $resourcePath", e)
        null
    }
}
