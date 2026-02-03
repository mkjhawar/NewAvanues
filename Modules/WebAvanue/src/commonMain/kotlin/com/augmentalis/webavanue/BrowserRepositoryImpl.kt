package com.augmentalis.webavanue

import io.github.aakira.napier.Napier
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * BrowserRepositoryImpl - Facade implementation of BrowserRepository
 *
 * ## Overview
 * This class serves as a facade that delegates to domain-specific repositories:
 * - [TabRepository] - Tab operations
 * - [FavoriteRepository] - Bookmark operations
 * - [HistoryRepository] - Browsing history operations
 * - [DownloadRepository] - Download management
 * - [SettingsRepository] - Browser settings
 * - [SessionRepository] - Session restore/crash recovery
 * - [SitePermissionRepository] - Site permissions
 *
 * ## Architecture Benefits
 * - **Single Responsibility**: Each repository handles one domain
 * - **Testability**: Individual repositories can be tested in isolation
 * - **Maintainability**: Smaller, focused classes (~150-200 lines each)
 * - **Backward Compatibility**: External API unchanged
 *
 * ## Threading Model
 * - **Database Operations**: Dispatchers.IO (all suspend functions)
 * - **Flow Updates**: Dispatchers.Main (UI-safe StateFlow updates)
 * - **Initialization**: Background (SupervisorJob + IO dispatcher)
 *
 * @param database SQLDelight database instance
 * @see BrowserRepository
 */
class BrowserRepositoryImpl(
    private val database: BrowserDatabase
) : BrowserRepository {

    // Domain-specific repositories
    private val tabRepository: TabRepository = TabRepositoryImpl(database)
    private val favoriteRepository: FavoriteRepository = FavoriteRepositoryImpl(database)
    private val historyRepository: HistoryRepository = HistoryRepositoryImpl(database)
    private val downloadRepository: DownloadRepository = DownloadRepositoryImpl(database)
    private val settingsRepository: SettingsRepository = SettingsRepositoryImpl(database)
    private val sessionRepository: SessionRepository = SessionRepositoryImpl(database)
    private val sitePermissionRepository: SitePermissionRepository = SitePermissionRepositoryImpl(database)

    private val queries = database.browserDatabaseQueries

    // Async initialization scope
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        initScope.launch {
            try {
                val startTime = Clock.System.now().toEpochMilliseconds()

                // PRIORITY 1: Load recent tabs (fast startup)
                val recentTabs = queries.selectRecentTabs(10).executeAsList().map { it.toDomainModel() }

                // PRIORITY 2: Load settings (needed for UI)
                queries.insertDefaultSettings()
                val dbSettings = queries.selectSettings().executeAsOneOrNull()
                val settings = dbSettings?.toDomainModel() ?: BrowserSettings.default()

                // Update StateFlows on Main thread
                withContext(Dispatchers.Main) {
                    tabRepository.updateState(recentTabs)
                    settingsRepository.updateState(settings)
                }

                val initTime = Clock.System.now().toEpochMilliseconds() - startTime
                Napier.i("Fast startup: Loaded ${recentTabs.size} recent tabs in ${initTime}ms", tag = TAG)

                // DEFERRED: Load remaining data in background
                launch { loadAllTabs() }
                launch { loadFavorites() }
                launch { loadHistoryAndDownloads() }
            } catch (e: Exception) {
                Napier.e("Error loading initial data: ${e.message}", e, tag = TAG)
            }
        }
    }

    private suspend fun loadAllTabs() {
        try {
            val allTabs = queries.selectAllTabs().executeAsList().map { it.toDomainModel() }
            withContext(Dispatchers.Main) {
                tabRepository.updateState(allTabs)
            }
            Napier.i("Background: Loaded ${allTabs.size} total tabs", tag = TAG)
        } catch (e: Exception) {
            Napier.e("Error loading all tabs: ${e.message}", e, tag = TAG)
        }
    }

    private suspend fun loadFavorites() {
        try {
            val favorites = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
            withContext(Dispatchers.Main) {
                favoriteRepository.updateState(favorites)
            }
            Napier.i("Background: Loaded ${favorites.size} favorites", tag = TAG)
        } catch (e: Exception) {
            Napier.e("Error loading favorites: ${e.message}", e, tag = TAG)
        }
    }

    private suspend fun loadHistoryAndDownloads() {
        try {
            val history = queries.selectAllHistory(100, 0).executeAsList().map { it.toDomainModel() }
            val downloads = queries.selectAllDownloads(100, 0).executeAsList().map { it.toDomainModel() }
            withContext(Dispatchers.Main) {
                historyRepository.updateState(history)
                downloadRepository.updateState(downloads)
            }
            Napier.i("Background: Loaded ${history.size} history entries, ${downloads.size} downloads", tag = TAG)
        } catch (e: Exception) {
            Napier.e("Error loading history/downloads: ${e.message}", e, tag = TAG)
        }
    }

    // ==================== Tab Operations (delegated) ====================

    override suspend fun createTab(tab: Tab): Result<Tab> = tabRepository.createTab(tab)
    override suspend fun getTab(tabId: String): Result<Tab?> = tabRepository.getTab(tabId)
    override suspend fun getAllTabs(): Result<List<Tab>> = tabRepository.getAllTabs()
    override suspend fun getRecentTabs(limit: Int): Result<List<Tab>> = tabRepository.getRecentTabs(limit)
    override fun observeTabs(): Flow<List<Tab>> = tabRepository.observeTabs()
    override suspend fun updateTab(tab: Tab): Result<Unit> = tabRepository.updateTab(tab)
    override suspend fun closeTab(tabId: String): Result<Unit> = tabRepository.closeTab(tabId)
    override suspend fun closeTabs(tabIds: List<String>): Result<Unit> = tabRepository.closeTabs(tabIds)
    override suspend fun closeAllTabs(): Result<Unit> = tabRepository.closeAllTabs()
    override suspend fun setActiveTab(tabId: String): Result<Unit> = tabRepository.setActiveTab(tabId)
    override suspend fun setPinned(tabId: String, isPinned: Boolean): Result<Unit> = tabRepository.setPinned(tabId, isPinned)
    override suspend fun reorderTabs(tabIds: List<String>): Result<Unit> = tabRepository.reorderTabs(tabIds)

    // ==================== Favorite Operations (delegated) ====================

    override suspend fun addFavorite(favorite: Favorite): Result<Favorite> = favoriteRepository.addFavorite(favorite)
    override suspend fun getFavorite(favoriteId: String): Result<Favorite?> = favoriteRepository.getFavorite(favoriteId)
    override suspend fun getAllFavorites(): Result<List<Favorite>> = favoriteRepository.getAllFavorites()
    override suspend fun getFavoritesInFolder(folderId: String?): Result<List<Favorite>> = favoriteRepository.getFavoritesInFolder(folderId)
    override fun observeFavorites(): Flow<List<Favorite>> = favoriteRepository.observeFavorites()
    override suspend fun updateFavorite(favorite: Favorite): Result<Unit> = favoriteRepository.updateFavorite(favorite)
    override suspend fun removeFavorite(favoriteId: String): Result<Unit> = favoriteRepository.removeFavorite(favoriteId)
    override suspend fun removeFavorites(favoriteIds: List<String>): Result<Unit> = favoriteRepository.removeFavorites(favoriteIds)
    override suspend fun isFavorite(url: String): Result<Boolean> = favoriteRepository.isFavorite(url)
    override suspend fun searchFavorites(query: String): Result<List<Favorite>> = favoriteRepository.searchFavorites(query)
    override suspend fun createFolder(folder: FavoriteFolder): Result<FavoriteFolder> = favoriteRepository.createFolder(folder)
    override suspend fun getAllFolders(): Result<List<FavoriteFolder>> = favoriteRepository.getAllFolders()
    override suspend fun deleteFolder(folderId: String, deleteContents: Boolean): Result<Unit> = favoriteRepository.deleteFolder(folderId, deleteContents)

    // ==================== History Operations (delegated) ====================

    override suspend fun addHistoryEntry(entry: HistoryEntry): Result<HistoryEntry> = historyRepository.addHistoryEntry(entry)
    override suspend fun getHistory(limit: Int, offset: Int): Result<List<HistoryEntry>> = historyRepository.getHistory(limit, offset)
    override suspend fun getHistoryByDateRange(startDate: Instant, endDate: Instant): Result<List<HistoryEntry>> = historyRepository.getHistoryByDateRange(startDate, endDate)
    override suspend fun searchHistory(query: String, limit: Int): Result<List<HistoryEntry>> = historyRepository.searchHistory(query, limit)
    override suspend fun getMostVisited(limit: Int): Result<List<HistoryEntry>> = historyRepository.getMostVisited(limit)
    override fun observeHistory(): Flow<List<HistoryEntry>> = historyRepository.observeHistory()
    override suspend fun deleteHistoryEntry(entryId: String): Result<Unit> = historyRepository.deleteHistoryEntry(entryId)
    override suspend fun deleteHistoryForUrl(url: String): Result<Unit> = historyRepository.deleteHistoryForUrl(url)
    override suspend fun clearHistoryByDateRange(startDate: Instant, endDate: Instant): Result<Unit> = historyRepository.clearHistoryByDateRange(startDate, endDate)
    override suspend fun clearAllHistory(): Result<Unit> = historyRepository.clearAllHistory()
    override suspend fun getHistorySessions(limit: Int): Result<List<HistorySession>> = historyRepository.getHistorySessions(limit)

    // ==================== Download Operations (delegated) ====================

    override fun observeDownloads(): Flow<List<Download>> = downloadRepository.observeDownloads()
    override suspend fun addDownload(download: Download): Result<Download> = downloadRepository.addDownload(download)
    override suspend fun getDownload(downloadId: String): Result<Download?> = downloadRepository.getDownload(downloadId)
    override suspend fun getDownloadByManagerId(managerId: Long): Download? = downloadRepository.getDownloadByManagerId(managerId)
    override suspend fun getAllDownloads(limit: Int, offset: Int): Result<List<Download>> = downloadRepository.getAllDownloads(limit, offset)
    override suspend fun getDownloadsByStatus(status: DownloadStatus): Result<List<Download>> = downloadRepository.getDownloadsByStatus(status)
    override suspend fun updateDownload(download: Download): Result<Unit> = downloadRepository.updateDownload(download)
    override suspend fun updateDownloadProgress(downloadId: String, downloadedSize: Long, status: DownloadStatus): Result<Unit> = downloadRepository.updateDownloadProgress(downloadId, downloadedSize, status)
    override suspend fun setDownloadManagerId(downloadId: String, managerId: Long): Result<Unit> = downloadRepository.setDownloadManagerId(downloadId, managerId)
    override suspend fun completeDownload(downloadId: String, filepath: String, downloadedSize: Long): Result<Unit> = downloadRepository.completeDownload(downloadId, filepath, downloadedSize)
    override suspend fun failDownload(downloadId: String, errorMessage: String): Result<Unit> = downloadRepository.failDownload(downloadId, errorMessage)
    override suspend fun deleteDownload(downloadId: String): Result<Unit> = downloadRepository.deleteDownload(downloadId)
    override suspend fun clearAllDownloads(): Result<Unit> = downloadRepository.clearAllDownloads()
    override suspend fun searchDownloads(query: String): Result<List<Download>> = downloadRepository.searchDownloads(query)

    // ==================== Settings Operations (delegated) ====================

    override suspend fun getSettings(): Result<BrowserSettings> = settingsRepository.getSettings()
    override fun observeSettings(): Flow<BrowserSettings> = settingsRepository.observeSettings()
    override suspend fun updateSettings(settings: BrowserSettings): Result<Unit> = settingsRepository.updateSettings(settings)
    override suspend fun <T> updateSetting(key: String, value: T): Result<Unit> = settingsRepository.updateSetting(key, value)
    override suspend fun resetSettings(): Result<Unit> = settingsRepository.resetSettings()
    override suspend fun applyPreset(preset: SettingsPreset): Result<Unit> = settingsRepository.applyPreset(preset)

    // ==================== Site Permission Operations (delegated) ====================

    override suspend fun getSitePermission(domain: String, permissionType: String): Result<SitePermission?> = sitePermissionRepository.getSitePermission(domain, permissionType)
    override suspend fun insertSitePermission(domain: String, permissionType: String, granted: Boolean): Result<Unit> = sitePermissionRepository.insertSitePermission(domain, permissionType, granted)
    override suspend fun deleteSitePermission(domain: String, permissionType: String): Result<Unit> = sitePermissionRepository.deleteSitePermission(domain, permissionType)
    override suspend fun deleteAllSitePermissions(domain: String): Result<Unit> = sitePermissionRepository.deleteAllSitePermissions(domain)
    override suspend fun getAllSitePermissions(): Result<List<SitePermission>> = sitePermissionRepository.getAllSitePermissions()

    // ==================== Session Operations (delegated) ====================

    override suspend fun saveSession(session: Session, tabs: List<SessionTab>): Result<Unit> = sessionRepository.saveSession(session, tabs)
    override suspend fun getSession(sessionId: String): Result<Session?> = sessionRepository.getSession(sessionId)
    override suspend fun getLatestSession(): Result<Session?> = sessionRepository.getLatestSession()
    override suspend fun getLatestCrashSession(): Result<Session?> = sessionRepository.getLatestCrashSession()
    override suspend fun getAllSessions(limit: Int, offset: Int): Result<List<Session>> = sessionRepository.getAllSessions(limit, offset)
    override suspend fun getSessionTabs(sessionId: String): Result<List<SessionTab>> = sessionRepository.getSessionTabs(sessionId)
    override suspend fun getActiveSessionTab(sessionId: String): Result<SessionTab?> = sessionRepository.getActiveSessionTab(sessionId)
    override suspend fun deleteSession(sessionId: String): Result<Unit> = sessionRepository.deleteSession(sessionId)
    override suspend fun deleteAllSessions(): Result<Unit> = sessionRepository.deleteAllSessions()
    override suspend fun deleteOldSessions(timestamp: Instant): Result<Unit> = sessionRepository.deleteOldSessions(timestamp)

    // ==================== Data Management ====================

    override suspend fun exportData(): Result<BrowserData> = withContext(Dispatchers.IO) {
        try {
            val tabs = queries.selectAllTabs().executeAsList().map { it.toDomainModel() }
            val favorites = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
            val history = queries.selectAllHistory(1000, 0).executeAsList().map { it.toDomainModel() }
            val settings = queries.selectSettings().executeAsOneOrNull()?.toDomainModel() ?: BrowserSettings.default()

            Result.success(BrowserData(
                tabs = tabs,
                favorites = favorites,
                folders = emptyList(),
                history = history,
                settings = settings,
                exportedAt = Clock.System.now(),
                version = "1.0.0"
            ))
        } catch (e: Exception) {
            Napier.e("Error exporting data: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun importData(data: BrowserData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionWithResult {
                data.tabs.forEach { queries.insertTab(it.toDbModel()) }
                data.favorites.forEach { queries.insertFavorite(it.toDbModel()) }
                data.history.forEach { queries.insertHistoryEntry(it.toDbModel()) }
            }
            settingsRepository.updateSettings(data.settings)
            tabRepository.refresh()
            favoriteRepository.refresh()
            historyRepository.refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error importing data: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transaction {
                queries.deleteAllTabs()
                queries.deleteAllHistory()
                queries.selectAllFavorites().executeAsList().forEach { queries.deleteFavorite(it.id) }
            }
            settingsRepository.resetSettings()
            tabRepository.refresh()
            favoriteRepository.refresh()
            historyRepository.refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error clearing all data: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getDatabaseSize(): Result<Long> = withContext(Dispatchers.IO) {
        Result.success(0L)
    }

    override suspend fun optimizeDatabase(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.vacuum()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error optimizing database: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override fun cleanup() {
        try {
            initScope.coroutineContext[Job]?.cancel()
            Napier.d("initScope cancelled", tag = TAG)
        } catch (e: Exception) {
            Napier.e("Error in cleanup: ${e.message}", e, tag = TAG)
        }
    }

    companion object {
        private const val TAG = "BrowserRepository"
    }
}
