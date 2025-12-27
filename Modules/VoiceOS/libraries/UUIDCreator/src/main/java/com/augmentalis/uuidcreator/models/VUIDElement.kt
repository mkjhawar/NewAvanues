/**
 * VUIDElement.kt - Core data model for VUID-managed elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/models/VUIDElement.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-20
 *
 * Data model representing a UI element with VUID and metadata
 */

package com.augmentalis.uuidcreator.models

import java.util.UUID

/**
 * Core data model for VUID-managed elements
 *
 * @param vuid Unique identifier for this element (Voice UUID)
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
     * Backward-compatible uuid property
     * @deprecated Use vuid instead
     */
    @Suppress("DEPRECATION")
    @Deprecated("Use vuid instead", ReplaceWith("vuid"))
    val uuid: String get() = vuid

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
}