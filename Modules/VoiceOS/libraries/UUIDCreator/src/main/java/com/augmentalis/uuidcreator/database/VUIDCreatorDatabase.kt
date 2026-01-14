/**
 * VUIDCreatorDatabase.kt - VUID database access (IVUIDRepository implementation)
 * Path: modules/libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/VUIDCreatorDatabase.kt
 *
 * Author: VoiceOS Restoration Team
 * Created: 2025-11-27
 * Modified: 2025-12-24 (UUID→VUID migration: updated all type references)
 *
 * Implementation of IVUIDRepository that provides in-memory storage
 * for VUID elements, hierarchies, analytics, and aliases.
 */

package com.augmentalis.uuidcreator.database

import android.content.Context
import com.augmentalis.database.dto.VUIDAliasDTO
import com.augmentalis.database.dto.VUIDAnalyticsDTO
import com.augmentalis.database.dto.VUIDElementDTO
import com.augmentalis.database.dto.VUIDHierarchyDTO
import com.augmentalis.database.repositories.IVUIDRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * VUID Creator Database
 *
 * Implements IVUIDRepository with in-memory storage for VUID management.
 * Provides alias management, element storage, hierarchy tracking, and analytics.
 *
 * **Features:**
 * - Implements IVUIDRepository interface
 * - In-memory storage for fast access
 * - Thread-safe via ConcurrentHashMap
 * - Compatible with UuidAliasManager
 *
 * **Usage:**
 * ```kotlin
 * val vuidDb = UUIDCreatorDatabase.getInstance(context)
 * val aliasManager = UuidAliasManager(vuidDb)
 * ```
 *
 * @property context Application context
 */
class UUIDCreatorDatabase private constructor(
    private val context: Context
) : IVUIDRepository {

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
    private val elements = ConcurrentHashMap<String, VUIDElementDTO>()
    private val hierarchies = ConcurrentHashMap<String, MutableList<VUIDHierarchyDTO>>()
    private val analytics = ConcurrentHashMap<String, VUIDAnalyticsDTO>()
    private val aliases = ConcurrentHashMap<String, VUIDAliasDTO>()
    private val aliasesByUuid = ConcurrentHashMap<String, MutableList<VUIDAliasDTO>>()

    // ==================== Element Operations ====================

    override suspend fun insertElement(element: VUIDElementDTO) {
        elements[element.uuid] = element
    }

    override suspend fun updateElement(element: VUIDElementDTO) {
        elements[element.uuid] = element
    }

    override suspend fun deleteElement(uuid: String) {
        elements.remove(uuid)
    }

    override suspend fun getElementByUuid(uuid: String): VUIDElementDTO? {
        return elements[uuid]
    }

    override suspend fun getAllElements(): List<VUIDElementDTO> {
        return elements.values.toList()
    }

    override suspend fun getElementsByType(type: String): List<VUIDElementDTO> {
        return elements.values.filter { it.type == type }
    }

    override suspend fun getChildrenOfParent(parentUuid: String): List<VUIDElementDTO> {
        return elements.values.filter { it.parentUuid == parentUuid }
    }

    override suspend fun getEnabledElements(): List<VUIDElementDTO> {
        return elements.values.filter { it.isEnabled }
    }

    override suspend fun searchByName(query: String): List<VUIDElementDTO> {
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

    override suspend fun insertHierarchy(hierarchy: VUIDHierarchyDTO) {
        val list = hierarchies.getOrPut(hierarchy.parentUuid) { mutableListOf() }
        list.add(hierarchy)
    }

    override suspend fun deleteHierarchyByParent(parentUuid: String) {
        hierarchies.remove(parentUuid)
    }

    override suspend fun getHierarchyByParent(parentUuid: String): List<VUIDHierarchyDTO> {
        return hierarchies[parentUuid] ?: emptyList()
    }

    override suspend fun getAllHierarchy(): List<VUIDHierarchyDTO> {
        return hierarchies.values.flatten()
    }

    // ==================== Analytics Operations ====================

    override suspend fun insertAnalytics(analytics: VUIDAnalyticsDTO) {
        this.analytics[analytics.uuid] = analytics
    }

    override suspend fun updateAnalytics(analytics: VUIDAnalyticsDTO) {
        this.analytics[analytics.uuid] = analytics
    }

    override suspend fun getAnalyticsByUuid(uuid: String): VUIDAnalyticsDTO? {
        return analytics[uuid]
    }

    override suspend fun getAllAnalytics(): List<VUIDAnalyticsDTO> {
        return analytics.values.toList()
    }

    override suspend fun getMostAccessed(limit: Int): List<VUIDAnalyticsDTO> {
        return analytics.values.sortedByDescending { it.accessCount }.take(limit)
    }

    override suspend fun getRecentlyAccessed(limit: Int): List<VUIDAnalyticsDTO> {
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
            analytics[uuid] = VUIDAnalyticsDTO(
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
            analytics[uuid] = VUIDAnalyticsDTO(
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

    override suspend fun insertAlias(alias: VUIDAliasDTO) {
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

    override suspend fun getAliasByName(alias: String): VUIDAliasDTO? {
        return aliases[alias]
    }

    override suspend fun getAliasesForUuid(uuid: String): List<VUIDAliasDTO> {
        return aliasesByUuid[uuid] ?: emptyList()
    }

    override suspend fun getUuidByAlias(alias: String): String? {
        return aliases[alias]?.uuid
    }

    override suspend fun aliasExists(alias: String): Boolean {
        return aliases.containsKey(alias)
    }

    override suspend fun getAllAliases(): List<VUIDAliasDTO> {
        return aliases.values.toList()
    }

    override suspend fun insertAliasesBatch(aliases: List<VUIDAliasDTO>) {
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
