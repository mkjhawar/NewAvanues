/**
 * IScrapedWebCommandRepository.kt - Repository interface for scraped web voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-14
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.ScrapedWebCommandDTO

/**
 * Repository interface for scraped web voice commands.
 * Provides CRUD operations for web element voice commands.
 */
interface IScrapedWebCommandRepository {

    /**
     * Insert or replace a web command.
     * @return The ID of the inserted command.
     */
    suspend fun insert(command: ScrapedWebCommandDTO): Long

    /**
     * Insert multiple commands in a batch.
     */
    suspend fun insertBatch(commands: List<ScrapedWebCommandDTO>)

    /**
     * Get command by ID.
     */
    suspend fun getById(id: Long): ScrapedWebCommandDTO?

    /**
     * Get all commands for a domain.
     */
    suspend fun getByDomain(domainId: String): List<ScrapedWebCommandDTO>

    /**
     * Get commands for a domain matching a specific URL pattern.
     */
    suspend fun getByDomainAndUrl(domainId: String, url: String): List<ScrapedWebCommandDTO>

    /**
     * Get commands by element hash.
     */
    suspend fun getByElementHash(elementHash: String, domainId: String): List<ScrapedWebCommandDTO>

    /**
     * Get high-confidence commands for a domain.
     */
    suspend fun getHighConfidence(domainId: String, minConfidence: Float): List<ScrapedWebCommandDTO>

    /**
     * Get user-approved commands for a domain.
     */
    suspend fun getUserApproved(domainId: String): List<ScrapedWebCommandDTO>

    /**
     * Get most used commands for a domain.
     */
    suspend fun getMostUsed(domainId: String, limit: Int): List<ScrapedWebCommandDTO>

    /**
     * Count commands for a domain.
     */
    suspend fun countByDomain(domainId: String): Long

    /**
     * Update command confidence.
     */
    suspend fun updateConfidence(id: Long, confidence: Float, lastVerified: Long)

    /**
     * Mark command as user-approved.
     */
    suspend fun markApproved(id: Long, approvedAt: Long)

    /**
     * Add or update synonyms for a command.
     */
    suspend fun updateSynonyms(id: Long, synonyms: List<String>)

    /**
     * Increment usage count for a command.
     */
    suspend fun incrementUsage(id: Long, lastUsed: Long)

    /**
     * Increment usage by element hash and domain.
     */
    suspend fun incrementUsageByHash(elementHash: String, domainId: String, lastUsed: Long)

    /**
     * Mark as verified (command still exists on page).
     */
    suspend fun markVerified(elementHash: String, domainId: String, lastVerified: Long)

    /**
     * Mark old commands as deprecated.
     */
    suspend fun markDeprecated(domainId: String, olderThan: Long)

    /**
     * Delete deprecated commands older than a timestamp.
     */
    suspend fun deleteDeprecated(olderThan: Long)

    /**
     * Delete all commands for a domain.
     */
    suspend fun deleteByDomain(domainId: String)

    /**
     * Vacuum/cleanup low-quality deprecated commands.
     */
    suspend fun vacuum(olderThan: Long)
}
