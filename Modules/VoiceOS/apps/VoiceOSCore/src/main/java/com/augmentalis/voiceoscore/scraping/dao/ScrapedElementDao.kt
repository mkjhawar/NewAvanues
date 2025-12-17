/**
 * ScrapedElementDao.kt - Data Access Object for scraped UI elements
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
import androidx.room.Update
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity

/**
 * DAO for ScrapedElementEntity operations
 *
 * Provides database access for UI element data including hash-based lookups,
 * filtering by capabilities (clickable, editable, etc.), and batch operations.
 */
@Dao
interface ScrapedElementDao {

    /**
     * Insert a new scraped element
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(element: ScrapedElementEntity): Long

    /**
     * Insert multiple elements in batch (optimized for tree scraping)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(elements: List<ScrapedElementEntity>)

    /**
     * Insert multiple elements in batch and return database-assigned IDs
     *
     * This method is used for hierarchy insertion where we need to capture
     * the auto-generated IDs to build valid foreign key relationships.
     *
     * @param elements List of elements to insert (id should be 0 for auto-generation)
     * @return List of database-assigned element IDs in the same order as input list
     *
     * Example:
     * ```
     * val elements = listOf(
     *     ScrapedElementEntity(id = 0, appId = "app1", ...),
     *     ScrapedElementEntity(id = 0, appId = "app1", ...)
     * )
     * val ids = insertBatchWithIds(elements)  // Returns [1001, 1002]
     * ```
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatchWithIds(elements: List<ScrapedElementEntity>): List<Long>

    /**
     * Update existing element
     */
    @Update
    suspend fun update(element: ScrapedElementEntity)

    /**
     * Find element by database ID
     */
    @Query("SELECT * FROM scraped_elements WHERE id = :id")
    suspend fun getElementById(id: Long): ScrapedElementEntity?

    /**
     * Find element by hash (primary lookup method for voice commands)
     * This is O(1) lookup using indexed hash column
     */
    @Query("SELECT * FROM scraped_elements WHERE element_hash = :hash")
    suspend fun getElementByHash(hash: String): ScrapedElementEntity?

    /**
     * Find all elements belonging to an app
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId ORDER BY depth, index_in_parent")
    suspend fun getElementsByAppId(appId: String): List<ScrapedElementEntity>

    /**
     * Find clickable elements in an app (candidates for click commands)
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND is_clickable = 1")
    suspend fun getClickableElements(appId: String): List<ScrapedElementEntity>

    /**
     * Find editable elements in an app (candidates for text input commands)
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND is_editable = 1")
    suspend fun getEditableElements(appId: String): List<ScrapedElementEntity>

    /**
     * Find scrollable elements in an app
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND is_scrollable = 1")
    suspend fun getScrollableElements(appId: String): List<ScrapedElementEntity>

    /**
     * Find elements by class name (e.g., all buttons)
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND class_name = :className")
    suspend fun getElementsByClassName(appId: String, className: String): List<ScrapedElementEntity>

    /**
     * Find elements by view ID resource name
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND view_id_resource_name = :viewId")
    suspend fun getElementsByViewId(appId: String, viewId: String): List<ScrapedElementEntity>

    /**
     * Find elements by text content (partial match)
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND text LIKE '%' || :text || '%'")
    suspend fun getElementsByTextContaining(appId: String, text: String): List<ScrapedElementEntity>

    /**
     * Find elements by content description (partial match)
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND content_description LIKE '%' || :description || '%'")
    suspend fun getElementsByContentDescription(appId: String, description: String): List<ScrapedElementEntity>

    /**
     * Find elements at a specific depth in the tree
     */
    @Query("SELECT * FROM scraped_elements WHERE app_id = :appId AND depth = :depth")
    suspend fun getElementsByDepth(appId: String, depth: Int): List<ScrapedElementEntity>

    /**
     * Get element count for an app
     */
    @Query("SELECT COUNT(*) FROM scraped_elements WHERE app_id = :appId")
    suspend fun getElementCountForApp(appId: String): Int

    /**
     * Delete all elements for an app (called when app is deleted)
     */
    @Query("DELETE FROM scraped_elements WHERE app_id = :appId")
    suspend fun deleteElementsForApp(appId: String)

    /**
     * Delete elements older than specified timestamp
     */
    @Query("DELETE FROM scraped_elements WHERE scraped_at < :timestamp")
    suspend fun deleteElementsOlderThan(timestamp: Long): Int

    /**
     * Check if element hash exists (fast existence check)
     */
    @Query("SELECT EXISTS(SELECT 1 FROM scraped_elements WHERE element_hash = :hash)")
    suspend fun elementHashExists(hash: String): Boolean

    /**
     * Insert or update element based on hash (UPSERT operation)
     *
     * This method implements hash-based merging for LearnApp mode:
     * - If element with same hash exists: updates it with new data (preserves database ID)
     * - If element doesn't exist: inserts as new element
     *
     * This ensures that:
     * 1. Dynamic mode elements are updated (not duplicated) when LearnApp runs
     * 2. LearnApp mode elements are updated when Dynamic mode revisits them
     * 3. No duplicate elements exist for same UI component
     *
     * @param element Element to insert or update (hash is the matching key)
     * @return Element hash for reference
     *
     * Example usage:
     * ```
     * val element = ScrapedElementEntity(...)
     * val hash = upsertElement(element)
     * // Element inserted or updated based on hash match
     * ```
     */
    @androidx.room.Transaction
    suspend fun upsertElement(element: ScrapedElementEntity): String {
        val existing = getElementByHash(element.elementHash)

        if (existing != null) {
            // Element exists - update with new data, preserve database ID
            val updated = element.copy(id = existing.id)
            update(updated)
            android.util.Log.d("ScrapedElementDao", "Updated existing element: ${element.elementHash}")
        } else {
            // Element doesn't exist - insert new
            insert(element)
            android.util.Log.d("ScrapedElementDao", "Inserted new element: ${element.elementHash}")
        }

        return element.elementHash
    }

    /**
     * Update formGroupId for specific element hash
     * Used for Phase 2.5 form grouping enhancement
     */
    @Query("UPDATE scraped_elements SET form_group_id = :formGroupId WHERE element_hash = :elementHash")
    suspend fun updateFormGroupId(elementHash: String, formGroupId: String?)

    /**
     * Update formGroupId for multiple elements in batch
     */
    suspend fun updateFormGroupIdBatch(elementHashes: List<String>, formGroupId: String?) {
        elementHashes.forEach { hash ->
            updateFormGroupId(hash, formGroupId)
        }
    }

    /**
     * Delete element by hash
     * Used for cascade delete testing
     */
    @Query("DELETE FROM scraped_elements WHERE element_hash = :elementHash")
    suspend fun deleteByHash(elementHash: String): Int

    /**
     * Get all elements (for testing)
     */
    @Query("SELECT * FROM scraped_elements")
    suspend fun getAllElements(): List<ScrapedElementEntity>
}
