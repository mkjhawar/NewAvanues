/**
 * VoiceTargetModel.kt - Voice target registration
 *
 * Cross-platform KMP model for voice targets.
 *
 * @param avid Unique identifier
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
package com.augmentalis.avidcreator

import com.augmentalis.avid.core.AvidGenerator

/**
 * Voice target registration
 */
data class VoiceTarget(
    val avid: String = AvidGenerator.generateCompactSimple(AvidGenerator.Module.VOICEOS, "element"),
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
