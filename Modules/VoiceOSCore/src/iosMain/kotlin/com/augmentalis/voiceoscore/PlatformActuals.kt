/**
 * PlatformActuals.kt - iOS Actual Implementations
 *
 * Provides iOS-specific implementations for expect declarations.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */

package com.augmentalis.voiceoscore

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

/**
 * iOS implementation of currentTimeMillis.
 * Uses NSDate.timeIntervalSince1970.
 */
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

/**
 * iOS implementation of getCurrentTimeMillis.
 * Uses NSDate.timeIntervalSince1970.
 */
actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

/**
 * iOS implementation of sha256.
 * Uses CommonCrypto via Kotlin/Native interop.
 */
actual fun sha256(input: String): String {
    // Delegate to sha256Impl which is implemented in Sha256Ios.kt
    return sha256Impl(input)
}

/**
 * iOS implementation of extractAccessibilityElements.
 * Returns empty list - actual implementation done via iOS accessibility APIs.
 */
actual fun extractAccessibilityElements(): List<ElementInfo> = emptyList()

/**
 * iOS implementation of executeWebScript.
 * Returns null - actual implementation done via WKWebView.
 */
actual fun executeWebScript(script: String): String? = null

/**
 * iOS implementation of isAccessibilityAvailable.
 * Returns false - actual check done at runtime.
 */
actual fun isAccessibilityAvailable(): Boolean = false

/**
 * iOS implementation of isWebExtractionAvailable.
 * Returns false - actual check done at runtime.
 */
actual fun isWebExtractionAvailable(): Boolean = false

/**
 * iOS implementation of getPlatformName.
 */
actual fun getPlatformName(): String = "iOS"
