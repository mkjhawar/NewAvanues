/**
 * UUIDElementDao.kt - Data Access Object for UUID elements
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/dao/UUIDElementDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * DAO for UUID element CRUD and query operations
 */

package com.augmentalis.uuidcreator.database.dao

import androidx.room.*
import com.augmentalis.uuidcreator.database.entities.UUIDElementEntity

/**
 * Data Access Object for UUID elements
 *
 * Provides CRUD operations and specialized queries for UUID element storage.
 */
@Dao
interface UUIDElementDao {

    // ==================== CREATE ====================

    /**
     * Insert a single UUID element
     * Replaces existing element with same UUID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(element: UUIDElementEntity)

    /**
     * Insert multiple UUID elements
     * Replaces existing elements with same UUIDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(elements: List<UUIDElementEntity>)

    // ==================== READ ====================

    /**
     * Get all UUID elements
     */
    @Query("SELECT * FROM uuid_elements")
    suspend fun getAll(): List<UUIDElementEntity>

    /**
     * Get element by UUID
     */
    @Query("SELECT * FROM uuid_elements WHERE uuid = :uuid")
    suspend fun getByUuid(uuid: String): UUIDElementEntity?

    /**
     * Get elements by name (case-insensitive)
     */
    @Query("SELECT * FROM uuid_elements WHERE LOWER(name) = LOWER(:name)")
    suspend fun getByName(name: String): List<UUIDElementEntity>

    /**
     * Get elements by type (case-insensitive)
     */
    @Query("SELECT * FROM uuid_elements WHERE LOWER(type) = LOWER(:type)")
    suspend fun getByType(type: String): List<UUIDElementEntity>

    /**
     * Get direct children of a parent UUID
     */
    @Query("SELECT * FROM uuid_elements WHERE parent_uuid = :parentUuid")
    suspend fun getChildren(parentUuid: String): List<UUIDElementEntity>

    /**
     * Get elements by enabled status
     */
    @Query("SELECT * FROM uuid_elements WHERE is_enabled = :enabled")
    suspend fun getByEnabled(enabled: Boolean): List<UUIDElementEntity>

    /**
     * Get elements by priority (descending order)
     */
    @Query("SELECT * FROM uuid_elements ORDER BY priority DESC")
    suspend fun getByPriorityDesc(): List<UUIDElementEntity>

    /**
     * Search elements by name pattern
     */
    @Query("SELECT * FROM uuid_elements WHERE name LIKE '%' || :pattern || '%'")
    suspend fun searchByName(pattern: String): List<UUIDElementEntity>

    /**
     * Search elements by description pattern
     */
    @Query("SELECT * FROM uuid_elements WHERE description LIKE '%' || :pattern || '%'")
    suspend fun searchByDescription(pattern: String): List<UUIDElementEntity>

    /**
     * Get most recent elements (by timestamp)
     */
    @Query("SELECT * FROM uuid_elements ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 10): List<UUIDElementEntity>

    /**
     * Get count of all elements
     */
    @Query("SELECT COUNT(*) FROM uuid_elements")
    suspend fun getCount(): Int

    /**
     * Get count by type
     */
    @Query("SELECT COUNT(*) FROM uuid_elements WHERE LOWER(type) = LOWER(:type)")
    suspend fun getCountByType(type: String): Int

    /**
     * Check if UUID exists
     */
    @Query("SELECT EXISTS(SELECT 1 FROM uuid_elements WHERE uuid = :uuid)")
    suspend fun exists(uuid: String): Boolean

    // ==================== UPDATE ====================

    /**
     * Update an existing element
     */
    @Update
    suspend fun update(element: UUIDElementEntity)

    /**
     * Update element enabled status
     */
    @Query("UPDATE uuid_elements SET is_enabled = :enabled WHERE uuid = :uuid")
    suspend fun updateEnabled(uuid: String, enabled: Boolean)

    /**
     * Update element priority
     */
    @Query("UPDATE uuid_elements SET priority = :priority WHERE uuid = :uuid")
    suspend fun updatePriority(uuid: String, priority: Int)

    // ==================== DELETE ====================

    /**
     * Delete an element
     */
    @Delete
    suspend fun delete(element: UUIDElementEntity)

    /**
     * Delete element by UUID
     */
    @Query("DELETE FROM uuid_elements WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    /**
     * Delete all elements of a specific type
     */
    @Query("DELETE FROM uuid_elements WHERE LOWER(type) = LOWER(:type)")
    suspend fun deleteByType(type: String)

    /**
     * Delete all disabled elements
     */
    @Query("DELETE FROM uuid_elements WHERE is_enabled = 0")
    suspend fun deleteDisabled()

    /**
     * Delete all elements
     */
    @Query("DELETE FROM uuid_elements")
    suspend fun deleteAll()
}
