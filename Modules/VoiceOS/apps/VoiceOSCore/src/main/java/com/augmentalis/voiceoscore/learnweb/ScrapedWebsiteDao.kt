/**
 * ScrapedWebsiteDao.kt - SQLDelight repository for website operations
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnweb/ScrapedWebsiteDao.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-13
 * Migrated to SQLDelight: 2025-12-17
 *
 * Repository for scraped website CRUD operations and cache management
 */

package com.augmentalis.voiceoscore.learnweb

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.web.ScrapedWebsiteQueries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scraped Website Repository
 *
 * Repository for scraped website operations using SQLDelight.
 * Supports Hybrid Smart caching with TTL, staleness tracking, and hierarchy queries.
 *
 * @since 1.0.0
 */
class ScrapedWebsiteDao(private val database: VoiceOSDatabase) {

    private val queries: ScrapedWebsiteQueries = database.scrapedWebsiteQueries

    /**
     * Insert or replace website
     *
     * @param website Website to insert
     */
    suspend fun insert(website: ScrapedWebsite) = withContext(Dispatchers.IO) {
        queries.insertScrapedWebsite(
            url_hash = website.urlHash,
            url = website.url,
            domain = website.domain,
            title = website.title,
            structure_hash = website.structureHash,
            parent_url_hash = website.parentUrlHash,
            scraped_at = website.scrapedAt,
            last_accessed_at = website.lastAccessedAt,
            access_count = website.accessCount.toLong(),
            is_stale = if (website.isStale) 1L else 0L
        )
    }

    /**
     * Update website
     *
     * @param website Website to update
     */
    suspend fun update(website: ScrapedWebsite) = insert(website)

    /**
     * Get website by URL hash
     *
     * @param urlHash URL hash
     * @return Website or null
     */
    suspend fun getByUrlHash(urlHash: String): ScrapedWebsite? = withContext(Dispatchers.IO) {
        queries.getByUrlHash(urlHash).executeAsOneOrNull()?.let { mapToScrapedWebsite(it) }
    }

    /**
     * Get website by domain
     *
     * @param domain Domain name
     * @return List of websites
     */
    suspend fun getByDomain(domain: String): List<ScrapedWebsite> = withContext(Dispatchers.IO) {
        queries.getByDomain(domain).executeAsList().map { mapToScrapedWebsite(it) }
    }

    /**
     * Get child websites (by parent URL hash)
     *
     * @param parentUrlHash Parent URL hash
     * @return List of child websites
     */
    suspend fun getChildren(parentUrlHash: String): List<ScrapedWebsite> = withContext(Dispatchers.IO) {
        queries.getChildren(parentUrlHash).executeAsList().map { mapToScrapedWebsite(it) }
    }

    /**
     * Get all stale websites (> 12 hours old)
     *
     * @param staleThreshold Timestamp threshold for staleness
     * @return List of stale websites
     */
    suspend fun getStaleWebsites(staleThreshold: Long): List<ScrapedWebsite> = withContext(Dispatchers.IO) {
        queries.getStaleWebsites(staleThreshold).executeAsList().map { mapToScrapedWebsite(it) }
    }

    /**
     * Mark website as stale
     *
     * @param urlHash URL hash
     */
    suspend fun markAsStale(urlHash: String) = withContext(Dispatchers.IO) {
        queries.markAsStale(urlHash)
    }

    /**
     * Update access metadata
     *
     * @param urlHash URL hash
     * @param lastAccessedAt New last accessed timestamp
     * @param accessCount New access count
     */
    suspend fun updateAccessMetadata(urlHash: String, lastAccessedAt: Long, accessCount: Int) = withContext(Dispatchers.IO) {
        queries.updateAccessMetadata(
            url_hash = urlHash,
            last_accessed_at = lastAccessedAt,
            access_count = accessCount.toLong()
        )
    }

    /**
     * Invalidate cache by structure change
     *
     * @param urlHash URL hash
     * @param newStructureHash New structure hash
     * @param timestamp Current timestamp
     */
    suspend fun updateStructureHash(urlHash: String, newStructureHash: String, timestamp: Long) = withContext(Dispatchers.IO) {
        queries.updateStructureHash(
            url_hash = urlHash,
            structure_hash = newStructureHash,
            scraped_at = timestamp
        )
    }

    /**
     * Delete website by URL hash
     *
     * @param urlHash URL hash
     */
    suspend fun deleteByUrlHash(urlHash: String) = withContext(Dispatchers.IO) {
        queries.deleteByUrlHash(urlHash)
    }

    /**
     * Delete all websites
     */
    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        queries.deleteAll()
    }

    /**
     * Get all websites ordered by access count (most used first)
     *
     * @return List of websites
     */
    suspend fun getAllByUsage(): List<ScrapedWebsite> = withContext(Dispatchers.IO) {
        queries.getAllByUsage().executeAsList().map { mapToScrapedWebsite(it) }
    }

    /**
     * Get cache statistics
     *
     * @return Cache statistics
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        queries.getCacheStats().executeAsOne().let {
            CacheStats(
                total = it.total ?: 0L,
                stale = it.stale,
                avgAccess = it.avg_access
            )
        }
    }

    /**
     * Map SQLDelight result to ScrapedWebsite data class
     */
    private fun mapToScrapedWebsite(result: com.augmentalis.database.web.GetByUrlHash): ScrapedWebsite {
        return ScrapedWebsite(
            urlHash = result.url_hash,
            url = result.url,
            domain = result.domain,
            title = result.title,
            structureHash = result.structure_hash,
            parentUrlHash = result.parent_url_hash,
            scrapedAt = result.scraped_at,
            lastAccessedAt = result.last_accessed_at,
            accessCount = result.access_count.toInt(),
            isStale = result.is_stale == 1L
        )
    }

    private fun mapToScrapedWebsite(result: com.augmentalis.database.web.GetByDomain): ScrapedWebsite {
        return ScrapedWebsite(
            urlHash = result.url_hash,
            url = result.url,
            domain = result.domain,
            title = result.title,
            structureHash = result.structure_hash,
            parentUrlHash = result.parent_url_hash,
            scrapedAt = result.scraped_at,
            lastAccessedAt = result.last_accessed_at,
            accessCount = result.access_count.toInt(),
            isStale = result.is_stale == 1L
        )
    }

    private fun mapToScrapedWebsite(result: com.augmentalis.database.web.GetChildren): ScrapedWebsite {
        return ScrapedWebsite(
            urlHash = result.url_hash,
            url = result.url,
            domain = result.domain,
            title = result.title,
            structureHash = result.structure_hash,
            parentUrlHash = result.parent_url_hash,
            scrapedAt = result.scraped_at,
            lastAccessedAt = result.last_accessed_at,
            accessCount = result.access_count.toInt(),
            isStale = result.is_stale == 1L
        )
    }

    private fun mapToScrapedWebsite(result: com.augmentalis.database.web.GetStaleWebsites): ScrapedWebsite {
        return ScrapedWebsite(
            urlHash = result.url_hash,
            url = result.url,
            domain = result.domain,
            title = result.title,
            structureHash = result.structure_hash,
            parentUrlHash = result.parent_url_hash,
            scrapedAt = result.scraped_at,
            lastAccessedAt = result.last_accessed_at,
            accessCount = result.access_count.toInt(),
            isStale = result.is_stale == 1L
        )
    }

    private fun mapToScrapedWebsite(result: com.augmentalis.database.web.GetAllByUsage): ScrapedWebsite {
        return ScrapedWebsite(
            urlHash = result.url_hash,
            url = result.url,
            domain = result.domain,
            title = result.title,
            structureHash = result.structure_hash,
            parentUrlHash = result.parent_url_hash,
            scrapedAt = result.scraped_at,
            lastAccessedAt = result.last_accessed_at,
            accessCount = result.access_count.toInt(),
            isStale = result.is_stale == 1L
        )
    }
}
