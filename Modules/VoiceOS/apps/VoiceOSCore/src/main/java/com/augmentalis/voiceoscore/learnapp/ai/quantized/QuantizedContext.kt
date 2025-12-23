/**
 * QuantizedContext.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Quantized representation of app context for LLM prompts
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

/**
 * Quantized Context
 *
 * Compact representation of an app's learned structure optimized for LLM consumption
 */
data class QuantizedContext(
    val packageName: String,
    val appName: String,
    val versionCode: Long,
    val versionName: String,
    val generatedAt: Long,
    val screens: List<QuantizedScreen>,
    val navigation: List<QuantizedNavigation>,
    val vocabulary: Set<String>,
    val knownCommands: List<QuantizedCommand>
) {
    /**
     * Find screen by hash
     *
     * @param screenHash Screen hash to find
     * @return QuantizedScreen if found, null otherwise
     */
    fun findScreen(screenHash: String): QuantizedScreen? =
        screens.find { it.screenHash == screenHash }

    /**
     * Get all navigation edges from a screen
     *
     * @param screenHash Source screen hash
     * @return List of navigation edges from this screen
     */
    fun getNavigationFrom(screenHash: String): List<QuantizedNavigation> =
        navigation.filter { it.fromScreenHash == screenHash }

    /**
     * Find screens containing elements with matching label
     *
     * @param label Label to search for
     * @return List of screens containing matching elements
     */
    fun findScreensWithElement(label: String): List<QuantizedScreen> =
        screens.filter { screen ->
            screen.elements.any { it.label.contains(label, ignoreCase = true) }
        }
}
