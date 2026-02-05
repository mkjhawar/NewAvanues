package com.augmentalis.browseravanue.domain.repository

import com.augmentalis.browseravanue.core.BrowserResult
import com.augmentalis.browseravanue.domain.model.BrowserSettings
import com.augmentalis.browseravanue.domain.model.Favorite
import com.augmentalis.browseravanue.domain.model.Tab
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for browser data operations
 *
 * Architecture:
 * - Interface in domain layer (Dependency Inversion Principle)
 * - Implementation in data layer
 * - Returns BrowserResult for type-safe error handling
 * - Returns Flow for reactive data streams
 * - Pure Kotlin (no Android dependencies)
 *
 * Usage by UseCases:
 * ```
 * class GetAllTabsUseCase(private val repository: BrowserRepository) {
 *     operator fun invoke(): Flow<BrowserResult<List<Tab>>> {
 *         return repository.observeAllTabs()
 *     }
 * }
 * ```
 */
interface BrowserRepository {

    // ==========================================
    // Tab Operations
    // ==========================================

    /**
     * Observe all tabs (reactive)
     */
    fun observeAllTabs(): Flow<BrowserResult<List<Tab>>>

    /**
     * Get all tabs (one-time query)
     */
    suspend fun getAllTabs(): BrowserResult<List<Tab>>

    /**
     * Observe specific tab by ID
     */
    fun observeTabById(tabId: String): Flow<BrowserResult<Tab>>

    /**
     * Get specific tab by ID
     */
    suspend fun getTabById(tabId: String): BrowserResult<Tab>

    /**
     * Get most recently accessed tab
     */
    suspend fun getMostRecentTab(): BrowserResult<Tab>

    /**
     * Get tab count
     */
    suspend fun getTabCount(): BrowserResult<Int>

    /**
     * Observe tab count
     */
    fun observeTabCount(): Flow<BrowserResult<Int>>

    /**
     * Search tabs by query
     */
    suspend fun searchTabs(query: String): BrowserResult<List<Tab>>

    /**
     * Create new tab
     */
    suspend fun createTab(url: String): BrowserResult<Tab>

    /**
     * Create new tab with search query
     */
    suspend fun createTabWithSearch(query: String): BrowserResult<Tab>

    /**
     * Update tab
     */
    suspend fun updateTab(tab: Tab): BrowserResult<Unit>

    /**
     * Delete tab
     */
    suspend fun deleteTab(tabId: String): BrowserResult<Unit>

    /**
     * Delete all tabs
     */
    suspend fun deleteAllTabs(): BrowserResult<Unit>

    /**
     * Mark tab as accessed (update last accessed timestamp)
     */
    suspend fun markTabAccessed(tabId: String): BrowserResult<Unit>

    // ==========================================
    // Favorite Operations
    // ==========================================

    /**
     * Observe all favorites (reactive)
     */
    fun observeAllFavorites(): Flow<BrowserResult<List<Favorite>>>

    /**
     * Get all favorites (one-time query)
     */
    suspend fun getAllFavorites(): BrowserResult<List<Favorite>>

    /**
     * Observe favorites by folder
     */
    fun observeFavoritesByFolder(folder: String?): Flow<BrowserResult<List<Favorite>>>

    /**
     * Observe all folders
     */
    fun observeFolders(): Flow<BrowserResult<List<String>>>

    /**
     * Get favorite by ID
     */
    suspend fun getFavoriteById(favoriteId: String): BrowserResult<Favorite>

    /**
     * Check if URL is favorited
     */
    suspend fun isFavorited(url: String): BrowserResult<Boolean>

    /**
     * Observe if URL is favorited
     */
    fun observeIsFavorited(url: String): Flow<BrowserResult<Boolean>>

    /**
     * Search favorites by query
     */
    suspend fun searchFavorites(query: String): BrowserResult<List<Favorite>>

    /**
     * Add favorite
     */
    suspend fun addFavorite(url: String, title: String): BrowserResult<Favorite>

    /**
     * Add favorite from tab
     */
    suspend fun addFavoriteFromTab(tab: Tab): BrowserResult<Favorite>

    /**
     * Update favorite
     */
    suspend fun updateFavorite(favorite: Favorite): BrowserResult<Unit>

    /**
     * Delete favorite
     */
    suspend fun deleteFavorite(favoriteId: String): BrowserResult<Unit>

    /**
     * Delete favorite by URL
     */
    suspend fun deleteFavoriteByUrl(url: String): BrowserResult<Unit>

    /**
     * Delete all favorites
     */
    suspend fun deleteAllFavorites(): BrowserResult<Unit>

    /**
     * Record visit to favorite (increment count, update last visited)
     */
    suspend fun recordFavoriteVisit(favoriteId: String): BrowserResult<Unit>

    // ==========================================
    // Navigation Operations
    // ==========================================

    /**
     * Navigate to URL in tab
     */
    suspend fun navigateToUrl(tabId: String, url: String): BrowserResult<Unit>

    /**
     * Go back in tab
     */
    suspend fun goBack(tabId: String): BrowserResult<Unit>

    /**
     * Go forward in tab
     */
    suspend fun goForward(tabId: String): BrowserResult<Unit>

    /**
     * Reload tab
     */
    suspend fun reload(tabId: String): BrowserResult<Unit>

    // ==========================================
    // Settings Operations
    // ==========================================

    /**
     * Observe browser settings (reactive)
     */
    fun observeSettings(): Flow<BrowserResult<BrowserSettings>>

    /**
     * Get browser settings (one-time query)
     */
    suspend fun getSettings(): BrowserResult<BrowserSettings>

    /**
     * Update settings
     */
    suspend fun updateSettings(settings: BrowserSettings): BrowserResult<Unit>

    /**
     * Reset settings to defaults
     */
    suspend fun resetSettings(): BrowserResult<Unit>
}
