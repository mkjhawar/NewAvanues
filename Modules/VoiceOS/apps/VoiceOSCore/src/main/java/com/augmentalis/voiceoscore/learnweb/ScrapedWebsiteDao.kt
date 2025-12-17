/**
 * ScrapedWebsiteDao.kt - DAO for website operations
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/ScrapedWebsiteDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 *
 * Data Access Object for scraped website CRUD operations and cache management
 */

package com.augmentalis.voiceoscore.learnweb

import androidx.room.*

/**
 * Cache statistics data class
 */
data class CacheStats(
    val total: Int,
    val stale: Int,
    val avg_access: Double
)

/**
 * Scraped Website DAO
 *
 * Data Access Object for scraped website operations.
 * Supports Hybrid Smart caching with TTL, staleness tracking, and hierarchy queries.
 *
 * @since 1.0.0
 */
@Dao
interface ScrapedWebsiteDao {

    /**
     * Insert or replace website
     *
     * @param website Website to insert
     * @return Row ID
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(website: ScrapedWebsite): Long

    /**
     * Update website
     *
     * @param website Website to update
     */
    @Update
    suspend fun update(website: ScrapedWebsite)

    /**
     * Get website by URL hash
     *
     * @param urlHash URL hash
     * @return Website or null
     */
    @Query("SELECT * FROM scraped_websites WHERE url_hash = :urlHash")
    suspend fun getByUrlHash(urlHash: String): ScrapedWebsite?

    /**
     * Get website by domain
     *
     * @param domain Domain name
     * @return List of websites
     */
    @Query("SELECT * FROM scraped_websites WHERE domain = :domain ORDER BY last_accessed_at DESC")
    suspend fun getByDomain(domain: String): List<ScrapedWebsite>

    /**
     * Get child websites (by parent URL hash)
     *
     * @param parentUrlHash Parent URL hash
     * @return List of child websites
     */
    @Query("SELECT * FROM scraped_websites WHERE parent_url_hash = :parentUrlHash ORDER BY last_accessed_at DESC")
    suspend fun getChildren(parentUrlHash: String): List<ScrapedWebsite>

    /**
     * Get all stale websites (> 12 hours old)
     *
     * @param staleThreshold Timestamp threshold for staleness
     * @return List of stale websites
     */
    @Query("SELECT * FROM scraped_websites WHERE is_stale = 1 OR (scraped_at < :staleThreshold)")
    suspend fun getStaleWebsites(staleThreshold: Long): List<ScrapedWebsite>

    /**
     * Mark website as stale
     *
     * @param urlHash URL hash
     */
    @Query("UPDATE scraped_websites SET is_stale = 1 WHERE url_hash = :urlHash")
    suspend fun markAsStale(urlHash: String)

    /**
     * Update access metadata
     *
     * @param urlHash URL hash
     * @param lastAccessedAt New last accessed timestamp
     * @param accessCount New access count
     */
    @Query("UPDATE scraped_websites SET last_accessed_at = :lastAccessedAt, access_count = :accessCount WHERE url_hash = :urlHash")
    suspend fun updateAccessMetadata(urlHash: String, lastAccessedAt: Long, accessCount: Int)

    /**
     * Invalidate cache by structure change
     *
     * @param urlHash URL hash
     * @param newStructureHash New structure hash
     */
    @Query("UPDATE scraped_websites SET structure_hash = :newStructureHash, scraped_at = :timestamp, is_stale = 0 WHERE url_hash = :urlHash")
    suspend fun updateStructureHash(urlHash: String, newStructureHash: String, timestamp: Long)

    /**
     * Delete website by URL hash
     *
     * @param urlHash URL hash
     */
    @Query("DELETE FROM scraped_websites WHERE url_hash = :urlHash")
    suspend fun deleteByUrlHash(urlHash: String)

    /**
     * Delete all websites
     */
    @Query("DELETE FROM scraped_websites")
    suspend fun deleteAll()

    /**
     * Get all websites ordered by access count (most used first)
     *
     * @return List of websites
     */
    @Query("SELECT * FROM scraped_websites ORDER BY access_count DESC, last_accessed_at DESC")
    suspend fun getAllByUsage(): List<ScrapedWebsite>

    /**
     * Get cache statistics
     *
     * @return Map of statistics
     */
    @Query("""
        SELECT
            COUNT(*) as total,
            SUM(CASE WHEN is_stale = 1 THEN 1 ELSE 0 END) as stale,
            AVG(access_count) as avg_access
        FROM scraped_websites
    """)
    suspend fun getCacheStats(): CacheStats
}
