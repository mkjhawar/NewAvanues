/**
 * UUIDRegistry.kt - Central registry for UUID elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/core/UUIDRegistry.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2024-08-20
 * Updated: 2025-10-08 - Migrated to Room hybrid storage
 *
 * Thread-safe storage and lookup with Room persistence via UUIDRepository
 */

package com.augmentalis.uuidcreator.core

import com.augmentalis.uuidcreator.database.repository.SQLDelightUUIDRepositoryAdapter
import com.augmentalis.uuidcreator.models.UUIDElement
import kotlinx.coroutines.flow.*

/**
 * Central registry for UUID elements
 *
 * Now delegates to SQLDelightUUIDRepositoryAdapter for hybrid storage (SQLDelight + in-memory cache).
 * Maintains backward-compatible API while adding persistence.
 *
 * @property repository Hybrid storage repository (SQLDelight database + in-memory cache)
 */
class UUIDRegistry(
    private val repository: SQLDelightUUIDRepositoryAdapter
) {
    
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
     *
     * Persists to Room database and updates in-memory cache.
     */
    suspend fun register(element: UUIDElement): String {
        // Delegate to repository (handles database + cache + indexes)
        repository.insert(element)

        // Update parent's children list in cache
        element.parent?.let { parentUuid ->
            repository.getByUuid(parentUuid)?.addChild(element.uuid)
        }

        _registrations.emit(RegistrationEvent.ElementRegistered(element))
        return element.uuid
    }
    
    /**
     * Unregister an element
     *
     * Removes from Room database and in-memory cache.
     * Recursively unregisters children.
     */
    suspend fun unregister(uuid: String): Boolean {
        val element = repository.getByUuid(uuid) ?: return false

        // Unregister children recursively
        element.children.toList().forEach { childUuid ->
            unregister(childUuid)
        }

        // Delete from repository (handles database + cache + indexes)
        val deleted = repository.deleteByUuid(uuid)

        if (deleted) {
            _registrations.emit(RegistrationEvent.ElementUnregistered(uuid))
        }

        return deleted
    }
    
    /**
     * Update an existing element
     *
     * Updates Room database and in-memory cache.
     */
    suspend fun update(element: UUIDElement): Boolean {
        if (!repository.exists(element.uuid)) return false

        // Delegate to repository (handles database + cache + indexes)
        repository.update(element)

        _registrations.emit(RegistrationEvent.ElementUpdated(element))
        return true
    }
    
    /**
     * Find element by UUID
     */
    fun findByUUID(uuid: String): UUIDElement? = repository.getByUuid(uuid)

    /**
     * Find elements by name (partial match)
     */
    fun findByName(name: String, exactMatch: Boolean = false): List<UUIDElement> {
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
    fun findByType(type: String): List<UUIDElement> {
        return repository.getByType(type).sortedBy { it.position?.index ?: Int.MAX_VALUE }
    }

    /**
     * Find children of element
     */
    fun findChildren(parentUuid: String): List<UUIDElement> {
        return repository.getChildren(parentUuid).sortedBy { it.position?.index ?: Int.MAX_VALUE }
    }

    /**
     * Find parent of element
     */
    fun findParent(uuid: String): UUIDElement? {
        val element = repository.getByUuid(uuid) ?: return null
        return element.parent?.let { repository.getByUuid(it) }
    }
    
    /**
     * Find elements by position
     */
    fun findByPosition(index: Int): List<UUIDElement> {
        return repository.getAll().filter {
            it.position?.index == index
        }.sortedBy { it.priority }
    }

    /**
     * Find elements in spatial area
     */
    fun findInArea(minX: Float, minY: Float, maxX: Float, maxY: Float): List<UUIDElement> {
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
    ): List<UUIDElement> {
        return repository.getAll().filter { element ->
            if (enabledOnly && !element.isEnabled) return@filter false
            element.matches(name, type, description)
        }.sortedBy { it.priority }
    }
    
    /**
     * Get all elements
     */
    fun getAllElements(): List<UUIDElement> = repository.getAll()

    /**
     * Get enabled elements only
     */
    fun getEnabledElements(): List<UUIDElement> = repository.getAll().filter { it.isEnabled }

    /**
     * Clear all registrations
     *
     * Clears both Room database and in-memory cache.
     */
    suspend fun clear() {
        val clearedUuids = repository.getAll().map { it.uuid }
        repository.deleteAll()

        clearedUuids.forEach { uuid ->
            _registrations.emit(RegistrationEvent.ElementUnregistered(uuid))
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
    fun exists(uuid: String): Boolean = repository.exists(uuid)

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
    suspend fun getRecentlyAccessedElements(limit: Int = 10): List<UUIDElement> {
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