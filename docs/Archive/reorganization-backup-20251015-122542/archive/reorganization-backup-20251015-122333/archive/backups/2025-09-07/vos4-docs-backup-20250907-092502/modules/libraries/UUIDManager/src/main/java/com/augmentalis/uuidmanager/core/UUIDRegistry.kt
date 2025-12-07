/**
 * UUIDRegistry.kt - Central registry for UUID elements
 * Path: libraries/UUIDManager/src/main/java/com/ai/uuidmgr/core/UUIDRegistry.kt
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-20
 * 
 * Thread-safe storage and lookup of all registered elements
 */

package com.augmentalis.uuidmanager.core

import com.augmentalis.uuidmanager.models.UUIDElement
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Central registry for UUID elements
 * Thread-safe storage and lookup of all registered elements
 */
class UUIDRegistry {
    
    private val elements = ConcurrentHashMap<String, UUIDElement>()
    private val nameIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val typeIndex = ConcurrentHashMap<String, MutableSet<String>>()
    private val hierarchyIndex = ConcurrentHashMap<String, MutableSet<String>>()
    
    private val _registrations = MutableSharedFlow<RegistrationEvent>()
    val registrations: SharedFlow<RegistrationEvent> = _registrations.asSharedFlow()
    
    /**
     * Registration events
     */
    sealed class RegistrationEvent {
        data class ElementRegistered(val element: UUIDElement) : RegistrationEvent()
        data class ElementUnregistered(val uuid: String) : RegistrationEvent()
        data class ElementUpdated(val element: UUIDElement) : RegistrationEvent()
    }
    
    /**
     * Register an element
     */
    suspend fun register(element: UUIDElement): String {
        elements[element.uuid] = element
        
        // Update indexes
        element.name?.let { name ->
            nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.uuid)
        }
        
        typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.uuid)
        
        element.parent?.let { parent ->
            hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.uuid)
        }
        
        // Update parent's children list
        element.parent?.let { parentUuid ->
            elements[parentUuid]?.addChild(element.uuid)
        }
        
        _registrations.emit(RegistrationEvent.ElementRegistered(element))
        return element.uuid
    }
    
    /**
     * Unregister an element
     */
    suspend fun unregister(uuid: String): Boolean {
        val element = elements.remove(uuid) ?: return false
        
        // Clean up indexes
        element.name?.let { name ->
            nameIndex[name.lowercase()]?.remove(uuid)
            if (nameIndex[name.lowercase()]?.isEmpty() == true) {
                nameIndex.remove(name.lowercase())
            }
        }
        
        typeIndex[element.type.lowercase()]?.remove(uuid)
        if (typeIndex[element.type.lowercase()]?.isEmpty() == true) {
            typeIndex.remove(element.type.lowercase())
        }
        
        element.parent?.let { parent ->
            hierarchyIndex[parent]?.remove(uuid)
            if (hierarchyIndex[parent]?.isEmpty() == true) {
                hierarchyIndex.remove(parent)
            }
            // Remove from parent's children list
            elements[parent]?.removeChild(uuid)
        }
        
        // Unregister children recursively
        element.children.forEach { childUuid ->
            unregister(childUuid)
        }
        
        _registrations.emit(RegistrationEvent.ElementUnregistered(uuid))
        return true
    }
    
    /**
     * Update an existing element
     */
    suspend fun update(element: UUIDElement): Boolean {
        val existing = elements[element.uuid] ?: return false
        
        // Remove old indexes
        existing.name?.let { name ->
            nameIndex[name.lowercase()]?.remove(element.uuid)
        }
        typeIndex[existing.type.lowercase()]?.remove(element.uuid)
        existing.parent?.let { parent ->
            hierarchyIndex[parent]?.remove(element.uuid)
        }
        
        // Update element
        elements[element.uuid] = element
        
        // Rebuild indexes
        element.name?.let { name ->
            nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.uuid)
        }
        typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.uuid)
        element.parent?.let { parent ->
            hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.uuid)
        }
        
        _registrations.emit(RegistrationEvent.ElementUpdated(element))
        return true
    }
    
    /**
     * Find element by UUID
     */
    fun findByUUID(uuid: String): UUIDElement? = elements[uuid]
    
    /**
     * Find elements by name (partial match)
     */
    fun findByName(name: String, exactMatch: Boolean = false): List<UUIDElement> {
        return if (exactMatch) {
            val uuids = nameIndex[name.lowercase()] ?: return emptyList()
            uuids.mapNotNull { elements[it] }.sortedBy { it.priority }
        } else {
            elements.values.filter { 
                it.name?.contains(name, ignoreCase = true) == true 
            }.sortedBy { it.priority }
        }
    }
    
    /**
     * Find elements by type
     */
    fun findByType(type: String): List<UUIDElement> {
        val uuids = typeIndex[type.lowercase()] ?: return emptyList()
        return uuids.mapNotNull { elements[it] }.sortedBy { it.position?.index ?: Int.MAX_VALUE }
    }
    
    /**
     * Find children of element
     */
    fun findChildren(parentUuid: String): List<UUIDElement> {
        val childUuids = hierarchyIndex[parentUuid] ?: return emptyList()
        return childUuids.mapNotNull { elements[it] }.sortedBy { it.position?.index ?: Int.MAX_VALUE }
    }
    
    /**
     * Find parent of element
     */
    fun findParent(uuid: String): UUIDElement? {
        val element = elements[uuid] ?: return null
        return element.parent?.let { elements[it] }
    }
    
    /**
     * Find elements by position
     */
    fun findByPosition(index: Int): List<UUIDElement> {
        return elements.values.filter { 
            it.position?.index == index 
        }.sortedBy { it.priority }
    }
    
    /**
     * Find elements in spatial area
     */
    fun findInArea(minX: Float, minY: Float, maxX: Float, maxY: Float): List<UUIDElement> {
        return elements.values.filter { element ->
            element.position?.let { pos ->
                pos.x >= minX && pos.x <= maxX && pos.y >= minY && pos.y <= maxY
            } ?: false
        }.sortedWith(compareBy({ it.position?.y }, { it.position?.x }))
    }
    
    /**
     * Search elements with criteria
     */
    fun search(
        name: String? = null,
        type: String? = null,
        description: String? = null,
        enabledOnly: Boolean = true
    ): List<UUIDElement> {
        return elements.values.filter { element ->
            if (enabledOnly && !element.isEnabled) return@filter false
            element.matches(name, type, description)
        }.sortedBy { it.priority }
    }
    
    /**
     * Get all elements
     */
    fun getAllElements(): List<UUIDElement> = elements.values.toList()
    
    /**
     * Get enabled elements only
     */
    fun getEnabledElements(): List<UUIDElement> = elements.values.filter { it.isEnabled }
    
    /**
     * Clear all registrations
     */
    suspend fun clear() {
        val clearedUuids = elements.keys.toList()
        elements.clear()
        nameIndex.clear()
        typeIndex.clear()
        hierarchyIndex.clear()
        
        clearedUuids.forEach { uuid ->
            _registrations.emit(RegistrationEvent.ElementUnregistered(uuid))
        }
    }
    
    /**
     * Get statistics
     */
    fun getStats(): RegistryStats = RegistryStats(
        totalElements = elements.size,
        enabledElements = elements.values.count { it.isEnabled },
        typeBreakdown = typeIndex.mapValues { it.value.size },
        nameIndexSize = nameIndex.size,
        hierarchyIndexSize = hierarchyIndex.size
    )
    
    /**
     * Check if element exists
     */
    fun exists(uuid: String): Boolean = elements.containsKey(uuid)
    
    /**
     * Get element count
     */
    fun size(): Int = elements.size
    
    /**
     * Check if registry is empty
     */
    fun isEmpty(): Boolean = elements.isEmpty()
}

/**
 * Registry statistics
 */
data class RegistryStats(
    val totalElements: Int,
    val enabledElements: Int,
    val typeBreakdown: Map<String, Int>,
    val nameIndexSize: Int,
    val hierarchyIndexSize: Int
)