/**
 * VoiceTarget.kt - Voice target registration from legacy UIKitVoiceCommandSystem
 *
 * EXACT port of VoiceTarget data class from working legacy implementation
 */

package com.augmentalis.avidcreator.models

import java.util.UUID

/**
 * Voice target registration - EXACT copy from legacy implementation
 *
 * @param avid Unique identifier (formerly uuid)
 * @param name Element name/label
 * @param type Element type
 * @param description Optional description
 * @param parent Parent element AVID
 * @param children List of child element AVIDs
 * @param position Spatial position information
 * @param actions Available actions for this element
 * @param isEnabled Whether element is currently enabled
 * @param priority Priority for targeting when multiple matches
 */
data class VoiceTarget(
    val avid: String = UUID.randomUUID().toString(),
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
