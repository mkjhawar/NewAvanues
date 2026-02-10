/**
 * SQLDelightScrapedWebsiteRepository.kt - SQLDelight implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-10
 */
package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.ScrapedWebsiteDTO
import com.augmentalis.database.dto.toScrapedWebsiteDTO
import com.augmentalis.database.repositories.IScrapedWebsiteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SQLDelightScrapedWebsiteRepository(
    private val database: VoiceOSDatabase
) : IScrapedWebsiteRepository {

    private val queries = database.scrapedWebsiteQueries

    override suspend fun insert(website: ScrapedWebsiteDTO) = withContext(Dispatchers.Default) {
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

    override suspend fun getByUrlHash(urlHash: String): ScrapedWebsiteDTO? = withContext(Dispatchers.Default) {
        queries.getByUrlHash(urlHash).executeAsOneOrNull()?.toScrapedWebsiteDTO()
    }

    override suspend fun getByDomain(domain: String): List<ScrapedWebsiteDTO> = withContext(Dispatchers.Default) {
        queries.getByDomain(domain).executeAsList().map { it.toScrapedWebsiteDTO() }
    }

    override suspend fun updateAccessMetadata(
        urlHash: String,
        lastAccessedAt: Long,
        accessCount: Int
    ) = withContext(Dispatchers.Default) {
        queries.updateAccessMetadata(lastAccessedAt, accessCount.toLong(), urlHash)
    }

    override suspend fun updateStructureHash(
        urlHash: String,
        structureHash: String,
        scrapedAt: Long
    ) = withContext(Dispatchers.Default) {
        queries.updateStructureHash(structureHash, scrapedAt, urlHash)
    }

    override suspend fun markAsStale(urlHash: String) = withContext(Dispatchers.Default) {
        queries.markAsStale(urlHash)
    }

    override suspend fun deleteByUrlHash(urlHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByUrlHash(urlHash)
    }

    override suspend fun getStaleWebsites(olderThan: Long): List<ScrapedWebsiteDTO> = withContext(Dispatchers.Default) {
        queries.getStaleWebsites(olderThan).executeAsList().map { it.toScrapedWebsiteDTO() }
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }
}
