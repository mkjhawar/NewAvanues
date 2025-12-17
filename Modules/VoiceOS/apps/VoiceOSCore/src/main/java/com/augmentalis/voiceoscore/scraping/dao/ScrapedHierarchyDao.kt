/**
 * ScrapedHierarchyDao.kt - Data Access Object for element hierarchy relationships
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.scraping.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.augmentalis.voiceoscore.scraping.entities.ScrapedHierarchyEntity

/**
 * DAO for ScrapedHierarchyEntity operations
 *
 * Provides database access for hierarchical relationships between UI elements.
 * Enables tree traversal and contextual command understanding.
 */
@Dao
interface ScrapedHierarchyDao {

    /**
     * Insert a new hierarchy relationship
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hierarchy: ScrapedHierarchyEntity): Long

    /**
     * Insert multiple relationships in batch (optimized for tree scraping)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(hierarchies: List<ScrapedHierarchyEntity>)

    /**
     * Get all children of a parent element
     * Returns children ordered by child_order (preserves layout order)
     */
    @Query("SELECT * FROM scraped_hierarchy WHERE parent_element_id = :parentId ORDER BY child_order")
    suspend fun getChildren(parentId: Long): List<ScrapedHierarchyEntity>

    /**
     * Get parent of a child element
     */
    @Query("SELECT * FROM scraped_hierarchy WHERE child_element_id = :childId")
    suspend fun getParent(childId: Long): ScrapedHierarchyEntity?

    /**
     * Get all siblings of an element (same parent)
     */
    @Query("""
        SELECT sh2.* FROM scraped_hierarchy sh1
        JOIN scraped_hierarchy sh2 ON sh1.parent_element_id = sh2.parent_element_id
        WHERE sh1.child_element_id = :elementId
        AND sh2.child_element_id != :elementId
        ORDER BY sh2.child_order
    """)
    suspend fun getSiblings(elementId: Long): List<ScrapedHierarchyEntity>

    /**
     * Get child count for a parent element
     */
    @Query("SELECT COUNT(*) FROM scraped_hierarchy WHERE parent_element_id = :parentId")
    suspend fun getChildCount(parentId: Long): Int

    /**
     * Check if element has children
     */
    @Query("SELECT EXISTS(SELECT 1 FROM scraped_hierarchy WHERE parent_element_id = :elementId)")
    suspend fun hasChildren(elementId: Long): Boolean

    /**
     * Check if element has parent
     */
    @Query("SELECT EXISTS(SELECT 1 FROM scraped_hierarchy WHERE child_element_id = :elementId)")
    suspend fun hasParent(elementId: Long): Boolean

    /**
     * Get all root elements (elements with no parent)
     * Uses LEFT JOIN to find elements not in the child_element_id column
     */
    @Query("""
        SELECT se.id FROM scraped_elements se
        LEFT JOIN scraped_hierarchy sh ON se.id = sh.child_element_id
        WHERE sh.child_element_id IS NULL AND se.app_id = :appId
    """)
    suspend fun getRootElements(appId: String): List<Long>

    /**
     * Get all leaf elements (elements with no children)
     * Uses LEFT JOIN to find elements not in the parent_element_id column
     */
    @Query("""
        SELECT se.id FROM scraped_elements se
        LEFT JOIN scraped_hierarchy sh ON se.id = sh.parent_element_id
        WHERE sh.parent_element_id IS NULL AND se.app_id = :appId
    """)
    suspend fun getLeafElements(appId: String): List<Long>

    /**
     * Delete all hierarchy relationships for elements of an app
     * Uses subquery to find elements belonging to the app
     */
    @Query("""
        DELETE FROM scraped_hierarchy
        WHERE parent_element_id IN (SELECT id FROM scraped_elements WHERE app_id = :appId)
        OR child_element_id IN (SELECT id FROM scraped_elements WHERE app_id = :appId)
    """)
    suspend fun deleteHierarchyForApp(appId: String)

    /**
     * Delete specific hierarchy relationship
     */
    @Query("DELETE FROM scraped_hierarchy WHERE parent_element_id = :parentId AND child_element_id = :childId")
    suspend fun deleteRelationship(parentId: Long, childId: Long)

    /**
     * Get total hierarchy relationship count
     */
    @Query("SELECT COUNT(*) FROM scraped_hierarchy")
    suspend fun getRelationshipCount(): Int
}
