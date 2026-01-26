/**
 * SQLDelightAvidRepository.kt - SQLDelight implementation of AVID repository
 *
 * Provides database operations for AVID elements using SQLDelight.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.AvidElementDTO
import com.augmentalis.database.dto.AvidHierarchyDTO
import com.augmentalis.database.dto.AvidAnalyticsDTO
import com.augmentalis.database.dto.AvidAliasDTO
import com.augmentalis.database.repositories.IAvidRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IAvidRepository.
 */
class SQLDelightAvidRepository(
    private val database: VoiceOSDatabase
) : IAvidRepository {

    private val elementQueries = database.avidElementQueries
    private val hierarchyQueries = database.avidHierarchyQueries
    private val analyticsQueries = database.avidAnalyticsQueries
    private val aliasQueries = database.avidAliasQueries

    // ==================== Element Operations ====================

    override suspend fun insertElement(element: AvidElementDTO) = withContext(Dispatchers.Default) {
        elementQueries.insertElement(
            avid = element.avid,
            name = element.name,
            type = element.type,
            description = element.description,
            parent_avid = element.parentAvid,
            is_enabled = if (element.isEnabled) 1L else 0L,
            priority = element.priority.toLong(),
            timestamp = element.timestamp,
            metadata_json = element.metadataJson,
            position_json = element.positionJson
        )
    }

    override suspend fun updateElement(element: AvidElementDTO) = withContext(Dispatchers.Default) {
        elementQueries.updateElement(
            name = element.name,
            type = element.type,
            description = element.description,
            parent_avid = element.parentAvid,
            is_enabled = if (element.isEnabled) 1L else 0L,
            priority = element.priority.toLong(),
            metadata_json = element.metadataJson,
            position_json = element.positionJson,
            avid = element.avid
        )
    }

    override suspend fun deleteElement(avid: String) = withContext(Dispatchers.Default) {
        elementQueries.deleteElement(avid)
    }

    override suspend fun getElementByAvid(avid: String): AvidElementDTO? = withContext(Dispatchers.Default) {
        elementQueries.getElementByAvid(avid).executeAsOneOrNull()?.toAvidElementDTO()
    }

    override suspend fun getAllElements(): List<AvidElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.getAllElements().executeAsList().map { it.toAvidElementDTO() }
    }

    override suspend fun getElementsByType(type: String): List<AvidElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.getElementsByType(type).executeAsList().map { it.toAvidElementDTO() }
    }

    override suspend fun getChildrenOfParent(parentAvid: String): List<AvidElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.getChildrenOfParent(parentAvid).executeAsList().map { it.toAvidElementDTO() }
    }

    override suspend fun getEnabledElements(): List<AvidElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.getEnabledElements().executeAsList().map { it.toAvidElementDTO() }
    }

    override suspend fun searchByName(query: String): List<AvidElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.searchByName(query).executeAsList().map { it.toAvidElementDTO() }
    }

    override suspend fun countElements(): Long = withContext(Dispatchers.Default) {
        elementQueries.countElements().executeAsOne()
    }

    override suspend fun countElementsByType(type: String): Long = withContext(Dispatchers.Default) {
        elementQueries.countElementsByType(type).executeAsOne()
    }

    // ==================== Hierarchy Operations ====================

    override suspend fun insertHierarchy(hierarchy: AvidHierarchyDTO) = withContext(Dispatchers.Default) {
        hierarchyQueries.insertHierarchy(
            parent_avid = hierarchy.parentAvid,
            child_avid = hierarchy.childAvid,
            depth = hierarchy.depth.toLong(),
            path = hierarchy.path,
            order_index = hierarchy.orderIndex.toLong()
        )
    }

    override suspend fun deleteHierarchyByParent(parentAvid: String) = withContext(Dispatchers.Default) {
        hierarchyQueries.deleteByParent(parentAvid)
    }

    override suspend fun getHierarchyByParent(parentAvid: String): List<AvidHierarchyDTO> = withContext(Dispatchers.Default) {
        hierarchyQueries.getChildrenOfParent(parentAvid).executeAsList().map { it.toAvidHierarchyDTO() }
    }

    override suspend fun getAllHierarchy(): List<AvidHierarchyDTO> = withContext(Dispatchers.Default) {
        hierarchyQueries.getAllHierarchy().executeAsList().map { it.toAvidHierarchyDTO() }
    }

    // ==================== Analytics Operations ====================

    override suspend fun insertAnalytics(analytics: AvidAnalyticsDTO) = withContext(Dispatchers.Default) {
        analyticsQueries.insertAnalytics(
            avid = analytics.avid,
            access_count = analytics.accessCount,
            first_accessed = analytics.firstAccessed,
            last_accessed = analytics.lastAccessed,
            execution_time_ms = analytics.executionTimeMs,
            success_count = analytics.successCount,
            failure_count = analytics.failureCount,
            lifecycle_state = analytics.lifecycleState
        )
    }

    override suspend fun updateAnalytics(analytics: AvidAnalyticsDTO) = withContext(Dispatchers.Default) {
        analyticsQueries.insertAnalytics(
            avid = analytics.avid,
            access_count = analytics.accessCount,
            first_accessed = analytics.firstAccessed,
            last_accessed = analytics.lastAccessed,
            execution_time_ms = analytics.executionTimeMs,
            success_count = analytics.successCount,
            failure_count = analytics.failureCount,
            lifecycle_state = analytics.lifecycleState
        )
    }

    override suspend fun getAnalyticsByAvid(avid: String): AvidAnalyticsDTO? = withContext(Dispatchers.Default) {
        analyticsQueries.getAnalyticsByAvid(avid).executeAsOneOrNull()?.toAvidAnalyticsDTO()
    }

    override suspend fun getAllAnalytics(): List<AvidAnalyticsDTO> = withContext(Dispatchers.Default) {
        analyticsQueries.getAllAnalytics().executeAsList().map { it.toAvidAnalyticsDTO() }
    }

    override suspend fun getMostAccessed(limit: Int): List<AvidAnalyticsDTO> = withContext(Dispatchers.Default) {
        analyticsQueries.getMostAccessed(limit.toLong()).executeAsList().map { it.toAvidAnalyticsDTO() }
    }

    override suspend fun getRecentlyAccessed(limit: Int): List<AvidAnalyticsDTO> = withContext(Dispatchers.Default) {
        analyticsQueries.getRecentlyAccessed(limit.toLong()).executeAsList().map { it.toAvidAnalyticsDTO() }
    }

    override suspend fun incrementAccessCount(avid: String, timestamp: Long) = withContext(Dispatchers.Default) {
        analyticsQueries.incrementAccessCount(timestamp, avid)
    }

    override suspend fun recordExecution(
        avid: String,
        executionTimeMs: Long,
        success: Boolean,
        timestamp: Long
    ) = withContext(Dispatchers.Default) {
        analyticsQueries.recordExecution(
            execution_time_ms = executionTimeMs,
            success_count = if (success) 1L else 0L,
            failure_count = if (success) 0L else 1L,
            last_accessed = timestamp,
            avid = avid
        )
    }

    // ==================== Alias Operations ====================

    override suspend fun insertAlias(alias: AvidAliasDTO) = withContext(Dispatchers.Default) {
        aliasQueries.insertAlias(
            alias = alias.alias,
            avid = alias.avid,
            is_primary = if (alias.isPrimary) 1L else 0L,
            created_at = alias.createdAt
        )
    }

    override suspend fun deleteAliasByName(alias: String) = withContext(Dispatchers.Default) {
        aliasQueries.deleteAliasByName(alias)
    }

    override suspend fun deleteAliasesForAvid(avid: String) = withContext(Dispatchers.Default) {
        aliasQueries.deleteAliasesForAvid(avid)
    }

    override suspend fun getAliasByName(alias: String): AvidAliasDTO? = withContext(Dispatchers.Default) {
        aliasQueries.getAliasByName(alias).executeAsOneOrNull()?.toAvidAliasDTO()
    }

    override suspend fun getAliasesForAvid(avid: String): List<AvidAliasDTO> = withContext(Dispatchers.Default) {
        aliasQueries.getAliasesForAvid(avid).executeAsList().map { it.toAvidAliasDTO() }
    }

    override suspend fun getAvidByAlias(alias: String): String? = withContext(Dispatchers.Default) {
        aliasQueries.getAvidByAlias(alias).executeAsOneOrNull()
    }

    override suspend fun aliasExists(alias: String): Boolean = withContext(Dispatchers.Default) {
        aliasQueries.aliasExists(alias).executeAsOne() > 0
    }

    override suspend fun getAllAliases(): List<AvidAliasDTO> = withContext(Dispatchers.Default) {
        aliasQueries.getAllAliases().executeAsList().map { it.toAvidAliasDTO() }
    }

    override suspend fun insertAliasesBatch(aliases: List<AvidAliasDTO>) = withContext(Dispatchers.Default) {
        database.transaction {
            aliases.forEach { alias ->
                aliasQueries.insertAlias(
                    alias = alias.alias,
                    avid = alias.avid,
                    is_primary = if (alias.isPrimary) 1L else 0L,
                    created_at = alias.createdAt
                )
            }
        }
    }

    // ==================== Bulk Operations ====================

    override suspend fun deleteAllElements() = withContext(Dispatchers.Default) {
        database.transaction {
            hierarchyQueries.deleteAll()
            analyticsQueries.deleteAll()
            aliasQueries.deleteAll()
            elementQueries.deleteAll()
        }
    }

    override suspend fun deleteAllHierarchy() = withContext(Dispatchers.Default) {
        hierarchyQueries.deleteAll()
    }

    override suspend fun deleteAllAnalytics() = withContext(Dispatchers.Default) {
        analyticsQueries.deleteAll()
    }

    override suspend fun deleteAllAliases() = withContext(Dispatchers.Default) {
        aliasQueries.deleteAll()
    }

    suspend fun <T> transaction(body: () -> T): T = withContext(Dispatchers.Default) {
        database.transactionWithResult {
            body()
        }
    }
}

// ==================== Extension Functions ====================

private fun com.augmentalis.database.Avid_elements.toAvidElementDTO() = AvidElementDTO(
    avid = avid,
    name = name,
    type = type,
    description = description,
    parentAvid = parent_avid,
    isEnabled = is_enabled == 1L,
    priority = priority.toInt(),
    timestamp = timestamp,
    metadataJson = metadata_json,
    positionJson = position_json
)

private fun com.augmentalis.database.Avid_hierarchy.toAvidHierarchyDTO() = AvidHierarchyDTO(
    id = id,
    parentAvid = parent_avid,
    childAvid = child_avid,
    depth = depth.toInt(),
    path = path,
    orderIndex = order_index.toInt()
)

private fun com.augmentalis.database.Avid_analytics.toAvidAnalyticsDTO() = AvidAnalyticsDTO(
    avid = avid,
    accessCount = access_count,
    firstAccessed = first_accessed,
    lastAccessed = last_accessed,
    executionTimeMs = execution_time_ms,
    successCount = success_count,
    failureCount = failure_count,
    lifecycleState = lifecycle_state
)

private fun com.augmentalis.database.Avid_aliases.toAvidAliasDTO() = AvidAliasDTO(
    id = id,
    alias = alias,
    avid = avid,
    isPrimary = is_primary == 1L,
    createdAt = created_at
)
