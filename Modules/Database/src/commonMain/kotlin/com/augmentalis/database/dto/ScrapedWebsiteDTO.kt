/**
 * ScrapedWebsiteDTO.kt - Data Transfer Object for scraped website cache entries
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-10
 */
package com.augmentalis.database.dto

import com.augmentalis.database.Scraped_websites

/**
 * DTO for scraped website cache entries.
 *
 * Stores metadata about a scraped web page including its structural fingerprint
 * for change detection. Used by the two-tier cache system (session + DB) to
 * avoid redundant DOM scraping on revisited pages.
 */
data class ScrapedWebsiteDTO(
    val urlHash: String,
    val url: String,
    val domain: String,
    val title: String,
    val structureHash: String,
    val parentUrlHash: String? = null,
    val scrapedAt: Long,
    val lastAccessedAt: Long,
    val accessCount: Int = 0,
    val isStale: Boolean = false
)

/**
 * Extension to convert SQLDelight generated type to DTO.
 */
fun Scraped_websites.toScrapedWebsiteDTO(): ScrapedWebsiteDTO {
    return ScrapedWebsiteDTO(
        urlHash = url_hash,
        url = url,
        domain = domain,
        title = title,
        structureHash = structure_hash,
        parentUrlHash = parent_url_hash,
        scrapedAt = scraped_at,
        lastAccessedAt = last_accessed_at,
        accessCount = access_count.toInt(),
        isStale = is_stale == 1L
    )
}
