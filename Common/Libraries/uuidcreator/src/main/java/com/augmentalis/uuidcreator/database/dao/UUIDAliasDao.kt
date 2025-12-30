/**
 * UUIDAliasDao.kt - Room DAO for UUID aliases
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/database/dao/UUIDAliasDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Data access object for UUID alias operations
 */

package com.augmentalis.uuidcreator.database.dao

import androidx.room.*
import com.augmentalis.uuidcreator.database.entities.UUIDAliasEntity

/**
 * UUID Alias DAO
 *
 * Room DAO for CRUD operations on UUID aliases.
 *
 * ## Query Performance
 *
 * - Get by alias: O(1) via unique index
 * - Get by UUID: O(1) via index
 * - Insert: O(1)
 * - Delete: O(1)
 *
 * ## Usage Example
 *
 * ```kotlin
 * @Database(
 *     entities = [UUIDAliasEntity::class],
 *     version = 1
 * )
 * abstract class AppDatabase : RoomDatabase() {
 *     abstract fun aliasDao(): UUIDAliasDao
 * }
 *
 * val dao = database.aliasDao()
 * dao.insert(alias)
 * val uuid = dao.getUuidByAlias("instagram_like_btn")
 * ```
 *
 * @since 1.0.0
 */
@Dao
interface UUIDAliasDao {

    /**
     * Insert alias
     *
     * @param alias Alias entity to insert
     * @throws SQLiteConstraintException if alias already exists
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(alias: UUIDAliasEntity)

    /**
     * Insert multiple aliases
     *
     * @param aliases List of alias entities
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(aliases: List<UUIDAliasEntity>)

    /**
     * Update alias
     *
     * @param alias Updated alias entity
     */
    @Update
    suspend fun update(alias: UUIDAliasEntity)

    /**
     * Delete alias
     *
     * @param alias Alias entity to delete
     */
    @Delete
    suspend fun delete(alias: UUIDAliasEntity)

    /**
     * Delete alias by alias string
     *
     * @param alias Alias string to delete
     */
    @Query("DELETE FROM uuid_aliases WHERE alias = :alias")
    suspend fun deleteByAlias(alias: String)

    /**
     * Delete all aliases for UUID
     *
     * @param uuid UUID to delete aliases for
     */
    @Query("DELETE FROM uuid_aliases WHERE uuid = :uuid")
    suspend fun deleteByUuid(uuid: String)

    /**
     * Delete all aliases
     */
    @Query("DELETE FROM uuid_aliases")
    suspend fun deleteAll()

    /**
     * Get UUID by alias
     *
     * Fast O(1) lookup via unique index.
     *
     * @param alias Alias string to look up
     * @return UUID or null if alias not found
     */
    @Query("SELECT uuid FROM uuid_aliases WHERE alias = :alias")
    suspend fun getUuidByAlias(alias: String): String?

    /**
     * Get alias entity by alias string
     *
     * @param alias Alias string to look up
     * @return Alias entity or null
     */
    @Query("SELECT * FROM uuid_aliases WHERE alias = :alias")
    suspend fun getByAlias(alias: String): UUIDAliasEntity?

    /**
     * Get all aliases for UUID
     *
     * @param uuid UUID to get aliases for
     * @return List of alias entities
     */
    @Query("SELECT * FROM uuid_aliases WHERE uuid = :uuid ORDER BY is_primary DESC, created_at ASC")
    suspend fun getAliasesByUuid(uuid: String): List<UUIDAliasEntity>

    /**
     * Get primary alias for UUID
     *
     * @param uuid UUID to get primary alias for
     * @return Primary alias entity or null
     */
    @Query("SELECT * FROM uuid_aliases WHERE uuid = :uuid AND is_primary = 1 LIMIT 1")
    suspend fun getPrimaryAlias(uuid: String): UUIDAliasEntity?

    /**
     * Get all aliases
     *
     * @return List of all alias entities
     */
    @Query("SELECT * FROM uuid_aliases ORDER BY created_at DESC")
    suspend fun getAll(): List<UUIDAliasEntity>

    /**
     * Get alias count
     *
     * @return Total number of aliases
     */
    @Query("SELECT COUNT(*) FROM uuid_aliases")
    suspend fun getCount(): Int

    /**
     * Check if alias exists
     *
     * @param alias Alias string to check
     * @return true if exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM uuid_aliases WHERE alias = :alias)")
    suspend fun exists(alias: String): Boolean

    /**
     * Get recently created aliases
     *
     * @param limit Maximum number of results
     * @return List of recently created aliases
     */
    @Query("SELECT * FROM uuid_aliases ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentlyCreated(limit: Int = 20): List<UUIDAliasEntity>

    /**
     * Search aliases by pattern
     *
     * @param pattern Search pattern (SQL LIKE format)
     * @return List of matching aliases
     */
    @Query("SELECT * FROM uuid_aliases WHERE alias LIKE :pattern ORDER BY alias ASC")
    suspend fun searchAliases(pattern: String): List<UUIDAliasEntity>

    /**
     * Get aliases for package
     *
     * Finds all aliases for UUIDs starting with package name.
     *
     * @param packageName Package name prefix
     * @return List of aliases for package
     */
    @Query("SELECT * FROM uuid_aliases WHERE uuid LIKE :packageName || '%' ORDER BY alias ASC")
    suspend fun getAliasesForPackage(packageName: String): List<UUIDAliasEntity>
}
