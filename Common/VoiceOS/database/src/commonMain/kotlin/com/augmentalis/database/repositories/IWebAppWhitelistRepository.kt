/**
 * IWebAppWhitelistRepository.kt - Repository interface for web app whitelist management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-12
 */

package com.augmentalis.database.repositories

import com.augmentalis.database.dto.WebAppWhitelistDTO

/**
 * Repository interface for web app whitelist.
 * Manages user-designated web apps for persistent voice command storage.
 */
interface IWebAppWhitelistRepository {

    /**
     * Insert or update a whitelisted web app.
     * @return The ID of the inserted/updated entry.
     */
    suspend fun insertOrUpdate(webApp: WebAppWhitelistDTO): Long

    /**
     * Insert a new web app to whitelist.
     * @return The ID of the inserted entry.
     */
    suspend fun insert(
        domainId: String,
        displayName: String,
        baseUrl: String?,
        category: String?,
        createdAt: Long,
        updatedAt: Long
    ): Long

    /**
     * Get all whitelisted web apps.
     */
    suspend fun getAll(): List<WebAppWhitelistDTO>

    /**
     * Get all enabled whitelisted web apps.
     */
    suspend fun getEnabled(): List<WebAppWhitelistDTO>

    /**
     * Get web apps by category.
     */
    suspend fun getByCategory(category: String): List<WebAppWhitelistDTO>

    /**
     * Get web app by domain ID.
     */
    suspend fun getByDomain(domainId: String): WebAppWhitelistDTO?

    /**
     * Check if a domain is whitelisted and command saving is enabled.
     */
    suspend fun isWhitelisted(domainId: String): Boolean

    /**
     * Get most visited web apps.
     */
    suspend fun getMostVisited(limit: Int): List<WebAppWhitelistDTO>

    /**
     * Update settings for a web app.
     */
    suspend fun updateSettings(
        domainId: String,
        isEnabled: Boolean,
        autoScan: Boolean,
        saveCommands: Boolean,
        updatedAt: Long
    )

    /**
     * Update display name.
     */
    suspend fun updateDisplayName(domainId: String, displayName: String, updatedAt: Long)

    /**
     * Update category.
     */
    suspend fun updateCategory(domainId: String, category: String, updatedAt: Long)

    /**
     * Record a visit to the web app.
     */
    suspend fun recordVisit(domainId: String, visitedAt: Long)

    /**
     * Update command count for the web app.
     */
    suspend fun updateCommandCount(domainId: String, count: Int, updatedAt: Long)

    /**
     * Increment command count by 1.
     */
    suspend fun incrementCommandCount(domainId: String, updatedAt: Long)

    /**
     * Delete web app from whitelist.
     */
    suspend fun deleteByDomain(domainId: String)

    /**
     * Delete disabled apps that haven't been visited recently.
     */
    suspend fun deleteInactive(olderThan: Long)
}
