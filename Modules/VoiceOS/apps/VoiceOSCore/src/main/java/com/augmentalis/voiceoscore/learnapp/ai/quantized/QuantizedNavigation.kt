/**
 * QuantizedNavigation.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Quantized navigation edge
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

/**
 * Quantized Navigation
 *
 * Represents a navigation edge between screens
 */
data class QuantizedNavigation(
    val fromScreenHash: String,
    val toScreenHash: String,
    val triggerLabel: String,
    val triggerVuid: String
)
