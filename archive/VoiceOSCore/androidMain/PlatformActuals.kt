/**
 * PlatformActuals.kt - Android Actual Implementations
 *
 * Provides Android-specific implementations for expect declarations.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */

package com.augmentalis.voiceoscore

/**
 * Android implementation of currentTimeMillis.
 * Uses System.currentTimeMillis().
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Android implementation of sha256.
 * Uses java.security.MessageDigest.
 */
actual fun sha256(input: String): String {
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

/**
 * Android implementation of extractAccessibilityElements.
 * Returns empty list - actual implementation done via accessibility service.
 */
actual fun extractAccessibilityElements(): List<ElementInfo> = emptyList()

/**
 * Android implementation of executeWebScript.
 * Returns null - actual implementation done via WebView.
 */
actual fun executeWebScript(script: String): String? = null

/**
 * Android implementation of isAccessibilityAvailable.
 * Returns false - actual check done at runtime with context.
 */
actual fun isAccessibilityAvailable(): Boolean = false

/**
 * Android implementation of isWebExtractionAvailable.
 * Returns false - actual check done at runtime.
 */
actual fun isWebExtractionAvailable(): Boolean = false

/**
 * Android implementation of getPlatformName.
 */
actual fun getPlatformName(): String = "Android"
