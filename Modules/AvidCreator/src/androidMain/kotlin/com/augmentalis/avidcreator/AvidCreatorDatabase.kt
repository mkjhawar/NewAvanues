/**
 * AvidCreatorDatabase.kt - AVID database access (IAvidRepository implementation)
 * Path: libraries/AvidCreator/src/main/java/com/augmentalis/avidcreator/database/AvidCreatorDatabase.kt
 *
 * Author: VoiceOS Restoration Team
 * Created: 2025-11-27
 * Modified: 2025-12-24 (UUID→AVID migration: updated all type references)
 *
 * Implementation of IAvidRepository that provides in-memory storage
 * for AVID elements, hierarchies, analytics, and aliases.
 */

package com.augmentalis.avidcreator.database

import android.content.Context
import com.augmentalis.database.dto.AvidAliasDTO
import com.augmentalis.database.dto.AvidAnalyticsDTO
import com.augmentalis.database.dto.AvidElementDTO
import com.augmentalis.database.dto.AvidHierarchyDTO
import com.augmentalis.database.repositories.IAvidRepository
import java.util.concurrent.ConcurrentHashMap

/**
 * AVID Creator Database
 *
 * Implements IAvidRepository with in-memory storage for AVID management.
 * Provides alias management, element storage, hierarchy tracking, and analytics.
 *
 * **Features:**
 * - Implements IAvidRepository interface
 * - In-memory storage for fast access
 * - Thread-safe via ConcurrentHashMap
 * - Compatible with AvidAliasManager
 *
 * **Usage:**
 * ```kotlin
 * val avidDb = AvidCreatorDatabase.getInstance(context)
 * val aliasManager = AvidAliasManager(avidDb)
 * ```
 *
 * @property context Application context
 */
class AvidCreatorDatabase private constructor(
    private val context: Context
) : IAvidRepository {

    companion object {
        @Volatile
        private var INSTANCE: AvidCreatorDatabase? = null

        /**
         * Get singleton instance
         *
         * @param context Application context
         * @return Database instance
         */
        fun getInstance(context: Context): AvidCreatorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AvidCreatorDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    // In-memory storage
    private val elements = ConcurrentHashMap<String, AvidElementDTO>()
    private val hierarchies = ConcurrentHashMap<String, MutableList<AvidHierarchyDTO>>()
    private val analytics = ConcurrentHashMap<String, AvidAnalyticsDTO>()
    private val aliases = ConcurrentHashMap<String, AvidAliasDTO>()
    private val aliasesByAvid = ConcurrentHashMap<String, MutableList<AvidAliasDTO>>()

    // ==================== Element Operations ====================

    override suspend fun insertElement(element: AvidElementDTO) {
        elements[element.avid] = element
    }

    override suspend fun updateElement(element: AvidElementDTO) {
        elements[element.avid] = element
    }

    override suspend fun deleteElement(uuid: String) {
        elements.remove(uuid)
    }

    override suspend fun getElementByAvid(uuid: String): AvidElementDTO? {
        return elements[uuid]
    }

    override suspend fun getAllElements(): List<AvidElementDTO> {
        return elements.values.toList()
    }

    override suspend fun getElementsByType(type: String): List<AvidElementDTO> {
        return elements.values.filter { it.type == type }
    }

    override suspend fun getChildrenOfParent(parentAvid: String): List<AvidElementDTO> {
        return elements.values.filter { it.parentAvid == parentAvid }
    }

    override suspend fun getEnabledElements(): List<AvidElementDTO> {
        return elements.values.filter { it.isEnabled }
    }

    override suspend fun searchByName(query: String): List<AvidElementDTO> {
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

    override suspend fun insertHierarchy(hierarchy: AvidHierarchyDTO) {
        val list = hierarchies.getOrPut(hierarchy.parentAvid) { mutableListOf() }
        list.add(hierarchy)
    }

    override suspend fun deleteHierarchyByParent(parentAvid: String) {
        hierarchies.remove(parentAvid)
    }

    override suspend fun getHierarchyByParent(parentAvid: String): List<AvidHierarchyDTO> {
        return hierarchies[parentAvid] ?: emptyList()
    }

    override suspend fun getAllHierarchy(): List<AvidHierarchyDTO> {
        return hierarchies.values.flatten()
    }

    // ==================== Analytics Operations ====================

    override suspend fun insertAnalytics(analytics: AvidAnalyticsDTO) {
        this.analytics[analytics.avid] = analytics
    }

    override suspend fun updateAnalytics(analytics: AvidAnalyticsDTO) {
        this.analytics[analytics.avid] = analytics
    }

    override suspend fun getAnalyticsByAvid(uuid: String): AvidAnalyticsDTO? {
        return analytics[uuid]
    }

    override suspend fun getAllAnalytics(): List<AvidAnalyticsDTO> {
        return analytics.values.toList()
    }

    override suspend fun getMostAccessed(limit: Int): List<AvidAnalyticsDTO> {
        return analytics.values.sortedByDescending { it.accessCount }.take(limit)
    }

    override suspend fun getRecentlyAccessed(limit: Int): List<AvidAnalyticsDTO> {
        return analytics.values.sortedByDescending { it.lastAccessed }.take(limit)
    }

    override suspend fun incrementAccessCount(avid: String, timestamp: Long) {
        val existing = analytics[avid]
        if (existing != null) {
            analytics[avid] = existing.copy(
                accessCount = existing.accessCount + 1,
                lastAccessed = timestamp
            )
        } else {
            analytics[avid] = AvidAnalyticsDTO(
                avid = avid,
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

    override suspend fun recordExecution(avid: String, executionTimeMs: Long, success: Boolean, timestamp: Long) {
        val existing = analytics[avid]
        if (existing != null) {
            val newSuccessCount = if (success) existing.successCount + 1 else existing.successCount
            val newFailureCount = if (!success) existing.failureCount + 1 else existing.failureCount

            analytics[avid] = existing.copy(
                successCount = newSuccessCount,
                failureCount = newFailureCount,
                executionTimeMs = executionTimeMs,
                lastAccessed = timestamp
            )
        } else {
            analytics[avid] = AvidAnalyticsDTO(
                avid = avid,
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

    override suspend fun insertAlias(alias: AvidAliasDTO) {
        aliases[alias.alias] = alias
        val list = aliasesByAvid.getOrPut(alias.avid) { mutableListOf() }
        list.add(alias)
    }

    override suspend fun deleteAliasByName(alias: String) {
        val dto = aliases.remove(alias)
        if (dto != null) {
            aliasesByAvid[dto.avid]?.removeIf { it.alias == alias }
        }
    }

    override suspend fun deleteAliasesForAvid(avid: String) {
        val list = aliasesByAvid.remove(avid) ?: return
        list.forEach { aliases.remove(it.alias) }
    }

    override suspend fun getAliasByName(alias: String): AvidAliasDTO? {
        return aliases[alias]
    }

    override suspend fun getAliasesForAvid(avid: String): List<AvidAliasDTO> {
        return aliasesByAvid[avid] ?: emptyList()
    }

    override suspend fun getAvidByAlias(alias: String): String? {
        return aliases[alias]?.avid
    }

    override suspend fun aliasExists(alias: String): Boolean {
        return aliases.containsKey(alias)
    }

    override suspend fun getAllAliases(): List<AvidAliasDTO> {
        return aliases.values.toList()
    }

    override suspend fun insertAliasesBatch(aliases: List<AvidAliasDTO>) {
        aliases.forEach { alias ->
            this.aliases[alias.alias] = alias
            val list = aliasesByAvid.getOrPut(alias.avid) { mutableListOf() }
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
        aliasesByAvid.clear()
    }

    /**
     * Clear all data
     */
    fun clearAll() {
        elements.clear()
        hierarchies.clear()
        analytics.clear()
        aliases.clear()
        aliasesByAvid.clear()
    }
}
