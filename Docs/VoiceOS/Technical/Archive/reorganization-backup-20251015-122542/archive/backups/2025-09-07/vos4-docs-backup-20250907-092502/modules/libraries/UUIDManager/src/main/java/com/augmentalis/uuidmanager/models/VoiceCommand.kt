/**
 * VoiceCommand.kt - Voice command data from legacy UIKitVoiceCommandSystem
 * Path: libraries/UUIDManager/src/main/java/com/ai/uuidmgr/models/VoiceCommand.kt
 * 
 * Extracted from: /VOS4/apps/VoiceUI/migration/legacy-backup/uikit/voice/UIKitVoiceCommandSystem.kt
 * Lines: 61-70
 * 
 * EXACT port of VoiceCommand data class from working legacy implementation
 */

package com.augmentalis.uuidmanager.models

import java.util.UUID

/**
 * Voice command data - EXACT copy from legacy implementation
 * 
 * @param id Unique command identifier
 * @param text Original voice command text
 * @param targetType Type of targeting to use
 * @param targetId Target identifier (UUID, name, etc.)
 * @param action Action to perform
 * @param parameters Command parameters
 * @param confidence Recognition confidence level
 * @param timestamp Command timestamp
 */
data class VoiceCommand(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val targetType: TargetType,
    val targetId: String? = null,
    val action: String,
    val parameters: Map<String, Any> = emptyMap(),
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
)