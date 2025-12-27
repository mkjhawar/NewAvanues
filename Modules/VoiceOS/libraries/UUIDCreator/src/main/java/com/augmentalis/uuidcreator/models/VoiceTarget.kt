/**
 * VoiceTarget.kt - Voice target registration from legacy UIKitVoiceCommandSystem
 * Path: libraries/UUIDManager/src/main/java/com/ai/uuidmgr/models/VoiceTarget.kt
 * 
 * Extracted from: /VOS4/apps/VoiceUI/migration/legacy-backup/uikit/voice/UIKitVoiceCommandSystem.kt
 * Lines: 73-84
 * 
 * EXACT port of VoiceTarget data class from working legacy implementation
 */

package com.augmentalis.uuidcreator.models

import java.util.UUID

/**
 * Voice target registration - EXACT copy from legacy implementation
 * 
 * @param uuid Unique identifier
 * @param name Element name/label
 * @param type Element type
 * @param description Optional description
 * @param parent Parent element UUID
 * @param children List of child element UUIDs
 * @param position Spatial position information
 * @param actions Available actions for this element
 * @param isEnabled Whether element is currently enabled
 * @param priority Priority for targeting when multiple matches
 */
data class VoiceTarget(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val type: String,
    val description: String? = null,
    val parent: String? = null,
    val children: MutableList<String> = mutableListOf(),
    val position: Position? = null,
    val actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap(),
    val isEnabled: Boolean = true,
    val priority: Int = 0
)