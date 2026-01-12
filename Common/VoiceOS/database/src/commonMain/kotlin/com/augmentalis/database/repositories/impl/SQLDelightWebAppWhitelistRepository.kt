/**
 * SQLDelightWebAppWhitelistRepository.kt - SQLDelight implementation of IWebAppWhitelistRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-12
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.WebAppWhitelistDTO
import com.augmentalis.database.dto.toWebAppWhitelistDTO
import com.augmentalis.database.repositories.IWebAppWhitelistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IWebAppWhitelistRepository.
 */
class SQLDelightWebAppWhitelistRepository(
    private val database: VoiceOSDatabase
) : IWebAppWhitelistRepository {

    private val queries = database.webAppWhitelistQueries

    override suspend fun insertOrUpdate(webApp: WebAppWhitelistDTO): Long = withContext(Dispatchers.Default) {
        queries.insertOrUpdate(
            domain_id = webApp.domainId,
            display_name = webApp.displayName,
            base_url = webApp.baseUrl,
            category = webApp.category,
            is_enabled = if (webApp.isEnabled) 1L else 0L,
            auto_scan = if (webApp.autoScan) 1L else 0L,
            save_commands = if (webApp.saveCommands) 1L else 0L,
            command_count = webApp.commandCount.toLong(),
            last_visited = webApp.lastVisited,
            visit_count = webApp.visitCount.toLong(),
            created_at = webApp.createdAt,
            updated_at = webApp.updatedAt
        )
        // Return ID by looking up the inserted/updated row
        queries.selectByDomain(webApp.domainId).executeAsOneOrNull()?.id ?: 0L
    }

    override suspend fun insert(
        domainId: String,
        displayName: String,
        baseUrl: String?,
        category: String?,
        createdAt: Long,
        updatedAt: Long
    ): Long = withContext(Dispatchers.Default) {
        queries.insert(
            domain_id = domainId,
            display_name = displayName,
            base_url = baseUrl,
            category = category,
            created_at = createdAt,
            updated_at = updatedAt
        )
        queries.selectByDomain(domainId).executeAsOneOrNull()?.id ?: 0L
    }

    override suspend fun getAll(): List<WebAppWhitelistDTO> = withContext(Dispatchers.Default) {
        queries.selectAll().executeAsList().map { it.toWebAppWhitelistDTO() }
    }

    override suspend fun getEnabled(): List<WebAppWhitelistDTO> = withContext(Dispatchers.Default) {
        queries.selectEnabled().executeAsList().map { it.toWebAppWhitelistDTO() }
    }

    override suspend fun getByCategory(category: String): List<WebAppWhitelistDTO> = withContext(Dispatchers.Default) {
        queries.selectByCategory(category).executeAsList().map { it.toWebAppWhitelistDTO() }
    }

    override suspend fun getByDomain(domainId: String): WebAppWhitelistDTO? = withContext(Dispatchers.Default) {
        queries.selectByDomain(domainId).executeAsOneOrNull()?.toWebAppWhitelistDTO()
    }

    override suspend fun isWhitelisted(domainId: String): Boolean = withContext(Dispatchers.Default) {
        queries.isWhitelisted(domainId).executeAsOne()
    }

    override suspend fun getMostVisited(limit: Int): List<WebAppWhitelistDTO> = withContext(Dispatchers.Default) {
        queries.selectMostVisited(limit.toLong()).executeAsList().map { it.toWebAppWhitelistDTO() }
    }

    override suspend fun updateSettings(
        domainId: String,
        isEnabled: Boolean,
        autoScan: Boolean,
        saveCommands: Boolean,
        updatedAt: Long
    ) = withContext(Dispatchers.Default) {
        queries.updateSettings(
            is_enabled = if (isEnabled) 1L else 0L,
            auto_scan = if (autoScan) 1L else 0L,
            save_commands = if (saveCommands) 1L else 0L,
            updated_at = updatedAt,
            domain_id = domainId
        )
    }

    override suspend fun updateDisplayName(domainId: String, displayName: String, updatedAt: Long) = withContext(Dispatchers.Default) {
        queries.updateDisplayName(displayName, updatedAt, domainId)
    }

    override suspend fun updateCategory(domainId: String, category: String, updatedAt: Long) = withContext(Dispatchers.Default) {
        queries.updateCategory(category, updatedAt, domainId)
    }

    override suspend fun recordVisit(domainId: String, visitedAt: Long) = withContext(Dispatchers.Default) {
        queries.updateVisit(visitedAt, visitedAt, domainId)
    }

    override suspend fun updateCommandCount(domainId: String, count: Int, updatedAt: Long) = withContext(Dispatchers.Default) {
        queries.updateCommandCount(count.toLong(), updatedAt, domainId)
    }

    override suspend fun incrementCommandCount(domainId: String, updatedAt: Long) = withContext(Dispatchers.Default) {
        queries.incrementCommandCount(updatedAt, domainId)
    }

    override suspend fun deleteByDomain(domainId: String) = withContext(Dispatchers.Default) {
        queries.deleteByDomain(domainId)
    }

    override suspend fun deleteInactive(olderThan: Long) = withContext(Dispatchers.Default) {
        queries.deleteDisabled(olderThan)
    }
}
