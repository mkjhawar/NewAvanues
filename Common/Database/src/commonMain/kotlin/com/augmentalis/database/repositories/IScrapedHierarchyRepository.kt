/**
 * IScrapedHierarchyRepository.kt - Repository interface for element hierarchies
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Database Migrator (Agent 2)
 * Created: 2025-11-26
 */

package com.avanues.database.repositories

import com.avanues.database.dto.ScrapedHierarchyDTO

/**
 * Repository interface for scraped element hierarchies.
 * Provides CRUD operations for parent-child relationships.
 */
interface IScrapedHierarchyRepository {

    /**
     * Insert a hierarchy relationship.
     */
    suspend fun insert(
        parentElementHash: String,
        childElementHash: String,
        depth: Long,
        createdAt: Long
    )

    /**
     * Get children of a parent element.
     */
    suspend fun getByParent(parentElementHash: String): List<ScrapedHierarchyDTO>

    /**
     * Get parent of a child element.
     */
    suspend fun getByChild(childElementHash: String): List<ScrapedHierarchyDTO>

    /**
     * Get hierarchies at a specific depth.
     */
    suspend fun getByDepth(depth: Long): List<ScrapedHierarchyDTO>

    /**
     * Delete all hierarchies for a parent element.
     */
    suspend fun deleteByParent(parentElementHash: String)

    /**
     * Delete all hierarchies for a child element.
     */
    suspend fun deleteByChild(childElementHash: String)

    /**
     * Delete a specific relationship.
     */
    suspend fun deleteRelationship(parentElementHash: String, childElementHash: String)

    /**
     * Delete all hierarchies.
     */
    suspend fun deleteAll()

    /**
     * Count all hierarchies.
     */
    suspend fun count(): Long
}
