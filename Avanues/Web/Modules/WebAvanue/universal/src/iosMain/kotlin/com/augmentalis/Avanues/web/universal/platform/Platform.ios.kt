/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 */

package com.augmentalis.Avanues.web.universal.platform

import platform.Foundation.NSBundle
import platform.Foundation.NSLog

/**
 * iOS implementation of platform utilities
 */

actual fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
    val levelPrefix = when (level) {
        LogLevel.DEBUG -> "D"
        LogLevel.INFO -> "I"
        LogLevel.WARN -> "W"
        LogLevel.ERROR -> "E"
    }
    val errorInfo = throwable?.let { " - ${it.message}" } ?: ""
    NSLog("[$levelPrefix/$tag] $message$errorInfo")
}

actual fun loadResourceAsText(resourcePath: String): String? {
    return try {
        // Get the resource from the main bundle
        val pathComponents = resourcePath.split(".")
        val name = pathComponents.dropLast(1).joinToString(".")
        val ext = pathComponents.lastOrNull() ?: ""

        val path = NSBundle.mainBundle.pathForResource(name, ext)
        if (path != null) {
            // Read file content using Kotlin/Native
            platform.Foundation.NSString.stringWithContentsOfFile(
                path,
                platform.Foundation.NSUTF8StringEncoding,
                null
            ) as? String
        } else {
            NSLog("Resource not found: $resourcePath")
            null
        }
    } catch (e: Exception) {
        NSLog("Failed to load resource: $resourcePath - ${e.message}")
        null
    }
}
