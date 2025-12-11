package com.augmentalis.Avanues.web.universal

import com.augmentalis.webavanue.domain.model.*
import com.augmentalis.webavanue.domain.repository.BrowserData
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.webavanue.domain.repository.SettingsPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Instant

class FakeBrowserRepository : BrowserRepository {
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private val _settings = MutableStateFlow(BrowserSettings())
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())

    // Test helpers
    fun setTabs(tabs: List<Tab>) { _tabs.value = tabs }
    fun setFavorites(favorites: List<Favorite>) { _favorites.value = favorites }
    fun setHistory(history: List<HistoryEntry>) { _history.value = history }
    fun setSettings(settings: BrowserSettings) { _settings.value = settings }
    fun setDownloads(downloads: List<Download>) { _downloads.value = downloads }

    // Tab Operations
    override suspend fun createTab(tab: Tab): Result<Tab> {
        _tabs.value = _tabs.value + tab
        return Result.success(tab)
    }

    override suspend fun getTab(tabId: String): Result<Tab?> {
        return Result.success(_tabs.value.find { it.id == tabId })
    }

    override suspend fun getAllTabs(): Result<List<Tab>> {
        return Result.success(_tabs.value)
    }

    override fun observeTabs(): Flow<List<Tab>> = _tabs.asStateFlow()

    override suspend fun updateTab(tab: Tab): Result<Unit> {
        _tabs.value = _tabs.value.map { if (it.id == tab.id) tab else it }
        return Result.success(Unit)
    }

    override suspend fun closeTab(tabId: String): Result<Unit> {
        _tabs.value = _tabs.value.filterNot { it.id == tabId }
        return Result.success(Unit)
    }

    override suspend fun closeTabs(tabIds: List<String>): Result<Unit> {
        _tabs.value = _tabs.value.filterNot { it.id in tabIds }
        return Result.success(Unit)
    }

    override suspend fun closeAllTabs(): Result<Unit> {
        _tabs.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun setActiveTab(tabId: String): Result<Unit> {
        _tabs.value = _tabs.value.map { it.copy(isActive = it.id == tabId) }
        return Result.success(Unit)
    }

    override suspend fun setPinned(tabId: String, isPinned: Boolean): Result<Unit> {
        _tabs.value = _tabs.value.map { if (it.id == tabId) it.copy(isPinned = isPinned) else it }
        return Result.success(Unit)
    }

    override suspend fun reorderTabs(tabIds: List<String>): Result<Unit> {
        val reordered = tabIds.mapNotNull { id -> _tabs.value.find { it.id == id } }
        _tabs.value = reordered
        return Result.success(Unit)
    }

    // Favorite Operations
    override suspend fun addFavorite(favorite: Favorite): Result<Favorite> {
        _favorites.value = _favorites.value + favorite
        return Result.success(favorite)
    }

    override suspend fun getFavorite(favoriteId: String): Result<Favorite?> {
        return Result.success(_favorites.value.find { it.id == favoriteId })
    }

    override suspend fun getAllFavorites(): Result<List<Favorite>> {
        return Result.success(_favorites.value)
    }

    override suspend fun getFavoritesInFolder(folderId: String?): Result<List<Favorite>> {
        return Result.success(_favorites.value.filter { it.folderId == folderId })
    }

    override fun observeFavorites(): Flow<List<Favorite>> = _favorites.asStateFlow()

    override suspend fun updateFavorite(favorite: Favorite): Result<Unit> {
        _favorites.value = _favorites.value.map { if (it.id == favorite.id) favorite else it }
        return Result.success(Unit)
    }

    override suspend fun removeFavorite(favoriteId: String): Result<Unit> {
        _favorites.value = _favorites.value.filterNot { it.id == favoriteId }
        return Result.success(Unit)
    }

    override suspend fun removeFavorites(favoriteIds: List<String>): Result<Unit> {
        _favorites.value = _favorites.value.filterNot { it.id in favoriteIds }
        return Result.success(Unit)
    }

    override suspend fun isFavorite(url: String): Result<Boolean> {
        return Result.success(_favorites.value.any { it.url == url })
    }

    override suspend fun searchFavorites(query: String): Result<List<Favorite>> {
        return Result.success(_favorites.value.filter {
            it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        })
    }

    override suspend fun createFolder(folder: FavoriteFolder): Result<FavoriteFolder> {
        return Result.success(folder)
    }

    override suspend fun getAllFolders(): Result<List<FavoriteFolder>> {
        return Result.success(emptyList())
    }

    override suspend fun deleteFolder(folderId: String, deleteContents: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    // History Operations
    override suspend fun addHistoryEntry(entry: HistoryEntry): Result<HistoryEntry> {
        _history.value = _history.value + entry
        return Result.success(entry)
    }

    override suspend fun getHistory(limit: Int, offset: Int): Result<List<HistoryEntry>> {
        return Result.success(_history.value.drop(offset).take(limit))
    }

    override suspend fun getHistoryByDateRange(startDate: Instant, endDate: Instant): Result<List<HistoryEntry>> {
        return Result.success(_history.value.filter { it.visitedAt >= startDate && it.visitedAt <= endDate })
    }

    override suspend fun searchHistory(query: String, limit: Int): Result<List<HistoryEntry>> {
        return Result.success(_history.value.filter {
            it.title.contains(query, ignoreCase = true) || it.url.contains(query, ignoreCase = true)
        }.take(limit))
    }

    override suspend fun getMostVisited(limit: Int): Result<List<HistoryEntry>> {
        return Result.success(_history.value.sortedByDescending { it.visitCount }.take(limit))
    }

    override fun observeHistory(): Flow<List<HistoryEntry>> = _history.asStateFlow()

    override suspend fun deleteHistoryEntry(entryId: String): Result<Unit> {
        _history.value = _history.value.filterNot { it.id == entryId }
        return Result.success(Unit)
    }

    override suspend fun deleteHistoryForUrl(url: String): Result<Unit> {
        _history.value = _history.value.filterNot { it.url == url }
        return Result.success(Unit)
    }

    override suspend fun clearHistoryByDateRange(startDate: Instant, endDate: Instant): Result<Unit> {
        _history.value = _history.value.filterNot { it.visitedAt >= startDate && it.visitedAt <= endDate }
        return Result.success(Unit)
    }

    override suspend fun clearAllHistory(): Result<Unit> {
        _history.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun getHistorySessions(limit: Int): Result<List<HistorySession>> {
        return Result.success(emptyList())
    }

    // Settings Operations
    override suspend fun getSettings(): Result<BrowserSettings> {
        return Result.success(_settings.value)
    }

    override fun observeSettings(): Flow<BrowserSettings> = _settings.asStateFlow()

    override suspend fun updateSettings(settings: BrowserSettings): Result<Unit> {
        _settings.value = settings
        return Result.success(Unit)
    }

    override suspend fun <T> updateSetting(key: String, value: T): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun resetSettings(): Result<Unit> {
        _settings.value = BrowserSettings()
        return Result.success(Unit)
    }

    override suspend fun applyPreset(preset: SettingsPreset): Result<Unit> {
        return Result.success(Unit)
    }

    // Site Permission Operations
    override suspend fun getSitePermission(domain: String, permissionType: String): Result<SitePermission?> {
        return Result.success(null) // Always return null (no remembered permissions in tests)
    }

    override suspend fun insertSitePermission(
        domain: String,
        permissionType: String,
        granted: Boolean
    ): Result<Unit> {
        return Result.success(Unit) // No-op for tests
    }

    override suspend fun deleteSitePermission(domain: String, permissionType: String): Result<Unit> {
        return Result.success(Unit) // No-op for tests
    }

    override suspend fun deleteAllSitePermissions(domain: String): Result<Unit> {
        return Result.success(Unit) // No-op for tests
    }

    override suspend fun getAllSitePermissions(): Result<List<SitePermission>> {
        return Result.success(emptyList()) // No permissions for tests
    }

    // Data Management
    override suspend fun exportData(): Result<BrowserData> {
        return Result.success(
            BrowserData(
                tabs = _tabs.value,
                favorites = _favorites.value,
                folders = emptyList(),
                history = _history.value,
                settings = _settings.value,
                exportedAt = kotlinx.datetime.Clock.System.now(),
                version = "1.0"
            )
        )
    }

    override suspend fun importData(data: BrowserData): Result<Unit> {
        _tabs.value = data.tabs
        _favorites.value = data.favorites
        _history.value = data.history
        _settings.value = data.settings
        return Result.success(Unit)
    }

    override suspend fun clearAllData(): Result<Unit> {
        _tabs.value = emptyList()
        _favorites.value = emptyList()
        _history.value = emptyList()
        _settings.value = BrowserSettings()
        return Result.success(Unit)
    }

    override suspend fun getDatabaseSize(): Result<Long> {
        return Result.success(0L)
    }

    override suspend fun optimizeDatabase(): Result<Unit> {
        return Result.success(Unit)
    }

    // Download Operations
    override fun observeDownloads(): Flow<List<Download>> = _downloads.asStateFlow()

    override suspend fun addDownload(download: Download): Result<Download> {
        _downloads.value = _downloads.value + download
        return Result.success(download)
    }

    override suspend fun getDownload(downloadId: String): Result<Download?> {
        return Result.success(_downloads.value.find { it.id == downloadId })
    }

    override suspend fun getDownloadByManagerId(managerId: Long): Download? {
        return _downloads.value.find { it.downloadManagerId == managerId }
    }

    override suspend fun getAllDownloads(limit: Int, offset: Int): Result<List<Download>> {
        return Result.success(_downloads.value.drop(offset).take(limit))
    }

    override suspend fun getDownloadsByStatus(status: DownloadStatus): Result<List<Download>> {
        return Result.success(_downloads.value.filter { it.status == status })
    }

    override suspend fun updateDownload(download: Download): Result<Unit> {
        _downloads.value = _downloads.value.map { if (it.id == download.id) download else it }
        return Result.success(Unit)
    }

    override suspend fun updateDownloadProgress(
        downloadId: String,
        downloadedSize: Long,
        status: DownloadStatus
    ): Result<Unit> {
        _downloads.value = _downloads.value.map {
            if (it.id == downloadId) {
                it.copy(downloadedSize = downloadedSize, status = status)
            } else it
        }
        return Result.success(Unit)
    }

    override suspend fun setDownloadManagerId(downloadId: String, managerId: Long): Result<Unit> {
        _downloads.value = _downloads.value.map {
            if (it.id == downloadId) it.copy(downloadManagerId = managerId) else it
        }
        return Result.success(Unit)
    }

    override suspend fun completeDownload(
        downloadId: String,
        filepath: String,
        downloadedSize: Long
    ): Result<Unit> {
        _downloads.value = _downloads.value.map {
            if (it.id == downloadId) {
                it.copy(status = DownloadStatus.COMPLETED, filepath = filepath, downloadedSize = downloadedSize)
            } else it
        }
        return Result.success(Unit)
    }

    override suspend fun failDownload(downloadId: String, errorMessage: String): Result<Unit> {
        _downloads.value = _downloads.value.map {
            if (it.id == downloadId) {
                it.copy(status = DownloadStatus.FAILED, errorMessage = errorMessage)
            } else it
        }
        return Result.success(Unit)
    }

    override suspend fun deleteDownload(downloadId: String): Result<Unit> {
        _downloads.value = _downloads.value.filterNot { it.id == downloadId }
        return Result.success(Unit)
    }

    override suspend fun clearAllDownloads(): Result<Unit> {
        _downloads.value = emptyList()
        return Result.success(Unit)
    }

    override suspend fun searchDownloads(query: String): Result<List<Download>> {
        return Result.success(_downloads.value.filter {
            it.filename.contains(query, ignoreCase = true) ||
            it.url.contains(query, ignoreCase = true)
        })
    }

    // Cleanup
    override fun cleanup() {
        // No-op for test repository - no resources to cleanup
    }
}
