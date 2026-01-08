/**
 * VUIDRegistry.kt - Central registry for VUID elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/core/VUIDRegistry.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-20
 * Updated: 2025-12-23 - Renamed UUID → VUID (VoiceUniqueID)
 *
 * Thread-safe storage and lookup with SQLDelight persistence via repository
 */

package com.augmentalis.uuidcreator.core

import com.augmentalis.uuidcreator.database.repository.SQLDelightVUIDRepositoryAdapter
import com.augmentalis.uuidcreator.models.VUIDElement
import kotlinx.coroutines.flow.*

/**
 * Central registry for VUID elements
 *
 * Delegates to SQLDelightUUIDRepositoryAdapter for hybrid storage (SQLDelight + in-memory cache).
 * Maintains backward-compatible API while adding persistence.
 *
 * @property repository Hybrid storage repository (SQLDelight database + in-memory cache)
 */
class VUIDRegistry(
    private val repository: SQLDelightVUIDRepositoryAdapter
) {
    
    private val _registrations = MutableSharedFlow<RegistrationEvent>()
    val registrations: SharedFlow<RegistrationEvent> = _registrations.asSharedFlow()
    
    /**
     * Registration events
     */
    sealed class RegistrationEvent {
        data class ElementRegistered(val element: VUIDElement) : RegistrationEvent()
        data class ElementUnregistered(val vuid: String) : RegistrationEvent()
        data class ElementUpdated(val element: VUIDElement) : RegistrationEvent()
    }
    
    /**
     * Register an element
     *
     * Persists to SQLDelight database and updates in-memory cache.
     */
    suspend fun register(element: VUIDElement): String {
        // Delegate to repository (handles database + cache + indexes)
        repository.insert(element)

        // Update parent's children list in cache
        element.parent?.let { parentVuid ->
            repository.getByVuid(parentVuid)?.addChild(element.vuid)
        }

        _registrations.emit(RegistrationEvent.ElementRegistered(element))
        return element.vuid
    }
    
    /**
     * Unregister an element
     *
     * Removes from SQLDelight database and in-memory cache.
     * Recursively unregisters children.
     */
    suspend fun unregister(vuid: String): Boolean {
        val element = repository.getByVuid(vuid) ?: return false

        // Unregister children recursively
        element.children.toList().forEach { childVuid: String ->
            unregister(childVuid)
        }

        // Delete from repository (handles database + cache + indexes)
        val deleted = repository.deleteByVuid(vuid)

        if (deleted) {
            _registrations.emit(RegistrationEvent.ElementUnregistered(vuid))
        }

        return deleted
    }
    
    /**
     * Update an existing element
     *
     * Updates SQLDelight database and in-memory cache.
     */
    suspend fun update(element: VUIDElement): Boolean {
        if (!repository.exists(element.vuid)) return false

        // Delegate to repository (handles database + cache + indexes)
        repository.update(element)

        _registrations.emit(RegistrationEvent.ElementUpdated(element))
        return true
    }

    /**
     * Find element by VUID
     */
    fun findByVUID(vuid: String): VUIDElement? = repository.getByVuid(vuid)

    /**
     * Find elements by name (partial match)
     */
    fun findByName(name: String, exactMatch: Boolean = false): List<VUIDElement> {
        return if (exactMatch) {
            repository.getByName(name).sortedBy { it.priority }
        } else {
            repository.getAll().filter {
                it.name?.contains(name, ignoreCase = true) == true
            }.sortedBy { it.priority }
        }
    }

    /**
     * Find elements by type
     */
    fun findByType(type: String): List<VUIDElement> {
        return repository.getByType(type).sortedBy { it.position?.index ?: Int.MAX_VALUE }
    }

    /**
     * Find children of element
     */
    fun findChildren(parentVuid: String): List<VUIDElement> {
        return repository.getChildren(parentVuid).sortedBy { it.position?.index ?: Int.MAX_VALUE }
    }

    /**
     * Find parent of element
     */
    fun findParent(vuid: String): VUIDElement? {
        val element = repository.getByVuid(vuid) ?: return null
        return element.parent?.let { repository.getByVuid(it) }
    }
    
    /**
     * Find elements by position
     */
    fun findByPosition(index: Int): List<VUIDElement> {
        return repository.getAll().filter {
            it.position?.index == index
        }.sortedBy { it.priority }
    }

    /**
     * Find elements in spatial area
     */
    fun findInArea(minX: Float, minY: Float, maxX: Float, maxY: Float): List<VUIDElement> {
        return repository.getAll().filter { element ->
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
    ): List<VUIDElement> {
        return repository.getAll().filter { element ->
            if (enabledOnly && !element.isEnabled) return@filter false
            element.matches(name, type, description)
        }.sortedBy { it.priority }
    }

    /**
     * Get all elements
     */
    fun getAllElements(): List<VUIDElement> = repository.getAll()

    /**
     * Get enabled elements only
     */
    fun getEnabledElements(): List<VUIDElement> = repository.getAll().filter { it.isEnabled }

    /**
     * Clear all registrations
     *
     * Clears both SQLDelight database and in-memory cache.
     */
    suspend fun clear() {
        val clearedVuids = repository.getAll().map { it.vuid }
        repository.deleteAll()

        clearedVuids.forEach { vuid ->
            _registrations.emit(RegistrationEvent.ElementUnregistered(vuid))
        }
    }

    /**
     * Get statistics
     */
    fun getStats(): RegistryStats {
        val all = repository.getAll()
        val typeBreakdown = all.groupBy { it.type.lowercase() }
            .mapValues { it.value.size }
        val nameCount = all.count { it.name != null }
        val hierarchyCount = all.count { it.children.isNotEmpty() }

        return RegistryStats(
            totalElements = repository.getCount(),
            enabledElements = all.count { it.isEnabled },
            typeBreakdown = typeBreakdown,
            nameIndexSize = nameCount,
            hierarchyIndexSize = hierarchyCount
        )
    }

    /**
     * Check if element exists
     */
    fun exists(vuid: String): Boolean = repository.exists(vuid)

    /**
     * Get element count
     */
    fun size(): Int = repository.getCount()

    /**
     * Check if registry is empty
     */
    fun isEmpty(): Boolean = repository.getCount() == 0

    /**
     * Get recently accessed elements (sorted by access timestamp, descending)
     *
     * This method is used for recent element targeting in voice commands.
     *
     * @param limit Maximum number of elements to return (default: 10)
     * @return List of recently accessed elements
     */
    suspend fun getRecentlyAccessedElements(limit: Int = 10): List<VUIDElement> {
        return repository.getRecentlyUsed(limit)
    }
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