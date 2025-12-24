/**
 * VUIDElement.kt - Core data model for VUID-managed elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/models/VUIDElement.kt
 *
 * Migration: UUID → VUID (VoiceUniqueID)
 * Author: AVAMagic Ecosystem v2.0
 * Created: 2025-12-23
 *
 * Data model representing a UI element with VUID (VoiceUniqueID) and metadata.
 * VUID uses standard UUID v4 format but with Voice-specific terminology.
 */

package com.augmentalis.uuidcreator.models

import java.util.UUID

/**
 * Core data model for VUID-managed elements
 *
 * VUID (VoiceUniqueID) uses standard UUID v4 format internally,
 * but provides Voice-specific terminology and semantics.
 *
 * @param vuid Unique identifier for this element (UUID v4 format)
 * @param name Human-readable name/label
 * @param type Element type (button, text, etc.)
 * @param description Optional description for accessibility
 * @param parent Parent element VUID for hierarchy
 * @param children List of child element VUIDs
 * @param position Spatial position information
 * @param actions Available actions for this element
 * @param isEnabled Whether element is currently enabled
 * @param priority Priority for targeting when multiple matches
 * @param metadata Additional metadata
 */
data class VUIDElement(
    val vuid: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val type: String,
    val description: String? = null,
    val parent: String? = null,
    val children: MutableList<String> = mutableListOf(),
    val position: VUIDPosition? = null,
    val actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap(),
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val metadata: VUIDMetadata? = null,
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * Check if this element has a specific action
     */
    fun hasAction(action: String): Boolean = actions.containsKey(action)

    /**
     * Execute an action on this element
     */
    fun executeAction(action: String, parameters: Map<String, Any> = emptyMap()): Boolean {
        if (!isEnabled) return false

        val actionHandler = actions[action] ?: actions["default"] ?: return false

        return try {
            actionHandler(parameters)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if this element is a child of the given parent
     */
    fun isChildOf(parentVuid: String): Boolean = parent == parentVuid

    /**
     * Check if this element is a parent of the given child
     */
    fun isParentOf(childVuid: String): Boolean = children.contains(childVuid)

    /**
     * Add a child element
     */
    fun addChild(childVuid: String) {
        if (!children.contains(childVuid)) {
            children.add(childVuid)
        }
    }

    /**
     * Remove a child element
     */
    fun removeChild(childVuid: String) {
        children.remove(childVuid)
    }

    /**
     * Get element summary for debugging
     */
    fun getSummary(): String {
        return "VUIDElement(vuid=$vuid, name=$name, type=$type, enabled=$isEnabled, children=${children.size})"
    }

    /**
     * Check if element matches search criteria
     */
    fun matches(
        searchName: String? = null,
        searchType: String? = null,
        searchDescription: String? = null
    ): Boolean {
        if (searchName != null && name?.contains(searchName, ignoreCase = true) != true) {
            return false
        }

        if (searchType != null && !type.equals(searchType, ignoreCase = true)) {
            return false
        }

        if (searchDescription != null && description?.contains(searchDescription, ignoreCase = true) != true) {
            return false
        }

        return true
    }

    /**
     * Convert to deprecated UUIDElement for backwards compatibility
     */
    @Deprecated("Use VUIDElement directly")
    fun toUUIDElement(): UUIDElement = UUIDElement(
        uuid = vuid,
        name = name,
        type = type,
        description = description,
        parent = parent,
        children = children,
        position = position?.toUUIDPosition(),
        actions = actions,
        isEnabled = isEnabled,
        priority = priority,
        metadata = metadata?.toUUIDMetadata(),
        timestamp = timestamp
    )

    companion object {
        /**
         * Convert from deprecated UUIDElement
         */
        @Deprecated("Use VUIDElement constructor directly")
        fun fromUUIDElement(element: UUIDElement): VUIDElement = VUIDElement(
            vuid = element.uuid,
            name = element.name,
            type = element.type,
            description = element.description,
            parent = element.parent,
            children = element.children,
            position = element.position?.let { VUIDPosition.fromUUIDPosition(it) },
            actions = element.actions,
            isEnabled = element.isEnabled,
            priority = element.priority,
            metadata = element.metadata?.let { VUIDMetadata.fromUUIDMetadata(it) },
            timestamp = element.timestamp
        )
    }
}
