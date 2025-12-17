/**
 * GeneratedWebCommandDao.kt - DAO for web command operations
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/GeneratedWebCommandDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Data Access Object for generated web command CRUD operations
 */

package com.augmentalis.voiceoscore.learnweb

import androidx.room.*

/**
 * Generated Web Command DAO
 *
 * Data Access Object for generated web command operations.
 * Supports command retrieval, usage tracking, and synonym searches.
 *
 * @since 1.0.0
 */
@Dao
interface GeneratedWebCommandDao {

    /**
     * Insert or replace command
     *
     * @param command Command to insert
     * @return Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(command: GeneratedWebCommand): Long

    /**
     * Insert multiple commands
     *
     * @param commands Commands to insert
     * @return List of row IDs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(commands: List<GeneratedWebCommand>): List<Long>

    /**
     * Update command
     *
     * @param command Command to update
     */
    @Update
    suspend fun update(command: GeneratedWebCommand)

    /**
     * Get all commands for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return List of commands
     */
    @Query("SELECT * FROM generated_web_commands WHERE website_url_hash = :websiteUrlHash ORDER BY usage_count DESC")
    suspend fun getByWebsiteUrlHash(websiteUrlHash: String): List<GeneratedWebCommand>

    /**
     * Get commands for a URL
     *
     * @param url Normalized URL (without protocol, www)
     * @return List of commands for this URL
     */
    @Query("""
        SELECT gwc.* FROM generated_web_commands gwc
        JOIN scraped_websites sw ON gwc.website_url_hash = sw.url_hash
        WHERE (
            sw.url LIKE '%' || :url || '%'
            OR sw.domain LIKE '%' || :url || '%'
        )
        ORDER BY gwc.usage_count DESC
    """)
    suspend fun getCommandsForUrl(url: String): List<GeneratedWebCommand>

    /**
     * Get commands by element hash
     *
     * @param elementHash Element hash
     * @return List of commands
     */
    @Query("SELECT * FROM generated_web_commands WHERE element_hash = :elementHash")
    suspend fun getByElementHash(elementHash: String): List<GeneratedWebCommand>

    /**
     * Search commands by text (includes synonyms)
     *
     * @param websiteUrlHash Website URL hash
     * @param searchText Search text (case-insensitive)
     * @return List of matching commands
     */
    @Query("""
        SELECT * FROM generated_web_commands
        WHERE website_url_hash = :websiteUrlHash
        AND (command_text LIKE '%' || :searchText || '%' OR synonyms LIKE '%' || :searchText || '%')
        COLLATE NOCASE
        ORDER BY usage_count DESC
    """)
    suspend fun searchCommands(websiteUrlHash: String, searchText: String): List<GeneratedWebCommand>

    /**
     * Get commands by action type
     *
     * @param websiteUrlHash Website URL hash
     * @param action Action type (e.g., "CLICK", "SCROLL_TO")
     * @return List of commands
     */
    @Query("SELECT * FROM generated_web_commands WHERE website_url_hash = :websiteUrlHash AND action = :action ORDER BY usage_count DESC")
    suspend fun getByAction(websiteUrlHash: String, action: String): List<GeneratedWebCommand>

    /**
     * Update command usage
     *
     * @param commandId Command ID
     * @param usageCount New usage count
     * @param lastUsedAt Timestamp of last usage
     */
    @Query("UPDATE generated_web_commands SET usage_count = :usageCount, last_used_at = :lastUsedAt WHERE id = :commandId")
    suspend fun updateUsage(commandId: Long, usageCount: Int, lastUsedAt: Long)

    /**
     * Increment command usage
     *
     * @param commandId Command ID
     */
    @Query("UPDATE generated_web_commands SET usage_count = usage_count + 1, last_used_at = :timestamp WHERE id = :commandId")
    suspend fun incrementUsage(commandId: Long, timestamp: Long)

    /**
     * Get most used commands for a website
     *
     * @param websiteUrlHash Website URL hash
     * @param limit Number of commands to return
     * @return List of most used commands
     */
    @Query("SELECT * FROM generated_web_commands WHERE website_url_hash = :websiteUrlHash ORDER BY usage_count DESC LIMIT :limit")
    suspend fun getMostUsed(websiteUrlHash: String, limit: Int): List<GeneratedWebCommand>

    /**
     * Get recently used commands for a website
     *
     * @param websiteUrlHash Website URL hash
     * @param limit Number of commands to return
     * @return List of recently used commands
     */
    @Query("SELECT * FROM generated_web_commands WHERE website_url_hash = :websiteUrlHash AND last_used_at IS NOT NULL ORDER BY last_used_at DESC LIMIT :limit")
    suspend fun getRecentlyUsed(websiteUrlHash: String, limit: Int): List<GeneratedWebCommand>

    /**
     * Delete all commands for a website
     *
     * @param websiteUrlHash Website URL hash
     */
    @Query("DELETE FROM generated_web_commands WHERE website_url_hash = :websiteUrlHash")
    suspend fun deleteByWebsiteUrlHash(websiteUrlHash: String)

    /**
     * Delete commands by element hash
     *
     * @param elementHash Element hash
     */
    @Query("DELETE FROM generated_web_commands WHERE element_hash = :elementHash")
    suspend fun deleteByElementHash(elementHash: String)

    /**
     * Delete all commands
     */
    @Query("DELETE FROM generated_web_commands")
    suspend fun deleteAll()

    /**
     * Get all commands (for testing and registration)
     *
     * @return List of all commands
     */
    @Query("SELECT * FROM generated_web_commands ORDER BY usage_count DESC")
    suspend fun getAllCommands(): List<GeneratedWebCommand>

    /**
     * Get command count for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return Command count
     */
    @Query("SELECT COUNT(*) FROM generated_web_commands WHERE website_url_hash = :websiteUrlHash")
    suspend fun getCommandCount(websiteUrlHash: String): Int

    /**
     * Get total command usage for a website
     *
     * @param websiteUrlHash Website URL hash
     * @return Total usage count
     */
    @Query("SELECT SUM(usage_count) FROM generated_web_commands WHERE website_url_hash = :websiteUrlHash")
    suspend fun getTotalUsage(websiteUrlHash: String): Int?
}
