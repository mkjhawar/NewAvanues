/**
 * IAvidRepository.kt - Interface for AVID element repository
 *
 * Defines the contract for AVID element storage operations.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.AvidElementDTO
import com.augmentalis.database.dto.AvidHierarchyDTO
import com.augmentalis.database.dto.AvidAnalyticsDTO
import com.augmentalis.database.dto.AvidAliasDTO

/**
 * Repository interface for AVID element operations.
 */
interface IAvidRepository {

    // ==================== Element Operations ====================

    suspend fun insertElement(element: AvidElementDTO)
    suspend fun updateElement(element: AvidElementDTO)
    suspend fun deleteElement(avid: String)
    suspend fun getElementByAvid(avid: String): AvidElementDTO?
    suspend fun getAllElements(): List<AvidElementDTO>
    suspend fun getElementsByType(type: String): List<AvidElementDTO>
    suspend fun getChildrenOfParent(parentAvid: String): List<AvidElementDTO>
    suspend fun getEnabledElements(): List<AvidElementDTO>
    suspend fun searchByName(query: String): List<AvidElementDTO>
    suspend fun countElements(): Long
    suspend fun countElementsByType(type: String): Long

    // ==================== Hierarchy Operations ====================

    suspend fun insertHierarchy(hierarchy: AvidHierarchyDTO)
    suspend fun deleteHierarchyByParent(parentAvid: String)
    suspend fun getHierarchyByParent(parentAvid: String): List<AvidHierarchyDTO>
    suspend fun getAllHierarchy(): List<AvidHierarchyDTO>

    // ==================== Analytics Operations ====================

    suspend fun insertAnalytics(analytics: AvidAnalyticsDTO)
    suspend fun updateAnalytics(analytics: AvidAnalyticsDTO)
    suspend fun getAnalyticsByAvid(avid: String): AvidAnalyticsDTO?
    suspend fun getAllAnalytics(): List<AvidAnalyticsDTO>
    suspend fun getMostAccessed(limit: Int): List<AvidAnalyticsDTO>
    suspend fun getRecentlyAccessed(limit: Int): List<AvidAnalyticsDTO>
    suspend fun incrementAccessCount(avid: String, timestamp: Long)
    suspend fun recordExecution(avid: String, executionTimeMs: Long, success: Boolean, timestamp: Long)

    // ==================== Alias Operations ====================

    suspend fun insertAlias(alias: AvidAliasDTO)
    suspend fun deleteAliasByName(alias: String)
    suspend fun deleteAliasesForAvid(avid: String)
    suspend fun getAliasByName(alias: String): AvidAliasDTO?
    suspend fun getAliasesForAvid(avid: String): List<AvidAliasDTO>
    suspend fun getAvidByAlias(alias: String): String?
    suspend fun aliasExists(alias: String): Boolean
    suspend fun getAllAliases(): List<AvidAliasDTO>

    /**
     * Batch insert aliases in single transaction (PERFORMANCE OPTIMIZATION)
     *
     * Replaces individual insertAlias() calls with single batch operation.
     *
     * **Performance:**
     * - Before: N database operations (1 per alias)
     * - After: 1 database operation (batch insert)
     * - 157x speedup for 63 elements (315 ops → 2 ops)
     *
     * **Use Case:**
     * LearnApp element registration - register 63 elements per screen in <100ms
     * instead of 1351ms with individual inserts.
     *
     * @param aliases List of alias DTOs to insert
     */
    suspend fun insertAliasesBatch(aliases: List<AvidAliasDTO>)

    // ==================== Bulk Operations ====================

    suspend fun deleteAllElements()
    suspend fun deleteAllHierarchy()
    suspend fun deleteAllAnalytics()
    suspend fun deleteAllAliases()
}
