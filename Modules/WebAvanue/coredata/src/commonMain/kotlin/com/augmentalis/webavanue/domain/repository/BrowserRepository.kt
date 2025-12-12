package com.augmentalis.webavanue.domain.repository

import com.augmentalis.webavanue.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Main repository interface for WebAvanue browser data operations.
 * This interface defines all data operations for tabs, favorites, history, and settings.
 * Implementations will be platform-specific using SQLDelight.
 */
interface BrowserRepository {

    // ==================== Tab Operations ====================

    /**
     * Creates a new tab
     */
    suspend fun createTab(tab: Tab): Result<Tab>

    /**
     * Gets a tab by ID
     */
    suspend fun getTab(tabId: String): Result<Tab?>

    /**
     * Gets all open tabs
     */
    suspend fun getAllTabs(): Result<List<Tab>>

    /**
     * Gets recent tabs (optimized for startup)
     * PERFORMANCE: Loads only N most recently accessed tabs to reduce startup time
     */
    suspend fun getRecentTabs(limit: Int = 10): Result<List<Tab>>

    /**
     * Observes all tabs with real-time updates
     */
    fun observeTabs(): Flow<List<Tab>>

    /**
     * Updates an existing tab
     */
    suspend fun updateTab(tab: Tab): Result<Unit>

    /**
     * Closes a tab
     */
    suspend fun closeTab(tabId: String): Result<Unit>

    /**
     * Closes multiple tabs
     */
    suspend fun closeTabs(tabIds: List<String>): Result<Unit>

    /**
     * Closes all tabs
     */
    suspend fun closeAllTabs(): Result<Unit>

    /**
     * Sets the active tab
     */
    suspend fun setActiveTab(tabId: String): Result<Unit>

    /**
     * Pins or unpins a tab
     */
    suspend fun setPinned(tabId: String, isPinned: Boolean): Result<Unit>

    /**
     * Reorders tabs
     */
    suspend fun reorderTabs(tabIds: List<String>): Result<Unit>

    // ==================== Favorite Operations ====================

    /**
     * Adds a new favorite
     */
    suspend fun addFavorite(favorite: Favorite): Result<Favorite>

    /**
     * Gets a favorite by ID
     */
    suspend fun getFavorite(favoriteId: String): Result<Favorite?>

    /**
     * Gets all favorites
     */
    suspend fun getAllFavorites(): Result<List<Favorite>>

    /**
     * Gets favorites in a specific folder
     */
    suspend fun getFavoritesInFolder(folderId: String?): Result<List<Favorite>>

    /**
     * Observes all favorites with real-time updates
     */
    fun observeFavorites(): Flow<List<Favorite>>

    /**
     * Updates a favorite
     */
    suspend fun updateFavorite(favorite: Favorite): Result<Unit>

    /**
     * Removes a favorite
     */
    suspend fun removeFavorite(favoriteId: String): Result<Unit>

    /**
     * Removes multiple favorites
     */
    suspend fun removeFavorites(favoriteIds: List<String>): Result<Unit>

    /**
     * Checks if a URL is favorited
     */
    suspend fun isFavorite(url: String): Result<Boolean>

    /**
     * Searches favorites
     */
    suspend fun searchFavorites(query: String): Result<List<Favorite>>

    /**
     * Creates a favorite folder
     */
    suspend fun createFolder(folder: FavoriteFolder): Result<FavoriteFolder>

    /**
     * Gets all favorite folders
     */
    suspend fun getAllFolders(): Result<List<FavoriteFolder>>

    /**
     * Deletes a folder and optionally its contents
     */
    suspend fun deleteFolder(folderId: String, deleteContents: Boolean): Result<Unit>

    // ==================== History Operations ====================

    /**
     * Adds a history entry
     *
     * Note: If the entry is from an incognito/private tab (isIncognito = true),
     * the history entry should NOT be persisted to the database.
     * Implementations should check this flag and return success without storing.
     */
    suspend fun addHistoryEntry(entry: HistoryEntry): Result<HistoryEntry>

    /**
     * Gets history entries
     */
    suspend fun getHistory(limit: Int = 100, offset: Int = 0): Result<List<HistoryEntry>>

    /**
     * Gets history for a specific date range
     */
    suspend fun getHistoryByDateRange(
        startDate: Instant,
        endDate: Instant
    ): Result<List<HistoryEntry>>

    /**
     * Searches history
     */
    suspend fun searchHistory(query: String, limit: Int = 50): Result<List<HistoryEntry>>

    /**
     * Gets most visited sites
     */
    suspend fun getMostVisited(limit: Int = 10): Result<List<HistoryEntry>>

    /**
     * Observes history with real-time updates
     */
    fun observeHistory(): Flow<List<HistoryEntry>>

    /**
     * Deletes a specific history entry
     */
    suspend fun deleteHistoryEntry(entryId: String): Result<Unit>

    /**
     * Deletes history for a URL
     */
    suspend fun deleteHistoryForUrl(url: String): Result<Unit>

    /**
     * Clears history for a date range
     */
    suspend fun clearHistoryByDateRange(
        startDate: Instant,
        endDate: Instant
    ): Result<Unit>

    /**
     * Clears all history
     */
    suspend fun clearAllHistory(): Result<Unit>

    /**
     * Gets history sessions
     */
    suspend fun getHistorySessions(limit: Int = 10): Result<List<HistorySession>>

    // ==================== Download Operations ====================

    /**
     * Observes all downloads with real-time updates
     */
    fun observeDownloads(): Flow<List<Download>>

    /**
     * Adds a new download
     */
    suspend fun addDownload(download: Download): Result<Download>

    /**
     * Gets a download by ID
     */
    suspend fun getDownload(downloadId: String): Result<Download?>

    /**
     * Gets a download by Android DownloadManager ID
     */
    suspend fun getDownloadByManagerId(managerId: Long): Download?

    /**
     * Gets all downloads
     */
    suspend fun getAllDownloads(limit: Int = 100, offset: Int = 0): Result<List<Download>>

    /**
     * Gets downloads by status
     */
    suspend fun getDownloadsByStatus(status: DownloadStatus): Result<List<Download>>

    /**
     * Updates a download
     */
    suspend fun updateDownload(download: Download): Result<Unit>

    /**
     * Updates download progress
     */
    suspend fun updateDownloadProgress(
        downloadId: String,
        downloadedSize: Long,
        status: DownloadStatus
    ): Result<Unit>

    /**
     * Sets the Android DownloadManager ID for a download
     */
    suspend fun setDownloadManagerId(downloadId: String, managerId: Long): Result<Unit>

    /**
     * Marks a download as complete
     */
    suspend fun completeDownload(
        downloadId: String,
        filepath: String,
        downloadedSize: Long
    ): Result<Unit>

    /**
     * Marks a download as failed
     */
    suspend fun failDownload(downloadId: String, errorMessage: String): Result<Unit>

    /**
     * Deletes a download
     */
    suspend fun deleteDownload(downloadId: String): Result<Unit>

    /**
     * Clears all downloads
     */
    suspend fun clearAllDownloads(): Result<Unit>

    /**
     * Searches downloads by filename
     */
    suspend fun searchDownloads(query: String): Result<List<Download>>

    // ==================== Settings Operations ====================

    /**
     * Gets browser settings
     */
    suspend fun getSettings(): Result<BrowserSettings>

    /**
     * Observes settings changes
     */
    fun observeSettings(): Flow<BrowserSettings>

    /**
     * Updates browser settings
     */
    suspend fun updateSettings(settings: BrowserSettings): Result<Unit>

    /**
     * Updates a specific setting
     */
    suspend fun <T> updateSetting(key: String, value: T): Result<Unit>

    /**
     * Resets settings to default
     */
    suspend fun resetSettings(): Result<Unit>

    /**
     * Applies a preset configuration
     */
    suspend fun applyPreset(preset: SettingsPreset): Result<Unit>

    // ==================== Site Permission Operations ====================

    /**
     * Gets a specific permission for a domain
     */
    suspend fun getSitePermission(domain: String, permissionType: String): Result<SitePermission?>

    /**
     * Inserts or updates a site permission
     */
    suspend fun insertSitePermission(
        domain: String,
        permissionType: String,
        granted: Boolean
    ): Result<Unit>

    /**
     * Deletes a specific permission for a domain
     */
    suspend fun deleteSitePermission(domain: String, permissionType: String): Result<Unit>

    /**
     * Deletes all permissions for a domain
     */
    suspend fun deleteAllSitePermissions(domain: String): Result<Unit>

    /**
     * Gets all site permissions
     */
    suspend fun getAllSitePermissions(): Result<List<SitePermission>>

    // ==================== Session Operations ====================

    /**
     * Saves a browsing session with its tabs
     *
     * @param session Session metadata
     * @param tabs List of tabs in this session
     */
    suspend fun saveSession(session: Session, tabs: List<SessionTab>): Result<Unit>

    /**
     * Gets a specific session by ID
     *
     * @param sessionId Session ID
     * @return Session or null if not found
     */
    suspend fun getSession(sessionId: String): Result<Session?>

    /**
     * Gets the most recent session
     *
     * @return Latest session or null if no sessions exist
     */
    suspend fun getLatestSession(): Result<Session?>

    /**
     * Gets the most recent crash recovery session
     *
     * @return Latest crash session or null if none exists
     */
    suspend fun getLatestCrashSession(): Result<Session?>

    /**
     * Gets all sessions (for session history)
     *
     * @param limit Maximum number of sessions
     * @param offset Offset for pagination
     * @return List of sessions
     */
    suspend fun getAllSessions(limit: Int = 20, offset: Int = 0): Result<List<Session>>

    /**
     * Gets all tabs for a specific session
     *
     * @param sessionId Session ID
     * @return List of session tabs
     */
    suspend fun getSessionTabs(sessionId: String): Result<List<SessionTab>>

    /**
     * Gets the active tab for a session
     *
     * @param sessionId Session ID
     * @return Active session tab or null
     */
    suspend fun getActiveSessionTab(sessionId: String): Result<SessionTab?>

    /**
     * Deletes a specific session and its tabs
     *
     * @param sessionId Session ID to delete
     */
    suspend fun deleteSession(sessionId: String): Result<Unit>

    /**
     * Deletes all sessions
     */
    suspend fun deleteAllSessions(): Result<Unit>

    /**
     * Deletes sessions older than a specific timestamp
     *
     * @param timestamp Cutoff timestamp
     */
    suspend fun deleteOldSessions(timestamp: Instant): Result<Unit>

    // ==================== Data Management ====================

    /**
     * Exports all browser data
     */
    suspend fun exportData(): Result<BrowserData>

    /**
     * Imports browser data
     */
    suspend fun importData(data: BrowserData): Result<Unit>

    /**
     * Clears all browser data
     */
    suspend fun clearAllData(): Result<Unit>

    /**
     * Gets database size in bytes
     */
    suspend fun getDatabaseSize(): Result<Long>

    /**
     * Optimizes the database
     */
    suspend fun optimizeDatabase(): Result<Unit>

    /**
     * Cleanup repository resources (coroutine scopes, etc.)
     * FIX C7: Call this when repository is no longer needed
     */
    fun cleanup()
}

/**
 * Settings preset options
 */
enum class SettingsPreset {
    DEFAULT,
    PRIVACY,
    PERFORMANCE,
    ACCESSIBILITY
}

/**
 * Container for all browser data (for export/import)
 */
data class BrowserData(
    val tabs: List<Tab>,
    val favorites: List<Favorite>,
    val folders: List<FavoriteFolder>,
    val history: List<HistoryEntry>,
    val settings: BrowserSettings,
    val exportedAt: Instant,
    val version: String
)