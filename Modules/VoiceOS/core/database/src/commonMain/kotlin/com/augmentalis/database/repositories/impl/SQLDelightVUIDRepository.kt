/**
 * SQLDelightVUIDRepository.kt - SQLDelight implementation of VUID repository
 *
 * Provides database operations for VUID elements using SQLDelight.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.VUIDElementDTO
import com.augmentalis.database.dto.VUIDHierarchyDTO
import com.augmentalis.database.dto.VUIDAnalyticsDTO
import com.augmentalis.database.dto.VUIDAliasDTO
import com.augmentalis.database.repositories.IVUIDRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IVUIDRepository.
 */
class SQLDelightVUIDRepository(
    private val database: VoiceOSDatabase
) : IVUIDRepository {

    private val elementQueries = database.vUIDElementQueries
    private val hierarchyQueries = database.vUIDHierarchyQueries
    private val analyticsQueries = database.vUIDAnalyticsQueries
    private val aliasQueries = database.vUIDAliasQueries

    // ==================== Element Operations ====================

    override suspend fun insertElement(element: VUIDElementDTO) = withContext(Dispatchers.Default) {
        elementQueries.insertElement(
            vuid = element.vuid,
            name = element.name,
            type = element.type,
            description = element.description,
            parent_vuid = element.parentVuid,
            is_enabled = if (element.isEnabled) 1L else 0L,
            priority = element.priority.toLong(),
            timestamp = element.timestamp,
            metadata_json = element.metadataJson,
            position_json = element.positionJson
        )
    }

    override suspend fun updateElement(element: VUIDElementDTO) = withContext(Dispatchers.Default) {
        elementQueries.updateElement(
            name = element.name,
            type = element.type,
            description = element.description,
            parent_vuid = element.parentVuid,
            is_enabled = if (element.isEnabled) 1L else 0L,
            priority = element.priority.toLong(),
            metadata_json = element.metadataJson,
            position_json = element.positionJson,
            vuid = element.vuid
        )
    }

    override suspend fun deleteElement(uuid: String) = withContext(Dispatchers.Default) {
        elementQueries.deleteElement(uuid)
    }

    override suspend fun getElementByUuid(uuid: String): VUIDElementDTO? = withContext(Dispatchers.Default) {
        elementQueries.getElementByVuid(uuid).executeAsOneOrNull()?.toVUIDElementDTO()
    }

    override suspend fun getAllElements(): List<VUIDElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.getAllElements().executeAsList().map { it.toVUIDElementDTO() }
    }

    override suspend fun getElementsByType(type: String): List<VUIDElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.getElementsByType(type).executeAsList().map { it.toVUIDElementDTO() }
    }

    override suspend fun getChildrenOfParent(parentUuid: String): List<VUIDElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.getChildrenOfParent(parentUuid).executeAsList().map { it.toVUIDElementDTO() }
    }

    override suspend fun getEnabledElements(): List<VUIDElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.getEnabledElements().executeAsList().map { it.toVUIDElementDTO() }
    }

    override suspend fun searchByName(query: String): List<VUIDElementDTO> = withContext(Dispatchers.Default) {
        elementQueries.searchByName(query).executeAsList().map { it.toVUIDElementDTO() }
    }

    override suspend fun countElements(): Long = withContext(Dispatchers.Default) {
        elementQueries.countElements().executeAsOne()
    }

    override suspend fun countElementsByType(type: String): Long = withContext(Dispatchers.Default) {
        elementQueries.countElementsByType(type).executeAsOne()
    }

    // ==================== Hierarchy Operations ====================

    override suspend fun insertHierarchy(hierarchy: VUIDHierarchyDTO) = withContext(Dispatchers.Default) {
        hierarchyQueries.insertHierarchy(
            parent_vuid = hierarchy.parentVuid,
            child_vuid = hierarchy.childVuid,
            depth = hierarchy.depth.toLong(),
            path = hierarchy.path,
            order_index = hierarchy.orderIndex.toLong()
        )
    }

    override suspend fun deleteHierarchyByParent(parentUuid: String) = withContext(Dispatchers.Default) {
        hierarchyQueries.deleteByParent(parentUuid)
    }

    override suspend fun getHierarchyByParent(parentUuid: String): List<VUIDHierarchyDTO> = withContext(Dispatchers.Default) {
        hierarchyQueries.getChildrenOfParent(parentUuid).executeAsList().map { it.toVUIDHierarchyDTO() }
    }

    override suspend fun getAllHierarchy(): List<VUIDHierarchyDTO> = withContext(Dispatchers.Default) {
        hierarchyQueries.getAllHierarchy().executeAsList().map { it.toVUIDHierarchyDTO() }
    }

    // ==================== Analytics Operations ====================

    override suspend fun insertAnalytics(analytics: VUIDAnalyticsDTO) = withContext(Dispatchers.Default) {
        analyticsQueries.insertAnalytics(
            vuid = analytics.vuid,
            access_count = analytics.accessCount,
            first_accessed = analytics.firstAccessed,
            last_accessed = analytics.lastAccessed,
            execution_time_ms = analytics.executionTimeMs,
            success_count = analytics.successCount,
            failure_count = analytics.failureCount,
            lifecycle_state = analytics.lifecycleState
        )
    }

    override suspend fun updateAnalytics(analytics: VUIDAnalyticsDTO) = withContext(Dispatchers.Default) {
        analyticsQueries.insertAnalytics(
            vuid = analytics.vuid,
            access_count = analytics.accessCount,
            first_accessed = analytics.firstAccessed,
            last_accessed = analytics.lastAccessed,
            execution_time_ms = analytics.executionTimeMs,
            success_count = analytics.successCount,
            failure_count = analytics.failureCount,
            lifecycle_state = analytics.lifecycleState
        )
    }

    override suspend fun getAnalyticsByUuid(uuid: String): VUIDAnalyticsDTO? = withContext(Dispatchers.Default) {
        analyticsQueries.getAnalyticsByVuid(uuid).executeAsOneOrNull()?.toVUIDAnalyticsDTO()
    }

    override suspend fun getAllAnalytics(): List<VUIDAnalyticsDTO> = withContext(Dispatchers.Default) {
        analyticsQueries.getAllAnalytics().executeAsList().map { it.toVUIDAnalyticsDTO() }
    }

    override suspend fun getMostAccessed(limit: Int): List<VUIDAnalyticsDTO> = withContext(Dispatchers.Default) {
        analyticsQueries.getMostAccessed(limit.toLong()).executeAsList().map { it.toVUIDAnalyticsDTO() }
    }

    override suspend fun getRecentlyAccessed(limit: Int): List<VUIDAnalyticsDTO> = withContext(Dispatchers.Default) {
        analyticsQueries.getRecentlyAccessed(limit.toLong()).executeAsList().map { it.toVUIDAnalyticsDTO() }
    }

    override suspend fun incrementAccessCount(uuid: String, timestamp: Long) = withContext(Dispatchers.Default) {
        analyticsQueries.incrementAccessCount(timestamp, uuid)
    }

    override suspend fun recordExecution(
        uuid: String,
        executionTimeMs: Long,
        success: Boolean,
        timestamp: Long
    ) = withContext(Dispatchers.Default) {
        analyticsQueries.recordExecution(
            execution_time_ms = executionTimeMs,
            success_count = if (success) 1L else 0L,
            failure_count = if (success) 0L else 1L,
            last_accessed = timestamp,
            vuid = uuid
        )
    }

    // ==================== Alias Operations ====================

    override suspend fun insertAlias(alias: VUIDAliasDTO) = withContext(Dispatchers.Default) {
        aliasQueries.insertAlias(
            alias = alias.alias,
            vuid = alias.vuid,
            is_primary = if (alias.isPrimary) 1L else 0L,
            created_at = alias.createdAt
        )
    }

    override suspend fun deleteAliasByName(alias: String) = withContext(Dispatchers.Default) {
        aliasQueries.deleteAliasByName(alias)
    }

    override suspend fun deleteAliasesForUuid(uuid: String) = withContext(Dispatchers.Default) {
        aliasQueries.deleteAliasesForVuid(uuid)
    }

    override suspend fun getAliasByName(alias: String): VUIDAliasDTO? = withContext(Dispatchers.Default) {
        aliasQueries.getAliasByName(alias).executeAsOneOrNull()?.toVUIDAliasDTO()
    }

    override suspend fun getAliasesForUuid(uuid: String): List<VUIDAliasDTO> = withContext(Dispatchers.Default) {
        aliasQueries.getAliasesForVuid(uuid).executeAsList().map { it.toVUIDAliasDTO() }
    }

    override suspend fun getUuidByAlias(alias: String): String? = withContext(Dispatchers.Default) {
        aliasQueries.getVuidByAlias(alias).executeAsOneOrNull()
    }

    override suspend fun aliasExists(alias: String): Boolean = withContext(Dispatchers.Default) {
        aliasQueries.aliasExists(alias).executeAsOne() > 0
    }

    override suspend fun getAllAliases(): List<VUIDAliasDTO> = withContext(Dispatchers.Default) {
        aliasQueries.getAllAliases().executeAsList().map { it.toVUIDAliasDTO() }
    }

    override suspend fun insertAliasesBatch(aliases: List<VUIDAliasDTO>) = withContext(Dispatchers.Default) {
        database.transaction {
            aliases.forEach { alias ->
                aliasQueries.insertAlias(
                    alias = alias.alias,
                    vuid = alias.vuid,
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

private fun com.augmentalis.database.vuid.Vuid_elements.toVUIDElementDTO() = VUIDElementDTO(
    vuid = vuid,
    name = name,
    type = type,
    description = description,
    parentVuid = parent_vuid,
    isEnabled = is_enabled == 1L,
    priority = priority.toInt(),
    timestamp = timestamp,
    metadataJson = metadata_json,
    positionJson = position_json
)

private fun com.augmentalis.database.vuid.Vuid_hierarchy.toVUIDHierarchyDTO() = VUIDHierarchyDTO(
    id = id,
    parentVuid = parent_vuid,
    childVuid = child_vuid,
    depth = depth.toInt(),
    path = path,
    orderIndex = order_index.toInt()
)

private fun com.augmentalis.database.vuid.Vuid_analytics.toVUIDAnalyticsDTO() = VUIDAnalyticsDTO(
    vuid = vuid,
    accessCount = access_count,
    firstAccessed = first_accessed,
    lastAccessed = last_accessed,
    executionTimeMs = execution_time_ms,
    successCount = success_count,
    failureCount = failure_count,
    lifecycleState = lifecycle_state
)

private fun com.augmentalis.database.vuid.Vuid_aliases.toVUIDAliasDTO() = VUIDAliasDTO(
    id = id,
    alias = alias,
    vuid = vuid,
    isPrimary = is_primary == 1L,
    createdAt = created_at
)
