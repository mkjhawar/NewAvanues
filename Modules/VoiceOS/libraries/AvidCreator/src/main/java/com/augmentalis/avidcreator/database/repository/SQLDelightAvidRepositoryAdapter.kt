/**
 * SQLDelightAvidRepositoryAdapter.kt - SQLDelight-based repository adapter
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/uuidcreator/database/repository/SQLDelightAvidRepositoryAdapter.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 *
 * Repository adapter that bridges AvidCreator models to SQLDelight database.
 * Replaces Room-based UUIDRepository.
 */

package com.augmentalis.avidcreator.database.repository

import com.augmentalis.database.dto.AvidElementDTO
import com.augmentalis.database.dto.UUIDHierarchyDTO
import com.augmentalis.database.dto.UUIDAnalyticsDTO
import com.augmentalis.database.dto.UUIDAliasDTO
import com.augmentalis.database.repositories.IUUIDRepository
import com.augmentalis.avidcreator.models.AvidElement
import com.augmentalis.avidcreator.models.VUIDMetadata
import com.augmentalis.avidcreator.models.VUIDPosition
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository adapter for UUID elements using SQLDelight.
 *
 * Implements hybrid storage pattern:
 * - In-memory cache (ConcurrentHashMap) for O(1) lookups
 * - SQLDelight database for persistence across app restarts
 * - Lazy loading on first access
 *
 * Thread-safe: All operations use ConcurrentHashMap and suspend functions
 */
class SQLDelightAvidRepositoryAdapter(
    private val repository: IUUIDRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val gson = Gson()

    // ==================== In-Memory Cache ====================

    /**
     * In-memory cache for fast O(1) lookups
     */
    private val elementsCache = ConcurrentHashMap<String, AvidElement>()

    /**
     * Index by name for fast lookup by name
     */
    private val nameIndex = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Index by type for fast lookup by type
     */
    private val typeIndex = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Index by parent UUID for fast hierarchy lookups
     */
    private val hierarchyIndex = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Index by alias for fast alias→UUID lookups
     */
    private val aliasIndex = ConcurrentHashMap<String, String>()

    /**
     * Flag indicating if cache has been loaded from database
     */
    @Volatile
    private var isLoaded = false

    // ==================== Lazy Loading ====================

    /**
     * Load cache from SQLDelight database on first access
     */
    suspend fun loadCache() = withContext(dispatcher) {
        if (!isLoaded) {
            // Load all elements from database
            val elementDTOs = repository.getAllElements()
            val hierarchyDTOs = repository.getAllHierarchy()
            val aliasDTOs = repository.getAllAliases()

            // Build children map from hierarchy relationships
            val childrenMap = hierarchyDTOs
                .groupBy { it.parentUuid }
                .mapValues { (_, hierarchies) ->
                    hierarchies.sortedBy { it.orderIndex }
                        .map { it.childUuid }
                        .toMutableList()
                }

            // Convert DTOs to models and populate cache
            elementDTOs.forEach { dto ->
                val element = dto.toModel(
                    children = childrenMap[dto.uuid] ?: mutableListOf()
                )
                elementsCache[element.vuid] = element

                // Build indexes
                element.name?.let { name ->
                    nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.vuid)
                }
                typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.vuid)
                element.parent?.let { parent ->
                    hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.vuid)
                }
            }

            // Build alias index
            aliasDTOs.forEach { aliasDTO ->
                aliasIndex[aliasDTO.alias] = aliasDTO.uuid
            }

            isLoaded = true
        }
    }

    /**
     * Check if cache is loaded
     */
    fun isCacheLoaded(): Boolean = isLoaded

    // ==================== CREATE ====================

    /**
     * Insert a new UUID element
     */
    suspend fun insert(element: AvidElement) = withContext(dispatcher) {
        // Save to SQLDelight database
        repository.insertElement(element.toDTO())

        // Save hierarchy relationships
        element.children.forEachIndexed { index, childUuid ->
            repository.insertHierarchy(
                UUIDHierarchyDTO(
                    id = 0,
                    parentUuid = element.vuid,
                    childUuid = childUuid,
                    depth = 0,
                    path = "/${element.vuid}/$childUuid",
                    orderIndex = index
                )
            )
        }

        // Create analytics record
        val now = System.currentTimeMillis()
        repository.insertAnalytics(
            UUIDAnalyticsDTO(
                uuid = element.vuid,
                accessCount = 0,
                firstAccessed = now,
                lastAccessed = now,
                executionTimeMs = 0,
                successCount = 0,
                failureCount = 0,
                lifecycleState = "CREATED"
            )
        )

        // Update in-memory cache
        elementsCache[element.vuid] = element

        // Update indexes
        element.name?.let { name ->
            nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.vuid)
        }
        typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.vuid)
        element.parent?.let { parent ->
            hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.vuid)
        }

        // Auto-create primary alias from element name
        element.name?.let { name ->
            val alias = sanitizeAlias(name)
            if (alias.isNotEmpty() && !repository.aliasExists(alias)) {
                try {
                    repository.insertAlias(
                        UUIDAliasDTO(
                            id = 0,
                            alias = alias,
                            uuid = element.vuid,
                            isPrimary = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    aliasIndex[alias] = element.vuid
                } catch (e: Exception) {
                    // Alias conflict - skip
                }
            }
        }
    }

    /**
     * Insert multiple elements in batch
     */
    suspend fun insertAll(elements: List<AvidElement>) = withContext(dispatcher) {
        elements.forEach { element ->
            insert(element)
        }
    }

    // ==================== READ ====================

    /**
     * Get element by UUID (O(1) from cache)
     */
    fun getByVuid(uuid: String): AvidElement? {
        return elementsCache[uuid]
    }

    /**
     * Get all elements (from cache)
     */
    fun getAll(): List<AvidElement> {
        return elementsCache.values.toList()
    }

    /**
     * Get elements by name (O(1) from index)
     */
    fun getByName(name: String): List<AvidElement> {
        val uuids = nameIndex[name.lowercase()] ?: return emptyList()
        return uuids.mapNotNull { elementsCache[it] }
    }

    /**
     * Get elements by type (O(1) from index)
     */
    fun getByType(type: String): List<AvidElement> {
        val uuids = typeIndex[type.lowercase()] ?: return emptyList()
        return uuids.mapNotNull { elementsCache[it] }
    }

    /**
     * Get direct children of parent (O(1) from index)
     */
    fun getChildren(parentUuid: String): List<AvidElement> {
        val uuids = hierarchyIndex[parentUuid] ?: return emptyList()
        return uuids.mapNotNull { elementsCache[it] }
    }

    /**
     * Get element count
     */
    fun getCount(): Int {
        return elementsCache.size
    }

    /**
     * Check if UUID exists
     */
    fun exists(uuid: String): Boolean {
        return elementsCache.containsKey(uuid)
    }

    /**
     * Get UUID by alias (O(1) from index)
     */
    fun getByAlias(alias: String): AvidElement? {
        val uuid = aliasIndex[alias] ?: return null
        return elementsCache[uuid]
    }

    /**
     * Get UUID string by alias (O(1) from index)
     */
    fun getUuidByAlias(alias: String): String? {
        return aliasIndex[alias]
    }

    /**
     * Get all aliases for UUID
     */
    suspend fun getAliasesByUuid(uuid: String): List<UUIDAliasDTO> = withContext(dispatcher) {
        repository.getAliasesForUuid(uuid)
    }

    // ==================== UPDATE ====================

    /**
     * Update an existing element
     */
    suspend fun update(element: AvidElement) = withContext(dispatcher) {
        val existing = elementsCache[element.vuid] ?: return@withContext

        // Update SQLDelight database
        repository.updateElement(element.toDTO())

        // Update hierarchy if children changed
        if (existing.children != element.children) {
            // Delete old hierarchy relationships
            repository.deleteHierarchyByParent(element.vuid)

            // Insert new relationships
            element.children.forEachIndexed { index, childUuid ->
                repository.insertHierarchy(
                    UUIDHierarchyDTO(
                        id = 0,
                        parentUuid = element.vuid,
                        childUuid = childUuid,
                        depth = 0,
                        path = "/${element.vuid}/$childUuid",
                        orderIndex = index
                    )
                )
            }

            // Update hierarchy index
            hierarchyIndex.remove(element.vuid)
            if (element.children.isNotEmpty()) {
                hierarchyIndex[element.vuid] = element.children.toMutableSet()
            }
        }

        // Update in-memory cache
        elementsCache[element.vuid] = element

        // Update name index if changed
        if (existing.name != element.name) {
            existing.name?.let { oldName ->
                nameIndex[oldName.lowercase()]?.remove(element.vuid)
            }
            element.name?.let { newName ->
                nameIndex.getOrPut(newName.lowercase()) { mutableSetOf() }.add(element.vuid)
            }
        }

        // Update type index if changed
        if (existing.type != element.type) {
            typeIndex[existing.type.lowercase()]?.remove(element.vuid)
            typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.vuid)
        }
    }

    // ==================== DELETE ====================

    /**
     * Delete element by UUID
     */
    suspend fun deleteByVuid(uuid: String): Boolean = withContext(dispatcher) {
        val element = elementsCache.remove(uuid) ?: return@withContext false

        // Delete from SQLDelight database (CASCADE handles hierarchy, analytics, aliases)
        repository.deleteElement(uuid)

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
        }

        // Clean up alias index
        aliasIndex.entries.removeIf { it.value == uuid }

        // Remove from parent's children list in cache
        element.parent?.let { parentUuid ->
            elementsCache[parentUuid]?.removeChild(uuid)
        }

        return@withContext true
    }

    /**
     * Delete all elements
     */
    suspend fun deleteAll() = withContext(dispatcher) {
        // Clear SQLDelight database
        repository.deleteAllElements()

        // Clear in-memory cache
        elementsCache.clear()
        nameIndex.clear()
        typeIndex.clear()
        hierarchyIndex.clear()
        aliasIndex.clear()
    }

    // ==================== Analytics ====================

    /**
     * Record element access for analytics
     */
    suspend fun recordAccess(
        uuid: String,
        executionTimeMs: Long = 0,
        success: Boolean = true
    ) = withContext(dispatcher) {
        repository.recordExecution(
            uuid = uuid,
            executionTimeMs = executionTimeMs,
            success = success,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Get most used elements
     */
    suspend fun getMostUsed(limit: Int = 10): List<AvidElement> = withContext(dispatcher) {
        val analytics = repository.getMostAccessed(limit)
        analytics.mapNotNull { elementsCache[it.uuid] }
    }

    /**
     * Get recently accessed elements
     */
    suspend fun getRecentlyUsed(limit: Int = 10): List<AvidElement> = withContext(dispatcher) {
        val analytics = repository.getRecentlyAccessed(limit)
        analytics.mapNotNull { elementsCache[it.uuid] }
    }

    // ==================== Alias Management ====================

    /**
     * Register a custom alias for a UUID
     */
    suspend fun registerAlias(
        uuid: String,
        alias: String,
        isPrimary: Boolean = false
    ): Boolean = withContext(dispatcher) {
        // Verify UUID exists
        if (!elementsCache.containsKey(uuid)) return@withContext false

        // Sanitize alias
        val sanitized = sanitizeAlias(alias)
        if (sanitized.isEmpty()) return@withContext false

        // Check if alias already exists
        if (repository.aliasExists(sanitized)) return@withContext false

        try {
            repository.insertAlias(
                UUIDAliasDTO(
                    id = 0,
                    alias = sanitized,
                    uuid = uuid,
                    isPrimary = isPrimary,
                    createdAt = System.currentTimeMillis()
                )
            )
            aliasIndex[sanitized] = uuid
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Unregister an alias
     */
    suspend fun unregisterAlias(alias: String): Boolean = withContext(dispatcher) {
        if (!aliasIndex.containsKey(alias)) return@withContext false

        try {
            repository.deleteAliasByName(alias)
            aliasIndex.remove(alias)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if alias exists
     */
    fun aliasExists(alias: String): Boolean {
        return aliasIndex.containsKey(alias)
    }

    // ==================== Conversion Methods ====================

    /**
     * Convert AvidElement model to DTO
     */
    private fun AvidElement.toDTO(): AvidElementDTO {
        return AvidElementDTO(
            uuid = this.vuid,
            name = this.name,
            type = this.type,
            description = this.description,
            parentUuid = this.parent,
            isEnabled = this.isEnabled,
            priority = this.priority,
            timestamp = this.timestamp,
            metadataJson = this.metadata?.let { gson.toJson(it) },
            positionJson = this.position?.let { gson.toJson(it) }
        )
    }

    /**
     * Convert DTO to AvidElement model
     */
    private fun AvidElementDTO.toModel(
        children: MutableList<String> = mutableListOf(),
        actions: Map<String, (Map<String, Any>) -> Unit> = emptyMap()
    ): AvidElement {
        return AvidElement(
            vuid = this.uuid,
            name = this.name,
            type = this.type,
            description = this.description,
            parent = this.parentUuid,
            children = children,
            position = this.positionJson?.let { gson.fromJson(it, VUIDPosition::class.java) },
            actions = actions,
            isEnabled = this.isEnabled,
            priority = this.priority,
            metadata = this.metadataJson?.let { gson.fromJson(it, VUIDMetadata::class.java) },
            timestamp = this.timestamp
        )
    }

    // ==================== Utility Methods ====================

    /**
     * Sanitize alias string
     */
    private fun sanitizeAlias(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(50)
            .takeIf { it.length >= 3 }
            ?: ""
    }
}
