/**
 * UUIDHierarchyDao.kt - Data Access Object for UUID hierarchy relationships
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/dao/UUIDHierarchyDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * DAO for UUID hierarchy CRUD and traversal operations
 */

package com.augmentalis.uuidcreator.database.dao

import androidx.room.*
import com.augmentalis.uuidcreator.database.entities.UUIDHierarchyEntity

/**
 * Data Access Object for UUID hierarchy relationships
 *
 * Provides operations for managing and querying parent-child relationships.
 */
@Dao
interface UUIDHierarchyDao {

    // ==================== CREATE ====================

    /**
     * Insert a single hierarchy relationship
     * Replaces existing relationship with same ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(hierarchy: UUIDHierarchyEntity)

    /**
     * Insert multiple hierarchy relationships
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(hierarchies: List<UUIDHierarchyEntity>)

    // ==================== READ ====================

    /**
     * Get all hierarchy relationships
     */
    @Query("SELECT * FROM uuid_hierarchy")
    suspend fun getAll(): List<UUIDHierarchyEntity>

    /**
     * Get direct children of a parent (ordered by order_index)
     */
    @Query("SELECT * FROM uuid_hierarchy WHERE parent_uuid = :parentUuid ORDER BY order_index ASC")
    suspend fun getChildren(parentUuid: String): List<UUIDHierarchyEntity>

    /**
     * Get parent relationship for a child
     */
    @Query("SELECT * FROM uuid_hierarchy WHERE child_uuid = :childUuid LIMIT 1")
    suspend fun getParent(childUuid: String): UUIDHierarchyEntity?

    /**
     * Get all descendants (children, grandchildren, etc.) by path prefix
     */
    @Query("SELECT * FROM uuid_hierarchy WHERE path LIKE :pathPrefix || '%'")
    suspend fun getDescendants(pathPrefix: String): List<UUIDHierarchyEntity>

    /**
     * Get all ancestors (parents, grandparents, etc.) by checking if path contains child UUID
     */
    @Query("SELECT * FROM uuid_hierarchy WHERE path LIKE '%/' || :childUuid || '%'")
    suspend fun getAncestors(childUuid: String): List<UUIDHierarchyEntity>

    /**
     * Get relationships at specific depth
     */
    @Query("SELECT * FROM uuid_hierarchy WHERE depth = :depth")
    suspend fun getByDepth(depth: Int): List<UUIDHierarchyEntity>

    /**
     * Get maximum depth in hierarchy
     */
    @Query("SELECT MAX(depth) FROM uuid_hierarchy")
    suspend fun getMaxDepth(): Int?

    /**
     * Get sibling count for a child
     */
    @Query("SELECT COUNT(*) FROM uuid_hierarchy WHERE parent_uuid = (SELECT parent_uuid FROM uuid_hierarchy WHERE child_uuid = :childUuid LIMIT 1)")
    suspend fun getSiblingCount(childUuid: String): Int

    /**
     * Get child count for a parent
     */
    @Query("SELECT COUNT(*) FROM uuid_hierarchy WHERE parent_uuid = :parentUuid")
    suspend fun getChildCount(parentUuid: String): Int

    /**
     * Check if a UUID has children
     */
    @Query("SELECT EXISTS(SELECT 1 FROM uuid_hierarchy WHERE parent_uuid = :uuid)")
    suspend fun hasChildren(uuid: String): Boolean

    /**
     * Check if a UUID has a parent
     */
    @Query("SELECT EXISTS(SELECT 1 FROM uuid_hierarchy WHERE child_uuid = :uuid)")
    suspend fun hasParent(uuid: String): Boolean

    /**
     * Get depth of a specific child
     */
    @Query("SELECT depth FROM uuid_hierarchy WHERE child_uuid = :childUuid LIMIT 1")
    suspend fun getDepth(childUuid: String): Int?

    // ==================== UPDATE ====================

    /**
     * Update a hierarchy relationship
     */
    @Update
    suspend fun update(hierarchy: UUIDHierarchyEntity)

    /**
     * Update order index for a child
     */
    @Query("UPDATE uuid_hierarchy SET order_index = :orderIndex WHERE parent_uuid = :parentUuid AND child_uuid = :childUuid")
    suspend fun updateOrderIndex(parentUuid: String, childUuid: String, orderIndex: Int)

    /**
     * Update depth for a child
     */
    @Query("UPDATE uuid_hierarchy SET depth = :depth WHERE child_uuid = :childUuid")
    suspend fun updateDepth(childUuid: String, depth: Int)

    // ==================== DELETE ====================

    /**
     * Delete a hierarchy relationship
     */
    @Delete
    suspend fun delete(hierarchy: UUIDHierarchyEntity)

    /**
     * Delete all children of a parent (CASCADE handles element deletion)
     */
    @Query("DELETE FROM uuid_hierarchy WHERE parent_uuid = :parentUuid")
    suspend fun deleteByParent(parentUuid: String)

    /**
     * Delete parent relationship for a child
     */
    @Query("DELETE FROM uuid_hierarchy WHERE child_uuid = :childUuid")
    suspend fun deleteByChild(childUuid: String)

    /**
     * Delete all hierarchy relationships
     */
    @Query("DELETE FROM uuid_hierarchy")
    suspend fun deleteAll()
}
