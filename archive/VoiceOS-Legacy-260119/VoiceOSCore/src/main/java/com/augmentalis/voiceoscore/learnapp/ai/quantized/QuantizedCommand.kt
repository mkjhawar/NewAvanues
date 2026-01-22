/**
 * QuantizedCommand.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Quantized voice command representation
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

/**
 * Quantized Command
 *
 * Represents a learned voice command
 */
data class QuantizedCommand(
    val phrase: String,
    val actionType: CommandActionType,
    val targetVuid: String?,
    val confidence: Float
)

/**
 * Command Action Type
 *
 * Type of action a command performs
 */
enum class CommandActionType {
    CLICK,
    LONG_CLICK,
    TYPE,
    NAVIGATE,
    CUSTOM
}
