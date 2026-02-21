/**
 * SQLDelightAvidRepositoryAdapter.kt - SQLDelight-based repository adapter
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/avidcreator/database/repository/SQLDelightAvidRepositoryAdapter.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 *
 * Repository adapter that bridges AvidCreator models to SQLDelight database.
 * Replaces Room-based AvidRepository.
 */

package com.augmentalis.avidcreator.database.repository

import com.augmentalis.database.dto.AvidElementDTO
import com.augmentalis.database.dto.AvidHierarchyDTO
import com.augmentalis.database.dto.AvidAnalyticsDTO
import com.augmentalis.database.dto.AvidAliasDTO
import com.augmentalis.database.repositories.IAvidRepository
import com.augmentalis.avidcreator.AvidElement
import com.augmentalis.avidcreator.AvidMetadata
import com.augmentalis.avidcreator.AvidPosition
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository adapter for AVID elements using SQLDelight.
 *
 * Implements hybrid storage pattern:
 * - In-memory cache (ConcurrentHashMap) for O(1) lookups
 * - SQLDelight database for persistence across app restarts
 * - Lazy loading on first access
 *
 * Thread-safe: All operations use ConcurrentHashMap and suspend functions
 */
class SQLDelightAvidRepositoryAdapter(
    private val repository: IAvidRepository,
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
     * Index by parent AVID for fast hierarchy lookups
     */
    private val hierarchyIndex = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * Index by alias for fast alias-to-AVID lookups
     */
    private val aliasIndex = ConcurrentHashMap<String, String>()

    /**
     * Flag indicating if cache has been loaded from database
     */
    @Volatile
    private var isLoaded = false

    private val loadLock = Any()

    // ==================== Lazy Loading ====================

    /**
     * Load cache from SQLDelight database on first access
     */
    suspend fun loadCache() = withContext(dispatcher) {
        if (!isLoaded) {
            synchronized(loadLock) {
                if (!isLoaded) {
                    // Load all elements from database
                    val elementDTOs = repository.getAllElements()
                    val hierarchyDTOs = repository.getAllHierarchy()
                    val aliasDTOs = repository.getAllAliases()

                    // Build children map from hierarchy relationships
                    val childrenMap = hierarchyDTOs
                        .groupBy { it.parentAvid }
                        .mapValues { (_, hierarchies) ->
                            hierarchies.sortedBy { it.orderIndex }
                                .map { it.childAvid }
                                .toMutableList()
                        }

                    // Convert DTOs to models and populate cache
                    elementDTOs.forEach { dto ->
                        val element = dto.toModel(
                            children = childrenMap[dto.avid] ?: mutableListOf()
                        )
                        elementsCache[element.avid] = element

                        // Build indexes
                        element.name?.let { name ->
                            nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.avid)
                        }
                        typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.avid)
                        element.parent?.let { parent ->
                            hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.avid)
                        }
                    }

                    // Build alias index
                    aliasDTOs.forEach { aliasDTO ->
                        aliasIndex[aliasDTO.alias] = aliasDTO.avid
                    }

                    isLoaded = true
                }
            }
        }
    }

    /**
     * Check if cache is loaded
     */
    fun isCacheLoaded(): Boolean = isLoaded

    // ==================== CREATE ====================

    /**
     * Insert a new AVID element
     */
    suspend fun insert(element: AvidElement) = withContext(dispatcher) {
        // Save to SQLDelight database
        repository.insertElement(element.toDTO())

        // Save hierarchy relationships
        element.children.forEachIndexed { index, childAvid ->
            repository.insertHierarchy(
                AvidHierarchyDTO(
                    id = 0,
                    parentAvid = element.avid,
                    childAvid = childAvid,
                    depth = 0,
                    path = "/${element.avid}/$childAvid",
                    orderIndex = index
                )
            )
        }

        // Create analytics record
        val now = System.currentTimeMillis()
        repository.insertAnalytics(
            AvidAnalyticsDTO(
                avid = element.avid,
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
        elementsCache[element.avid] = element

        // Update indexes
        element.name?.let { name ->
            nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.avid)
        }
        typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.avid)
        element.parent?.let { parent ->
            hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.avid)
        }

        // Auto-create primary alias from element name
        element.name?.let { name ->
            val alias = sanitizeAlias(name)
            if (alias.isNotEmpty() && !repository.aliasExists(alias)) {
                try {
                    repository.insertAlias(
                        AvidAliasDTO(
                            id = 0,
                            alias = alias,
                            avid = element.avid,
                            isPrimary = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                    aliasIndex[alias] = element.avid
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
     * Get element by AVID (O(1) from cache)
     */
    fun getByAvid(avid: String): AvidElement? {
        return elementsCache[avid]
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
    fun getChildren(parentAvid: String): List<AvidElement> {
        val uuids = hierarchyIndex[parentAvid] ?: return emptyList()
        return uuids.mapNotNull { elementsCache[it] }
    }

    /**
     * Get element count
     */
    fun getCount(): Int {
        return elementsCache.size
    }

    /**
     * Check if AVID exists
     */
    fun exists(avid: String): Boolean {
        return elementsCache.containsKey(avid)
    }

    /**
     * Get element by alias (O(1) from index)
     */
    fun getByAlias(alias: String): AvidElement? {
        val avid = aliasIndex[alias] ?: return null
        return elementsCache[avid]
    }

    /**
     * Get AVID string by alias (O(1) from index)
     */
    fun getAvidByAlias(alias: String): String? {
        return aliasIndex[alias]
    }

    /**
     * Get all aliases for AVID
     */
    suspend fun getAliasesByAvid(avid: String): List<AvidAliasDTO> = withContext(dispatcher) {
        repository.getAliasesForAvid(avid)
    }

    // ==================== UPDATE ====================

    /**
     * Update an existing element
     */
    suspend fun update(element: AvidElement) = withContext(dispatcher) {
        val existing = elementsCache[element.avid] ?: return@withContext

        // Update SQLDelight database
        repository.updateElement(element.toDTO())

        // Update hierarchy if children changed
        if (existing.children != element.children) {
            // Delete old hierarchy relationships
            repository.deleteHierarchyByParent(element.avid)

            // Insert new relationships
            element.children.forEachIndexed { index, childAvid ->
                repository.insertHierarchy(
                    AvidHierarchyDTO(
                        id = 0,
                        parentAvid = element.avid,
                        childAvid = childAvid,
                        depth = 0,
                        path = "/${element.avid}/$childAvid",
                        orderIndex = index
                    )
                )
            }

            // Update hierarchy index
            hierarchyIndex.remove(element.avid)
            if (element.children.isNotEmpty()) {
                hierarchyIndex[element.avid] = element.children.toMutableSet()
            }
        }

        // Update in-memory cache
        elementsCache[element.avid] = element

        // Update name index if changed
        if (existing.name != element.name) {
            existing.name?.let { oldName ->
                nameIndex[oldName.lowercase()]?.remove(element.avid)
            }
            element.name?.let { newName ->
                nameIndex.getOrPut(newName.lowercase()) { mutableSetOf() }.add(element.avid)
            }
        }

        // Update type index if changed
        if (existing.type != element.type) {
            typeIndex[existing.type.lowercase()]?.remove(element.avid)
            typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.avid)
        }
    }

    // ==================== DELETE ====================

    /**
     * Delete element by AVID
     */
    suspend fun deleteByAvid(avid: String): Boolean = withContext(dispatcher) {
        val element = elementsCache.remove(avid) ?: return@withContext false

        // Delete from SQLDelight database (CASCADE handles hierarchy, analytics, aliases)
        repository.deleteElement(avid)

        // Clean up indexes
        element.name?.let { name ->
            nameIndex[name.lowercase()]?.remove(avid)
            if (nameIndex[name.lowercase()]?.isEmpty() == true) {
                nameIndex.remove(name.lowercase())
            }
        }

        typeIndex[element.type.lowercase()]?.remove(avid)
        if (typeIndex[element.type.lowercase()]?.isEmpty() == true) {
            typeIndex.remove(element.type.lowercase())
        }

        element.parent?.let { parent ->
            hierarchyIndex[parent]?.remove(avid)
            if (hierarchyIndex[parent]?.isEmpty() == true) {
                hierarchyIndex.remove(parent)
            }
        }

        // Clean up alias index
        aliasIndex.entries.removeIf { it.value == avid }

        // Remove from parent's children list in cache
        element.parent?.let { parentAvid ->
            elementsCache[parentAvid]?.removeChild(avid)
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
        avid: String,
        executionTimeMs: Long = 0,
        success: Boolean = true
    ) = withContext(dispatcher) {
        repository.recordExecution(
            avid = avid,
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
        analytics.mapNotNull { elementsCache[it.avid] }
    }

    /**
     * Get recently accessed elements
     */
    suspend fun getRecentlyUsed(limit: Int = 10): List<AvidElement> = withContext(dispatcher) {
        val analytics = repository.getRecentlyAccessed(limit)
        analytics.mapNotNull { elementsCache[it.avid] }
    }

    // ==================== Alias Management ====================

    /**
     * Register a custom alias for an AVID
     */
    suspend fun registerAlias(
        avid: String,
        alias: String,
        isPrimary: Boolean = false
    ): Boolean = withContext(dispatcher) {
        // Verify AVID exists
        if (!elementsCache.containsKey(avid)) return@withContext false

        // Sanitize alias
        val sanitized = sanitizeAlias(alias)
        if (sanitized.isEmpty()) return@withContext false

        // Check if alias already exists
        if (repository.aliasExists(sanitized)) return@withContext false

        try {
            repository.insertAlias(
                AvidAliasDTO(
                    id = 0,
                    alias = sanitized,
                    avid = avid,
                    isPrimary = isPrimary,
                    createdAt = System.currentTimeMillis()
                )
            )
            aliasIndex[sanitized] = avid
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
            avid = this.avid,
            name = this.name,
            type = this.type,
            description = this.description,
            parentAvid = this.parent,
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
            avid = this.avid,
            name = this.name,
            type = this.type,
            description = this.description,
            parent = this.parentAvid,
            children = children,
            position = this.positionJson?.let { gson.fromJson(it, AvidPosition::class.java) },
            actions = actions,
            isEnabled = this.isEnabled,
            priority = this.priority,
            metadata = this.metadataJson?.let { gson.fromJson(it, AvidMetadata::class.java) },
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
