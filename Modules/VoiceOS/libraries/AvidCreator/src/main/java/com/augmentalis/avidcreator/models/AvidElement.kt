/**
 * AvidElement.kt - Core data model for AVID-managed elements
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-20
 * Updated: 2026-01-15 - Migrated to AVID naming
 *
 * Data model representing a UI element with AVID and metadata
 */

package com.augmentalis.avidcreator.models

import java.util.UUID

/**
 * Core data model for AVID-managed elements
 *
 * @param avid Unique identifier for this element (Augmentalis Voice ID)
 * @param name Human-readable name/label
 * @param type Element type (button, text, etc.)
 * @param description Optional description for accessibility
 * @param parent Parent element AVID for hierarchy
 * @param children List of child element AVIDs
 * @param position Spatial position information
 * @param actions Available actions for this element
 * @param isEnabled Whether element is currently enabled
 * @param priority Priority for targeting when multiple matches
 * @param metadata Additional metadata
 */
data class AvidElement(
    val avid: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val type: String,
    val description: String? = null,
    val parent: String? = null,
    val children: MutableList<String> = mutableListOf(),
    val position: AvidPosition? = null,
    val actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap(),
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val metadata: AvidMetadata? = null,
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
    fun isChildOf(parentAvid: String): Boolean = parent == parentAvid

    /**
     * Check if this element is a parent of the given child
     */
    fun isParentOf(childAvid: String): Boolean = children.contains(childAvid)

    /**
     * Add a child element
     */
    fun addChild(childAvid: String) {
        if (!children.contains(childAvid)) {
            children.add(childAvid)
        }
    }

    /**
     * Remove a child element
     */
    fun removeChild(childAvid: String) {
        children.remove(childAvid)
    }

    /**
     * Get element summary for debugging
     */
    fun getSummary(): String {
        return "AvidElement(avid=$avid, name=$name, type=$type, enabled=$isEnabled, children=${children.size})"
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
}

// Backward compatibility aliases
@Deprecated("Use AvidElement instead", ReplaceWith("AvidElement"))
typealias AvidElement = AvidElement

@Deprecated("Use AvidElement instead", ReplaceWith("AvidElement"))
typealias AvidElement = AvidElement
