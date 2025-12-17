/**
 * GeneratedCommandDao.kt - Data Access Object for generated voice commands
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
import com.augmentalis.voiceoscore.scraping.entities.GeneratedCommandEntity

/**
 * DAO for GeneratedCommandEntity operations
 *
 * Provides database access for voice command data including fuzzy matching,
 * usage statistics tracking, and synonym-based search.
 */
@Dao
interface GeneratedCommandDao {

    /**
     * Insert a new generated command
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: GeneratedCommandEntity): Long

    /**
     * Insert multiple commands in batch
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBatch(commands: List<GeneratedCommandEntity>)

    /**
     * Update existing command (used for usage tracking)
     */
    @Update
    suspend fun update(command: GeneratedCommandEntity)

    /**
     * Find command by ID
     */
    @Query("SELECT * FROM generated_commands WHERE id = :id")
    suspend fun getCommandById(id: Long): GeneratedCommandEntity?

    /**
     * Get all commands (used for testing and debugging)
     */
    @Query("SELECT * FROM generated_commands ORDER BY confidence DESC")
    suspend fun getAll(): List<GeneratedCommandEntity>

    /**
     * Get all commands (alias for getAll)
     */
    @Query("SELECT * FROM generated_commands ORDER BY confidence DESC")
    suspend fun getAllCommands(): List<GeneratedCommandEntity>

    /**
     * Find commands for a specific element by hash
     */
    @Query("SELECT * FROM generated_commands WHERE element_hash = :elementHash ORDER BY confidence DESC")
    suspend fun getCommandsForElement(elementHash: String): List<GeneratedCommandEntity>

    /**
     * Find command by exact text match
     * Returns highest confidence match if multiple exist
     */
    @Query("SELECT * FROM generated_commands WHERE command_text = :commandText ORDER BY confidence DESC LIMIT 1")
    suspend fun getCommandByText(commandText: String): GeneratedCommandEntity?

    /**
     * Find commands by partial text match (fuzzy search)
     * Uses LIKE for substring matching
     */
    @Query("SELECT * FROM generated_commands WHERE command_text LIKE '%' || :searchText || '%' ORDER BY confidence DESC")
    suspend fun searchCommandsByText(searchText: String): List<GeneratedCommandEntity>

    /**
     * Find commands by action type
     */
    @Query("SELECT * FROM generated_commands WHERE action_type = :actionType ORDER BY confidence DESC")
    suspend fun getCommandsByActionType(actionType: String): List<GeneratedCommandEntity>

    /**
     * Find commands for an app (via element relationship)
     * Joins with scraped_elements to get app_id
     */
    @Query("""
        SELECT gc.* FROM generated_commands gc
        JOIN scraped_elements se ON gc.element_hash = se.element_hash
        WHERE se.app_id = :appId
        ORDER BY gc.confidence DESC
    """)
    suspend fun getCommandsForApp(appId: String): List<GeneratedCommandEntity>

    /**
     * Find user-approved commands (high-quality commands)
     */
    @Query("SELECT * FROM generated_commands WHERE is_user_approved = 1 ORDER BY usage_count DESC")
    suspend fun getUserApprovedCommands(): List<GeneratedCommandEntity>

    /**
     * Find most-used commands (top N)
     */
    @Query("SELECT * FROM generated_commands WHERE usage_count > 0 ORDER BY usage_count DESC LIMIT :limit")
    suspend fun getMostUsedCommands(limit: Int): List<GeneratedCommandEntity>

    /**
     * Find high-confidence commands (confidence >= threshold)
     */
    @Query("SELECT * FROM generated_commands WHERE confidence >= :threshold ORDER BY confidence DESC")
    suspend fun getHighConfidenceCommands(threshold: Float): List<GeneratedCommandEntity>

    /**
     * Find recently used commands
     */
    @Query("SELECT * FROM generated_commands WHERE last_used IS NOT NULL ORDER BY last_used DESC LIMIT :limit")
    suspend fun getRecentlyUsedCommands(limit: Int): List<GeneratedCommandEntity>

    /**
     * Increment usage count and update last_used timestamp
     */
    @Query("UPDATE generated_commands SET usage_count = usage_count + 1, last_used = :timestamp WHERE id = :commandId")
    suspend fun incrementUsage(commandId: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Mark command as user-approved
     */
    @Query("UPDATE generated_commands SET is_user_approved = 1 WHERE id = :commandId")
    suspend fun markAsUserApproved(commandId: Long)

    /**
     * Update command confidence score
     */
    @Query("UPDATE generated_commands SET confidence = :confidence WHERE id = :commandId")
    suspend fun updateConfidence(commandId: Long, confidence: Float)

    /**
     * Get command count for an element by hash
     */
    @Query("SELECT COUNT(*) FROM generated_commands WHERE element_hash = :elementHash")
    suspend fun getCommandCountForElement(elementHash: String): Int

    /**
     * Get total command count for an app
     */
    @Query("""
        SELECT COUNT(*) FROM generated_commands gc
        JOIN scraped_elements se ON gc.element_hash = se.element_hash
        WHERE se.app_id = :appId
    """)
    suspend fun getCommandCountForApp(appId: String): Int

    /**
     * Get total count of all generated commands across all apps
     */
    @Query("SELECT COUNT(*) FROM generated_commands")
    suspend fun getTotalCommandCount(): Int

    /**
     * Delete commands for a specific element by hash
     */
    @Query("DELETE FROM generated_commands WHERE element_hash = :elementHash")
    suspend fun deleteCommandsForElement(elementHash: String)

    /**
     * Delete commands for an app (via element relationship)
     */
    @Query("""
        DELETE FROM generated_commands
        WHERE element_hash IN (SELECT element_hash FROM scraped_elements WHERE app_id = :appId)
    """)
    suspend fun deleteCommandsForApp(appId: String)

    /**
     * Delete commands older than specified timestamp
     */
    @Query("DELETE FROM generated_commands WHERE generated_at < :timestamp")
    suspend fun deleteCommandsOlderThan(timestamp: Long): Int

    /**
     * Delete low-confidence, unused commands (cleanup)
     * Removes commands that have never been used and have low confidence
     */
    @Query("DELETE FROM generated_commands WHERE usage_count = 0 AND confidence < :threshold")
    suspend fun deleteLowQualityCommands(threshold: Float = 0.3f): Int
}
