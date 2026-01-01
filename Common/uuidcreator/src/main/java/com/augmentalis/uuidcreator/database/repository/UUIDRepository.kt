/**
 * UUIDRepository.kt - Hybrid storage repository for UUID elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/repository/UUIDRepository.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Repository implementing hybrid storage pattern (Room + in-memory cache)
 */

package com.augmentalis.uuidcreator.database.repository

import com.augmentalis.uuidcreator.database.dao.UUIDAliasDao
import com.augmentalis.uuidcreator.database.dao.UUIDAnalyticsDao
import com.augmentalis.uuidcreator.database.dao.UUIDElementDao
import com.augmentalis.uuidcreator.database.dao.UUIDHierarchyDao
import com.augmentalis.uuidcreator.database.converters.*
import com.augmentalis.uuidcreator.database.entities.UUIDAliasEntity
import com.augmentalis.uuidcreator.models.UUIDElement
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for UUID elements with hybrid storage
 *
 * Implements hybrid storage pattern:
 * - In-memory cache (ConcurrentHashMap) for O(1) lookups
 * - Room database for persistence across app restarts
 * - Lazy loading on first access
 *
 * Thread-safe: All operations use ConcurrentHashMap and suspend functions
 */
class UUIDRepository(
    private val elementDao: UUIDElementDao,
    private val hierarchyDao: UUIDHierarchyDao,
    private val analyticsDao: UUIDAnalyticsDao,
    private val aliasDao: UUIDAliasDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    // ==================== In-Memory Cache ====================

    /**
     * In-memory cache for fast O(1) lookups
     * Replaces existing ConcurrentHashMap in UUIDRegistry
     */
    private val elementsCache = ConcurrentHashMap<String, UUIDElement>()

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
     * Load cache from Room database on first access
     *
     * This is called once when UUIDCreator is first instantiated.
     * All subsequent operations use the in-memory cache.
     */
    suspend fun loadCache() = withContext(dispatcher) {
        if (!isLoaded) {
            // Load all elements from database
            val entities = elementDao.getAll()
            val hierarchies = hierarchyDao.getAll()
            val aliases = aliasDao.getAll()

            // Build children map from hierarchy relationships
            val childrenMap = hierarchies.toChildrenMap()

            // Convert entities to models and populate cache
            entities.forEach { entity ->
                val element = entity.toModel(
                    children = childrenMap[entity.uuid] ?: mutableListOf()
                    // Note: Actions are not persisted, will be re-registered by UI components
                )
                elementsCache[element.uuid] = element

                // Build indexes
                element.name?.let { name ->
                    nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.uuid)
                }
                typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.uuid)
                element.parent?.let { parent ->
                    hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.uuid)
                }
            }

            // Build alias index
            aliases.forEach { aliasEntity ->
                aliasIndex[aliasEntity.alias] = aliasEntity.uuid
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
     *
     * Updates both Room database and in-memory cache.
     *
     * @param element UUIDElement to insert
     */
    suspend fun insert(element: UUIDElement) = withContext(dispatcher) {
        // Save to Room database
        elementDao.insert(element.toEntity())

        // Save hierarchy relationships
        element.children.forEachIndexed { index, childUuid ->
            hierarchyDao.insert(
                createHierarchyEntity(
                    parentUuid = element.uuid,
                    childUuid = childUuid,
                    depth = 0,
                    orderIndex = index
                )
            )
        }

        // Create analytics record
        analyticsDao.insert(createAnalyticsEntity(element.uuid))

        // Update in-memory cache
        elementsCache[element.uuid] = element

        // Update indexes
        element.name?.let { name ->
            nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.uuid)
        }
        typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.uuid)
        element.parent?.let { parent ->
            hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.uuid)
        }

        // Auto-create primary alias from element name
        element.name?.let { name ->
            val alias = sanitizeAlias(name)
            if (alias.isNotEmpty() && !aliasDao.exists(alias)) {
                try {
                    val aliasEntity = UUIDAliasEntity(
                        alias = alias,
                        uuid = element.uuid,
                        isPrimary = true,
                        createdAt = System.currentTimeMillis()
                    )
                    aliasDao.insert(aliasEntity)
                    aliasIndex[alias] = element.uuid
                } catch (e: Exception) {
                    // Alias conflict - skip (element still registered)
                }
            }
        }
    }

    /**
     * Insert multiple elements in batch
     *
     * @param elements List of UUIDElement to insert
     */
    suspend fun insertAll(elements: List<UUIDElement>) = withContext(dispatcher) {
        // Save to Room database
        elementDao.insertAll(elements.toEntities())

        // Save hierarchy relationships
        val hierarchies = elements.flatMap { element ->
            element.children.mapIndexed { index, childUuid ->
                createHierarchyEntity(
                    parentUuid = element.uuid,
                    childUuid = childUuid,
                    depth = 0,
                    orderIndex = index
                )
            }
        }
        if (hierarchies.isNotEmpty()) {
            hierarchyDao.insertAll(hierarchies)
        }

        // Create analytics records
        val analytics = elements.map { createAnalyticsEntity(it.uuid) }
        analyticsDao.insertAll(analytics)

        // Update in-memory cache and indexes
        elements.forEach { element ->
            elementsCache[element.uuid] = element

            element.name?.let { name ->
                nameIndex.getOrPut(name.lowercase()) { mutableSetOf() }.add(element.uuid)
            }
            typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.uuid)
            element.parent?.let { parent ->
                hierarchyIndex.getOrPut(parent) { mutableSetOf() }.add(element.uuid)
            }
        }

        // Auto-create primary aliases for elements with names
        val aliasEntities = elements.mapNotNull { element ->
            element.name?.let { name ->
                val alias = sanitizeAlias(name)
                if (alias.isNotEmpty()) {
                    UUIDAliasEntity(
                        alias = alias,
                        uuid = element.uuid,
                        isPrimary = true,
                        createdAt = System.currentTimeMillis()
                    )
                } else null
            }
        }
        if (aliasEntities.isNotEmpty()) {
            try {
                aliasDao.insertAll(aliasEntities)
                aliasEntities.forEach { aliasEntity ->
                    aliasIndex[aliasEntity.alias] = aliasEntity.uuid
                }
            } catch (e: Exception) {
                // Some aliases may conflict - insert individually
                aliasEntities.forEach { aliasEntity ->
                    try {
                        if (!aliasDao.exists(aliasEntity.alias)) {
                            aliasDao.insert(aliasEntity)
                            aliasIndex[aliasEntity.alias] = aliasEntity.uuid
                        }
                    } catch (ignored: Exception) {
                        // Skip conflicting alias
                    }
                }
            }
        }
    }

    // ==================== READ ====================

    /**
     * Get element by UUID (O(1) from cache)
     *
     * @param uuid UUID of element
     * @return UUIDElement or null if not found
     */
    fun getByUuid(uuid: String): UUIDElement? {
        return elementsCache[uuid]
    }

    /**
     * Get all elements (from cache)
     *
     * @return List of all UUIDElement
     */
    fun getAll(): List<UUIDElement> {
        return elementsCache.values.toList()
    }

    /**
     * Get elements by name (O(1) from index)
     *
     * @param name Element name (case-insensitive)
     * @return List of matching UUIDElement
     */
    fun getByName(name: String): List<UUIDElement> {
        val uuids = nameIndex[name.lowercase()] ?: return emptyList()
        return uuids.mapNotNull { elementsCache[it] }
    }

    /**
     * Get elements by type (O(1) from index)
     *
     * @param type Element type (case-insensitive)
     * @return List of matching UUIDElement
     */
    fun getByType(type: String): List<UUIDElement> {
        val uuids = typeIndex[type.lowercase()] ?: return emptyList()
        return uuids.mapNotNull { elementsCache[it] }
    }

    /**
     * Get direct children of parent (O(1) from index)
     *
     * @param parentUuid Parent UUID
     * @return List of child UUIDElement
     */
    fun getChildren(parentUuid: String): List<UUIDElement> {
        val uuids = hierarchyIndex[parentUuid] ?: return emptyList()
        return uuids.mapNotNull { elementsCache[it] }
    }

    /**
     * Get element count
     *
     * @return Total number of elements
     */
    fun getCount(): Int {
        return elementsCache.size
    }

    /**
     * Check if UUID exists
     *
     * @param uuid UUID to check
     * @return true if exists, false otherwise
     */
    fun exists(uuid: String): Boolean {
        return elementsCache.containsKey(uuid)
    }

    /**
     * Get UUID by alias (O(1) from index)
     *
     * @param alias Alias string to look up
     * @return UUID or null if alias not found
     */
    fun getByAlias(alias: String): UUIDElement? {
        val uuid = aliasIndex[alias] ?: return null
        return elementsCache[uuid]
    }

    /**
     * Get UUID string by alias (O(1) from index)
     *
     * @param alias Alias string to look up
     * @return UUID string or null if alias not found
     */
    fun getUuidByAlias(alias: String): String? {
        return aliasIndex[alias]
    }

    /**
     * Get all aliases for UUID
     *
     * @param uuid UUID to get aliases for
     * @return List of alias entities
     */
    suspend fun getAliasesByUuid(uuid: String): List<UUIDAliasEntity> = withContext(dispatcher) {
        aliasDao.getAliasesByUuid(uuid)
    }

    // ==================== UPDATE ====================

    /**
     * Update an existing element
     *
     * Updates both Room database and in-memory cache.
     *
     * @param element Updated UUIDElement
     */
    suspend fun update(element: UUIDElement) = withContext(dispatcher) {
        val existing = elementsCache[element.uuid] ?: return@withContext

        // Update Room database
        elementDao.update(element.toEntity())

        // Update hierarchy if children changed
        if (existing.children != element.children) {
            // Delete old hierarchy relationships
            hierarchyDao.deleteByParent(element.uuid)

            // Insert new relationships
            element.children.forEachIndexed { index, childUuid ->
                hierarchyDao.insert(
                    createHierarchyEntity(
                        parentUuid = element.uuid,
                        childUuid = childUuid,
                        depth = 0,
                        orderIndex = index
                    )
                )
            }

            // Update hierarchy index
            hierarchyIndex.remove(element.uuid)
            if (element.children.isNotEmpty()) {
                hierarchyIndex[element.uuid] = element.children.toMutableSet()
            }
        }

        // Update in-memory cache
        elementsCache[element.uuid] = element

        // Update name index if changed
        if (existing.name != element.name) {
            existing.name?.let { oldName ->
                nameIndex[oldName.lowercase()]?.remove(element.uuid)
            }
            element.name?.let { newName ->
                nameIndex.getOrPut(newName.lowercase()) { mutableSetOf() }.add(element.uuid)
            }
        }

        // Update type index if changed
        if (existing.type != element.type) {
            typeIndex[existing.type.lowercase()]?.remove(element.uuid)
            typeIndex.getOrPut(element.type.lowercase()) { mutableSetOf() }.add(element.uuid)
        }
    }

    // ==================== DELETE ====================

    /**
     * Delete element by UUID
     *
     * Updates both Room database and in-memory cache.
     * Cascade delete handles hierarchy and analytics (foreign keys).
     *
     * @param uuid UUID of element to delete
     * @return true if deleted, false if not found
     */
    suspend fun deleteByUuid(uuid: String): Boolean = withContext(dispatcher) {
        val element = elementsCache.remove(uuid) ?: return@withContext false

        // Delete from Room database (CASCADE handles hierarchy, analytics, and aliases)
        elementDao.deleteByUuid(uuid)

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

        // Clean up alias index (foreign key CASCADE deletes from DB)
        aliasIndex.entries.removeIf { it.value == uuid }

        // Remove from parent's children list in cache
        element.parent?.let { parentUuid ->
            elementsCache[parentUuid]?.removeChild(uuid)
        }

        return@withContext true
    }

    /**
     * Delete all elements
     *
     * Clears both Room database and in-memory cache.
     */
    suspend fun deleteAll() = withContext(dispatcher) {
        // Clear Room database
        elementDao.deleteAll()
        hierarchyDao.deleteAll()
        analyticsDao.deleteAll()
        aliasDao.deleteAll()

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
     *
     * @param uuid UUID of accessed element
     * @param executionTimeMs Execution time in milliseconds
     * @param success Whether access was successful
     */
    suspend fun recordAccess(
        uuid: String,
        executionTimeMs: Long = 0,
        success: Boolean = true
    ) = withContext(dispatcher) {
        val analytics = analyticsDao.getByUuid(uuid) ?: createAnalyticsEntity(uuid)
        val updated = analytics.recordAccess(executionTimeMs, success)
        analyticsDao.update(updated)
    }

    /**
     * Get most used elements
     *
     * @param limit Maximum number of results
     * @return List of most frequently accessed elements
     */
    suspend fun getMostUsed(limit: Int = 10): List<UUIDElement> = withContext(dispatcher) {
        val analytics = analyticsDao.getMostUsed(limit)
        analytics.mapNotNull { elementsCache[it.uuid] }
    }

    /**
     * Get least used elements
     *
     * @param limit Maximum number of results
     * @return List of least frequently accessed elements
     */
    suspend fun getLeastUsed(limit: Int = 10): List<UUIDElement> = withContext(dispatcher) {
        val analytics = analyticsDao.getLeastUsed(limit)
        analytics.mapNotNull { elementsCache[it.uuid] }
    }

    /**
     * Get recently accessed elements
     *
     * Returns elements sorted by most recent access timestamp (descending).
     *
     * @param limit Maximum number of results
     * @return List of recently accessed elements
     */
    suspend fun getRecentlyUsed(limit: Int = 10): List<UUIDElement> = withContext(dispatcher) {
        val analytics = analyticsDao.getRecentlyUsed(limit)
        analytics.mapNotNull { elementsCache[it.uuid] }
    }

    // ==================== Alias Management ====================

    /**
     * Register a custom alias for a UUID
     *
     * Allows manual registration of human-readable aliases for elements.
     *
     * @param uuid UUID to create alias for
     * @param alias Alias string (will be sanitized)
     * @param isPrimary Whether this is the primary alias
     * @return true if alias created, false if UUID not found or alias exists
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
        if (aliasDao.exists(sanitized)) return@withContext false

        try {
            val aliasEntity = UUIDAliasEntity(
                alias = sanitized,
                uuid = uuid,
                isPrimary = isPrimary,
                createdAt = System.currentTimeMillis()
            )
            aliasDao.insert(aliasEntity)
            aliasIndex[sanitized] = uuid
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Unregister an alias
     *
     * @param alias Alias to remove
     * @return true if alias was removed, false if not found
     */
    suspend fun unregisterAlias(alias: String): Boolean = withContext(dispatcher) {
        if (!aliasIndex.containsKey(alias)) return@withContext false

        try {
            aliasDao.deleteByAlias(alias)
            aliasIndex.remove(alias)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if alias exists
     *
     * @param alias Alias to check
     * @return true if alias exists, false otherwise
     */
    fun aliasExists(alias: String): Boolean {
        return aliasIndex.containsKey(alias)
    }

    // ==================== Utility Methods ====================

    /**
     * Sanitize alias string
     *
     * Converts to lowercase, replaces spaces/special chars with underscores,
     * removes consecutive underscores, and enforces length constraints.
     *
     * Format: lowercase alphanumeric + underscores, 3-50 characters
     *
     * Examples:
     * - "Instagram Like Button" → "instagram_like_button"
     * - "Submit   Form" → "submit_form"
     * - "Menu-Item#1" → "menu_item_1"
     *
     * @param input Raw alias string
     * @return Sanitized alias or empty string if invalid
     */
    private fun sanitizeAlias(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "_")  // Replace non-alphanumeric with underscore
            .replace(Regex("_+"), "_")           // Replace multiple underscores with single
            .trim('_')                           // Remove leading/trailing underscores
            .take(50)                            // Max 50 characters
            .takeIf { it.length >= 3 }           // Min 3 characters
            ?: ""
    }
}
