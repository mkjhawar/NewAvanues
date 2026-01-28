/**
 * PlatformActuals.kt - Desktop (JVM) Actual Implementations
 *
 * Provides JVM-specific implementations for expect declarations.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-17
 */

package com.augmentalis.commandmanager

/**
 * Desktop (JVM) implementation of currentTimeMillis.
 * Uses System.currentTimeMillis().
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Desktop (JVM) implementation of getCurrentTimeMillis.
 * Uses System.currentTimeMillis().
 */
actual fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

/**
 * Desktop (JVM) implementation of sha256.
 * Uses java.security.MessageDigest.
 */
actual fun sha256(input: String): String {
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val digest = md.digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}

/**
 * Desktop implementation of extractAccessibilityElements.
 * Returns empty list - actual implementation done via desktop accessibility APIs.
 */
actual fun extractAccessibilityElements(): List<ElementInfo> = emptyList()

/**
 * Desktop implementation of executeWebScript.
 * Returns null - actual implementation done via embedded browser.
 */
actual fun executeWebScript(script: String): String? = null

/**
 * Desktop implementation of isAccessibilityAvailable.
 * Returns false - actual check done at runtime.
 */
actual fun isAccessibilityAvailable(): Boolean = false

/**
 * Desktop implementation of isWebExtractionAvailable.
 * Returns false - actual check done at runtime.
 */
actual fun isWebExtractionAvailable(): Boolean = false

/**
 * Desktop implementation of getPlatformName.
 */
actual fun getPlatformName(): String = "Desktop"
