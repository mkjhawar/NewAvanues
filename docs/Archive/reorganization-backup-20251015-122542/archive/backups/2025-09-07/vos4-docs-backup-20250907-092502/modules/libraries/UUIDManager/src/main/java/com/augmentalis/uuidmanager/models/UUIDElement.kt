/**
 * UUIDElement.kt - Core data model for UUID-managed elements
 * Path: libraries/UUIDManager/src/main/java/com/ai/uuidmgr/models/UUIDElement.kt
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-20
 * 
 * Data model representing a UI element with UUID and metadata
 */

package com.augmentalis.uuidmanager.models

import java.util.UUID

/**
 * Core data model for UUID-managed elements
 * 
 * @param uuid Unique identifier for this element
 * @param name Human-readable name/label
 * @param type Element type (button, text, etc.)
 * @param description Optional description for accessibility
 * @param parent Parent element UUID for hierarchy
 * @param children List of child element UUIDs
 * @param position Spatial position information
 * @param actions Available actions for this element
 * @param isEnabled Whether element is currently enabled
 * @param priority Priority for targeting when multiple matches
 * @param metadata Additional metadata
 */
data class UUIDElement(
    val uuid: String = UUID.randomUUID().toString(),
    val name: String? = null,
    val type: String,
    val description: String? = null,
    val parent: String? = null,
    val children: MutableList<String> = mutableListOf(),
    val position: UUIDPosition? = null,
    val actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap(),
    val isEnabled: Boolean = true,
    val priority: Int = 0,
    val metadata: UUIDMetadata? = null,
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
    fun isChildOf(parentUuid: String): Boolean = parent == parentUuid
    
    /**
     * Check if this element is a parent of the given child
     */
    fun isParentOf(childUuid: String): Boolean = children.contains(childUuid)
    
    /**
     * Add a child element
     */
    fun addChild(childUuid: String) {
        if (!children.contains(childUuid)) {
            children.add(childUuid)
        }
    }
    
    /**
     * Remove a child element
     */
    fun removeChild(childUuid: String) {
        children.remove(childUuid)
    }
    
    /**
     * Get element summary for debugging
     */
    fun getSummary(): String {
        return "UUIDElement(uuid=$uuid, name=$name, type=$type, enabled=$isEnabled, children=${children.size})"
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