/**
 * UUIDCreatorDatabase.kt - UUID database access (IUUIDRepository implementation)
 * Path: modules/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/UUIDCreatorDatabase.kt
 *
 * Author: VoiceOS Restoration Team
 * Created: 2025-11-27
 * Modified: 2025-12-17 (Implements IUUIDRepository for UuidAliasManager compatibility)
 *
 * Implementation of IUUIDRepository that provides in-memory storage
 * for UUID elements, hierarchies, analytics, and aliases.
 */

package com.augmentalis.uuidcreator.database

import android.content.Context
import com.augmentalis.database.dto.UUIDAliasDTO
import com.augmentalis.database.dto.UUIDAnalyticsDTO
import com.augmentalis.database.dto.UUIDElementDTO
import com.augmentalis.database.dto.UUIDHierarchyDTO
import com.augmentalis.database.repositories.IUUIDRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * UUID Creator Database
 *
 * Implements IUUIDRepository with in-memory storage for UUID management.
 * Provides alias management, element storage, hierarchy tracking, and analytics.
 *
 * **Features:**
 * - Implements IUUIDRepository interface
 * - In-memory storage for fast access
 * - Thread-safe via ConcurrentHashMap
 * - Compatible with UuidAliasManager
 *
 * **Usage:**
 * ```kotlin
 * val uuidDb = UUIDCreatorDatabase.getInstance(context)
 * val aliasManager = UuidAliasManager(uuidDb)
 * ```
 *
 * @property context Application context
 */
class UUIDCreatorDatabase private constructor(
    private val context: Context
) : IUUIDRepository {

    companion object {
        @Volatile
        private var INSTANCE: UUIDCreatorDatabase? = null

        /**
         * Get singleton instance
         *
         * @param context Application context
         * @return Database instance
         */
        fun getInstance(context: Context): UUIDCreatorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UUIDCreatorDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // In-memory storage
    private val elements = ConcurrentHashMap<String, UUIDElementDTO>()
    private val hierarchies = ConcurrentHashMap<String, MutableList<UUIDHierarchyDTO>>()
    private val analytics = ConcurrentHashMap<String, UUIDAnalyticsDTO>()
    private val aliases = ConcurrentHashMap<String, UUIDAliasDTO>()
    private val aliasesByUuid = ConcurrentHashMap<String, MutableList<UUIDAliasDTO>>()

    // ==================== Element Operations ====================

    override suspend fun insertElement(element: UUIDElementDTO) {
        elements[element.uuid] = element
    }

    override suspend fun updateElement(element: UUIDElementDTO) {
        elements[element.uuid] = element
    }

    override suspend fun deleteElement(uuid: String) {
        elements.remove(uuid)
    }

    override suspend fun getElementByUuid(uuid: String): UUIDElementDTO? {
        return elements[uuid]
    }

    override suspend fun getAllElements(): List<UUIDElementDTO> {
        return elements.values.toList()
    }

    override suspend fun getElementsByType(type: String): List<UUIDElementDTO> {
        return elements.values.filter { it.type == type }
    }

    override suspend fun getChildrenOfParent(parentUuid: String): List<UUIDElementDTO> {
        return elements.values.filter { it.parentUuid == parentUuid }
    }

    override suspend fun getEnabledElements(): List<UUIDElementDTO> {
        return elements.values.filter { it.isEnabled }
    }

    override suspend fun searchByName(query: String): List<UUIDElementDTO> {
        val lowerQuery = query.lowercase()
        return elements.values.filter {
            it.name?.lowercase()?.contains(lowerQuery) == true ||
            it.description?.lowercase()?.contains(lowerQuery) == true
        }
    }

    override suspend fun countElements(): Long {
        return elements.size.toLong()
    }

    override suspend fun countElementsByType(type: String): Long {
        return elements.values.count { it.type == type }.toLong()
    }

    // ==================== Hierarchy Operations ====================

    override suspend fun insertHierarchy(hierarchy: UUIDHierarchyDTO) {
        val list = hierarchies.getOrPut(hierarchy.parentUuid) { mutableListOf() }
        list.add(hierarchy)
    }

    override suspend fun deleteHierarchyByParent(parentUuid: String) {
        hierarchies.remove(parentUuid)
    }

    override suspend fun getHierarchyByParent(parentUuid: String): List<UUIDHierarchyDTO> {
        return hierarchies[parentUuid] ?: emptyList()
    }

    override suspend fun getAllHierarchy(): List<UUIDHierarchyDTO> {
        return hierarchies.values.flatten()
    }

    // ==================== Analytics Operations ====================

    override suspend fun insertAnalytics(analytics: UUIDAnalyticsDTO) {
        this.analytics[analytics.uuid] = analytics
    }

    override suspend fun updateAnalytics(analytics: UUIDAnalyticsDTO) {
        this.analytics[analytics.uuid] = analytics
    }

    override suspend fun getAnalyticsByUuid(uuid: String): UUIDAnalyticsDTO? {
        return analytics[uuid]
    }

    override suspend fun getAllAnalytics(): List<UUIDAnalyticsDTO> {
        return analytics.values.toList()
    }

    override suspend fun getMostAccessed(limit: Int): List<UUIDAnalyticsDTO> {
        return analytics.values.sortedByDescending { it.accessCount }.take(limit)
    }

    override suspend fun getRecentlyAccessed(limit: Int): List<UUIDAnalyticsDTO> {
        return analytics.values.sortedByDescending { it.lastAccessed }.take(limit)
    }

    override suspend fun incrementAccessCount(uuid: String, timestamp: Long) {
        val existing = analytics[uuid]
        if (existing != null) {
            analytics[uuid] = existing.copy(
                accessCount = existing.accessCount + 1,
                lastAccessed = timestamp
            )
        } else {
            analytics[uuid] = UUIDAnalyticsDTO(
                uuid = uuid,
                accessCount = 1,
                firstAccessed = timestamp,
                lastAccessed = timestamp,
                executionTimeMs = 0,
                successCount = 0,
                failureCount = 0,
                lifecycleState = "active"
            )
        }
    }

    override suspend fun recordExecution(uuid: String, executionTimeMs: Long, success: Boolean, timestamp: Long) {
        val existing = analytics[uuid]
        if (existing != null) {
            val newSuccessCount = if (success) existing.successCount + 1 else existing.successCount
            val newFailureCount = if (!success) existing.failureCount + 1 else existing.failureCount

            analytics[uuid] = existing.copy(
                successCount = newSuccessCount,
                failureCount = newFailureCount,
                executionTimeMs = executionTimeMs,
                lastAccessed = timestamp
            )
        } else {
            analytics[uuid] = UUIDAnalyticsDTO(
                uuid = uuid,
                accessCount = 0,
                firstAccessed = timestamp,
                lastAccessed = timestamp,
                executionTimeMs = executionTimeMs,
                successCount = if (success) 1 else 0,
                failureCount = if (!success) 1 else 0,
                lifecycleState = "active"
            )
        }
    }

    // ==================== Alias Operations ====================

    override suspend fun insertAlias(alias: UUIDAliasDTO) {
        aliases[alias.alias] = alias
        val list = aliasesByUuid.getOrPut(alias.uuid) { mutableListOf() }
        list.add(alias)
    }

    override suspend fun deleteAliasByName(alias: String) {
        val dto = aliases.remove(alias)
        if (dto != null) {
            aliasesByUuid[dto.uuid]?.removeIf { it.alias == alias }
        }
    }

    override suspend fun deleteAliasesForUuid(uuid: String) {
        val list = aliasesByUuid.remove(uuid) ?: return
        list.forEach { aliases.remove(it.alias) }
    }

    override suspend fun getAliasByName(alias: String): UUIDAliasDTO? {
        return aliases[alias]
    }

    override suspend fun getAliasesForUuid(uuid: String): List<UUIDAliasDTO> {
        return aliasesByUuid[uuid] ?: emptyList()
    }

    override suspend fun getUuidByAlias(alias: String): String? {
        return aliases[alias]?.uuid
    }

    override suspend fun aliasExists(alias: String): Boolean {
        return aliases.containsKey(alias)
    }

    override suspend fun getAllAliases(): List<UUIDAliasDTO> {
        return aliases.values.toList()
    }

    override suspend fun insertAliasesBatch(aliases: List<UUIDAliasDTO>) {
        aliases.forEach { alias ->
            this.aliases[alias.alias] = alias
            val list = aliasesByUuid.getOrPut(alias.uuid) { mutableListOf() }
            list.add(alias)
        }
    }

    // ==================== Bulk Operations ====================

    override suspend fun deleteAllElements() {
        elements.clear()
    }

    override suspend fun deleteAllHierarchy() {
        hierarchies.clear()
    }

    override suspend fun deleteAllAnalytics() {
        analytics.clear()
    }

    override suspend fun deleteAllAliases() {
        aliases.clear()
        aliasesByUuid.clear()
    }

    /**
     * Clear all data
     */
    fun clearAll() {
        elements.clear()
        hierarchies.clear()
        analytics.clear()
        aliases.clear()
        aliasesByUuid.clear()
    }
}
