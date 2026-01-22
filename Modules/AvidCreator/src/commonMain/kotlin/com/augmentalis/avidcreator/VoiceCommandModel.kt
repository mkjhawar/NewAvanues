/**
 * VoiceCommandModel.kt - Voice command data model
 *
 * Cross-platform KMP model for voice commands.
 *
 * @param id Unique command identifier
 * @param text Original voice command text
 * @param targetType Type of targeting to use
 * @param targetId Target identifier (AVID, name, etc.)
 * @param action Action to perform
 * @param parameters Command parameters
 * @param confidence Recognition confidence level
 * @param timestamp Command timestamp
 */
package com.augmentalis.avidcreator

import com.augmentalis.avid.core.AvidGenerator
import kotlinx.datetime.Clock

/**
 * Voice command data
 */
data class VoiceCommand(
    val id: String = AvidGenerator.generateCompactSimple(AvidGenerator.Module.VOICEOS, "intent"),
    val text: String,
    val targetType: TargetType,
    val targetId: String? = null,
    val action: String,
    val parameters: Map<String, Any> = emptyMap(),
    val confidence: Float = 1.0f,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)
