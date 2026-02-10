/**
 * IScrapedWebsiteRepository.kt - Repository interface for scraped website cache
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-10
 */
package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ScrapedWebsiteDTO

/**
 * Repository for scraped website cache entries.
 *
 * Manages the persistence layer of the two-tier DOM scraping cache.
 * Stores website metadata (URL hash, structure hash) for cross-session
 * change detection and command restoration.
 */
interface IScrapedWebsiteRepository {

    /**
     * Insert or replace a scraped website entry.
     */
    suspend fun insert(website: ScrapedWebsiteDTO)

    /**
     * Get a website by its URL hash.
     * Returns null if not found.
     */
    suspend fun getByUrlHash(urlHash: String): ScrapedWebsiteDTO?

    /**
     * Get all websites for a domain, ordered by last access.
     */
    suspend fun getByDomain(domain: String): List<ScrapedWebsiteDTO>

    /**
     * Update access metadata (last accessed timestamp and access count).
     */
    suspend fun updateAccessMetadata(urlHash: String, lastAccessedAt: Long, accessCount: Int)

    /**
     * Update the structure hash and reset stale flag.
     * Called when the DOM structure has changed since last scrape.
     */
    suspend fun updateStructureHash(urlHash: String, structureHash: String, scrapedAt: Long)

    /**
     * Mark a website as stale (forces rescrape on next visit).
     */
    suspend fun markAsStale(urlHash: String)

    /**
     * Delete a website by URL hash.
     */
    suspend fun deleteByUrlHash(urlHash: String)

    /**
     * Get websites that are stale or older than the given timestamp.
     * Used for TTL-based cache eviction.
     */
    suspend fun getStaleWebsites(olderThan: Long): List<ScrapedWebsiteDTO>

    /**
     * Count total cached websites.
     */
    suspend fun count(): Long
}
