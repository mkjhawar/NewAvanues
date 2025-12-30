/**
 * CommandResult.kt - Command result from legacy UIKitVoiceCommandSystem
 * Path: libraries/UUIDManager/src/main/java/com/ai/uuidmgr/models/CommandResult.kt
 * 
 * Extracted from: /VOS4/apps/VoiceUI/migration/legacy-backup/uikit/voice/UIKitVoiceCommandSystem.kt
 * Lines: 97-102
 * 
 * EXACT port of CommandResult data class from working legacy implementation
 */

package com.augmentalis.uuidcreator.models

/**
 * Command result - EXACT copy from legacy implementation
 * 
 * @param success Whether command executed successfully
 * @param targetId Target identifier that was processed
 * @param message Result message
 * @param data Optional result data
 */
data class CommandResult(
    val success: Boolean,
    val targetId: String? = null,
    val message: String? = null,
    val data: Any? = null
)