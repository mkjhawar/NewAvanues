/**
 * QuantizedScreen.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Quantized screen representation
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

/**
 * Quantized Screen
 *
 * Compact representation of a screen for LLM consumption
 */
data class QuantizedScreen(
    val screenHash: String,
    val screenTitle: String,
    val activityName: String?,
    val elements: List<QuantizedElement>
)

/**
 * Quantized Element
 *
 * Compact representation of a UI element
 */
data class QuantizedElement(
    val vuid: String,
    val type: ElementType,
    val label: String,
    val aliases: List<String> = emptyList()
)

/**
 * Element Type
 *
 * Type of UI element
 */
enum class ElementType {
    BUTTON,
    TEXT_FIELD,
    CHECKBOX,
    SWITCH,
    DROPDOWN,
    TAB,
    OTHER
}
