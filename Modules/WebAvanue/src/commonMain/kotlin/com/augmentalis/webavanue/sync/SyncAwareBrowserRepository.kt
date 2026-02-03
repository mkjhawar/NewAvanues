package com.augmentalis.webavanue.sync

import com.augmentalis.webavanue.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Instant
import io.github.aakira.napier.Napier

/**
 * Sync-aware browser repository that wraps the base repository
 * and adds WebSocket synchronization capabilities.
 *
 * This repository:
 * - Delegates all operations to the underlying repository
 * - Sends changes to the sync server when connected
 * - Applies remote changes from the server
 * - Handles conflict resolution
 */
class SyncAwareBrowserRepository(
    private val delegate: BrowserRepository,
    private val syncService: WebSocketSyncService,
    private val conflictResolver: ConflictResolver = ConflictResolver()
) : BrowserRepository {

    private val TAG = "SyncAwareRepo"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        // Listen for remote updates and apply them
        scope.launch {
            syncService.remoteUpdates.collect { event ->
                handleRemoteUpdate(event)
            }
        }
    }

    // ==================== Tab Operations ====================

    override suspend fun createTab(tab: Tab): Result<Tab> {
        val result = delegate.createTab(tab)
        result.onSuccess { createdTab ->
            syncService.sendTabUpdate(createdTab, SyncOperation.CREATE)
        }
        return result
    }

    override suspend fun getTab(tabId: String): Result<Tab?> = delegate.getTab(tabId)

    override suspend fun getAllTabs(): Result<List<Tab>> = delegate.getAllTabs()

    override suspend fun getRecentTabs(limit: Int): Result<List<Tab>> = delegate.getRecentTabs(limit)

    override fun observeTabs(): Flow<List<Tab>> = delegate.observeTabs()

    override suspend fun updateTab(tab: Tab): Result<Unit> {
        val result = delegate.updateTab(tab)
        result.onSuccess {
            syncService.sendTabUpdate(tab, SyncOperation.UPDATE)
        }
        return result
    }

    override suspend fun closeTab(tabId: String): Result<Unit> {
        // Get tab before closing for sync
        val tabResult = delegate.getTab(tabId)
        val result = delegate.closeTab(tabId)
        result.onSuccess {
            tabResult.getOrNull()?.let { tab ->
                syncService.sendTabUpdate(tab, SyncOperation.DELETE)
            }
        }
        return result
    }

    override suspend fun closeTabs(tabIds: List<String>): Result<Unit> {
        // Get tabs before closing for sync
        val tabsResult = delegate.getAllTabs()
        val result = delegate.closeTabs(tabIds)
        result.onSuccess {
            tabsResult.getOrNull()?.filter { it.id in tabIds }?.forEach { tab ->
                syncService.sendTabUpdate(tab, SyncOperation.DELETE)
            }
        }
        return result
    }

    override suspend fun closeAllTabs(): Result<Unit> {
        val tabsResult = delegate.getAllTabs()
        val result = delegate.closeAllTabs()
        result.onSuccess {
            tabsResult.getOrNull()?.forEach { tab ->
                syncService.sendTabUpdate(tab, SyncOperation.DELETE)
            }
        }
        return result
    }

    override suspend fun setActiveTab(tabId: String): Result<Unit> {
        val result = delegate.setActiveTab(tabId)
        result.onSuccess {
            delegate.getTab(tabId).getOrNull()?.let { tab ->
                syncService.sendTabUpdate(tab, SyncOperation.UPDATE)
            }
        }
        return result
    }

    override suspend fun setPinned(tabId: String, isPinned: Boolean): Result<Unit> {
        val result = delegate.setPinned(tabId, isPinned)
        result.onSuccess {
            delegate.getTab(tabId).getOrNull()?.let { tab ->
                syncService.sendTabUpdate(tab, SyncOperation.UPDATE)
            }
        }
        return result
    }

    override suspend fun reorderTabs(tabIds: List<String>): Result<Unit> {
        val result = delegate.reorderTabs(tabIds)
        result.onSuccess {
            // Sync all reordered tabs
            delegate.getAllTabs().getOrNull()?.filter { it.id in tabIds }?.forEach { tab ->
                syncService.sendTabUpdate(tab, SyncOperation.UPDATE)
            }
        }
        return result
    }

    // ==================== Favorite Operations ====================

    override suspend fun addFavorite(favorite: Favorite): Result<Favorite> {
        val result = delegate.addFavorite(favorite)
        result.onSuccess { addedFavorite ->
            syncService.sendFavoriteUpdate(addedFavorite, SyncOperation.CREATE)
        }
        return result
    }

    override suspend fun getFavorite(favoriteId: String): Result<Favorite?> = delegate.getFavorite(favoriteId)

    override suspend fun getAllFavorites(): Result<List<Favorite>> = delegate.getAllFavorites()

    override suspend fun getFavoritesInFolder(folderId: String?): Result<List<Favorite>> =
        delegate.getFavoritesInFolder(folderId)

    override fun observeFavorites(): Flow<List<Favorite>> = delegate.observeFavorites()

    override suspend fun updateFavorite(favorite: Favorite): Result<Unit> {
        val result = delegate.updateFavorite(favorite)
        result.onSuccess {
            syncService.sendFavoriteUpdate(favorite, SyncOperation.UPDATE)
        }
        return result
    }

    override suspend fun removeFavorite(favoriteId: String): Result<Unit> {
        val favoriteResult = delegate.getFavorite(favoriteId)
        val result = delegate.removeFavorite(favoriteId)
        result.onSuccess {
            favoriteResult.getOrNull()?.let { favorite ->
                syncService.sendFavoriteUpdate(favorite, SyncOperation.DELETE)
            }
        }
        return result
    }

    override suspend fun removeFavorites(favoriteIds: List<String>): Result<Unit> {
        val favoritesResult = delegate.getAllFavorites()
        val result = delegate.removeFavorites(favoriteIds)
        result.onSuccess {
            favoritesResult.getOrNull()?.filter { it.id in favoriteIds }?.forEach { favorite ->
                syncService.sendFavoriteUpdate(favorite, SyncOperation.DELETE)
            }
        }
        return result
    }

    override suspend fun isFavorite(url: String): Result<Boolean> = delegate.isFavorite(url)

    override suspend fun searchFavorites(query: String): Result<List<Favorite>> = delegate.searchFavorites(query)

    override suspend fun createFolder(folder: FavoriteFolder): Result<FavoriteFolder> =
        delegate.createFolder(folder)

    override suspend fun getAllFolders(): Result<List<FavoriteFolder>> = delegate.getAllFolders()

    override suspend fun deleteFolder(folderId: String, deleteContents: Boolean): Result<Unit> =
        delegate.deleteFolder(folderId, deleteContents)

    // ==================== History Operations ====================

    override suspend fun addHistoryEntry(entry: HistoryEntry): Result<HistoryEntry> {
        val result = delegate.addHistoryEntry(entry)
        result.onSuccess { addedEntry ->
            // Don't sync incognito history
            if (!addedEntry.isIncognito) {
                syncService.sendHistoryUpdate(addedEntry, SyncOperation.CREATE)
            }
        }
        return result
    }

    override suspend fun getHistory(limit: Int, offset: Int): Result<List<HistoryEntry>> =
        delegate.getHistory(limit, offset)

    override suspend fun getHistoryByDateRange(startDate: Instant, endDate: Instant): Result<List<HistoryEntry>> =
        delegate.getHistoryByDateRange(startDate, endDate)

    override suspend fun searchHistory(query: String, limit: Int): Result<List<HistoryEntry>> =
        delegate.searchHistory(query, limit)

    override suspend fun getMostVisited(limit: Int): Result<List<HistoryEntry>> = delegate.getMostVisited(limit)

    override fun observeHistory(): Flow<List<HistoryEntry>> = delegate.observeHistory()

    override suspend fun deleteHistoryEntry(entryId: String): Result<Unit> {
        val entryResult = delegate.getHistory(1000, 0) // Find the entry
        val result = delegate.deleteHistoryEntry(entryId)
        result.onSuccess {
            entryResult.getOrNull()?.find { it.id == entryId }?.let { entry ->
                if (!entry.isIncognito) {
                    syncService.sendHistoryUpdate(entry, SyncOperation.DELETE)
                }
            }
        }
        return result
    }

    override suspend fun deleteHistoryForUrl(url: String): Result<Unit> = delegate.deleteHistoryForUrl(url)

    override suspend fun clearHistoryByDateRange(startDate: Instant, endDate: Instant): Result<Unit> =
        delegate.clearHistoryByDateRange(startDate, endDate)

    override suspend fun clearAllHistory(): Result<Unit> = delegate.clearAllHistory()

    override suspend fun getHistorySessions(limit: Int): Result<List<HistorySession>> =
        delegate.getHistorySessions(limit)

    // ==================== Download Operations ====================

    override fun observeDownloads(): Flow<List<Download>> = delegate.observeDownloads()

    override suspend fun addDownload(download: Download): Result<Download> {
        val result = delegate.addDownload(download)
        result.onSuccess { addedDownload ->
            syncService.sendDownloadUpdate(addedDownload, SyncOperation.CREATE)
        }
        return result
    }

    override suspend fun getDownload(downloadId: String): Result<Download?> = delegate.getDownload(downloadId)

    override suspend fun getDownloadByManagerId(managerId: Long): Download? = delegate.getDownloadByManagerId(managerId)

    override suspend fun getAllDownloads(limit: Int, offset: Int): Result<List<Download>> =
        delegate.getAllDownloads(limit, offset)

    override suspend fun getDownloadsByStatus(status: DownloadStatus): Result<List<Download>> =
        delegate.getDownloadsByStatus(status)

    override suspend fun updateDownload(download: Download): Result<Unit> {
        val result = delegate.updateDownload(download)
        result.onSuccess {
            syncService.sendDownloadUpdate(download, SyncOperation.UPDATE)
        }
        return result
    }

    override suspend fun updateDownloadProgress(
        downloadId: String,
        downloadedSize: Long,
        status: DownloadStatus
    ): Result<Unit> = delegate.updateDownloadProgress(downloadId, downloadedSize, status)

    override suspend fun setDownloadManagerId(downloadId: String, managerId: Long): Result<Unit> =
        delegate.setDownloadManagerId(downloadId, managerId)

    override suspend fun completeDownload(downloadId: String, filepath: String, downloadedSize: Long): Result<Unit> {
        val result = delegate.completeDownload(downloadId, filepath, downloadedSize)
        result.onSuccess {
            delegate.getDownload(downloadId).getOrNull()?.let { download ->
                syncService.sendDownloadUpdate(download, SyncOperation.UPDATE)
            }
        }
        return result
    }

    override suspend fun failDownload(downloadId: String, errorMessage: String): Result<Unit> =
        delegate.failDownload(downloadId, errorMessage)

    override suspend fun deleteDownload(downloadId: String): Result<Unit> {
        val downloadResult = delegate.getDownload(downloadId)
        val result = delegate.deleteDownload(downloadId)
        result.onSuccess {
            downloadResult.getOrNull()?.let { download ->
                syncService.sendDownloadUpdate(download, SyncOperation.DELETE)
            }
        }
        return result
    }

    override suspend fun clearAllDownloads(): Result<Unit> = delegate.clearAllDownloads()

    override suspend fun searchDownloads(query: String): Result<List<Download>> = delegate.searchDownloads(query)

    // ==================== Settings Operations ====================

    override suspend fun getSettings(): Result<BrowserSettings> = delegate.getSettings()

    override fun observeSettings(): Flow<BrowserSettings> = delegate.observeSettings()

    override suspend fun updateSettings(settings: BrowserSettings): Result<Unit> {
        val result = delegate.updateSettings(settings)
        result.onSuccess {
            syncService.sendSettingsUpdate(settings)
        }
        return result
    }

    override suspend fun <T> updateSetting(key: String, value: T): Result<Unit> {
        val result = delegate.updateSetting(key, value)
        result.onSuccess {
            delegate.getSettings().getOrNull()?.let { settings ->
                syncService.sendSettingsUpdate(settings)
            }
        }
        return result
    }

    override suspend fun resetSettings(): Result<Unit> {
        val result = delegate.resetSettings()
        result.onSuccess {
            delegate.getSettings().getOrNull()?.let { settings ->
                syncService.sendSettingsUpdate(settings)
            }
        }
        return result
    }

    override suspend fun applyPreset(preset: SettingsPreset): Result<Unit> {
        val result = delegate.applyPreset(preset)
        result.onSuccess {
            delegate.getSettings().getOrNull()?.let { settings ->
                syncService.sendSettingsUpdate(settings)
            }
        }
        return result
    }

    // ==================== Site Permission Operations ====================

    override suspend fun getSitePermission(domain: String, permissionType: String): Result<SitePermission?> =
        delegate.getSitePermission(domain, permissionType)

    override suspend fun insertSitePermission(domain: String, permissionType: String, granted: Boolean): Result<Unit> =
        delegate.insertSitePermission(domain, permissionType, granted)

    override suspend fun deleteSitePermission(domain: String, permissionType: String): Result<Unit> =
        delegate.deleteSitePermission(domain, permissionType)

    override suspend fun deleteAllSitePermissions(domain: String): Result<Unit> =
        delegate.deleteAllSitePermissions(domain)

    override suspend fun getAllSitePermissions(): Result<List<SitePermission>> = delegate.getAllSitePermissions()

    // ==================== Session Operations ====================

    override suspend fun saveSession(session: Session, tabs: List<SessionTab>): Result<Unit> {
        val result = delegate.saveSession(session, tabs)
        result.onSuccess {
            syncService.sendSessionUpdate(session, tabs, SyncOperation.CREATE)
        }
        return result
    }

    override suspend fun getSession(sessionId: String): Result<Session?> = delegate.getSession(sessionId)

    override suspend fun getLatestSession(): Result<Session?> = delegate.getLatestSession()

    override suspend fun getLatestCrashSession(): Result<Session?> = delegate.getLatestCrashSession()

    override suspend fun getAllSessions(limit: Int, offset: Int): Result<List<Session>> =
        delegate.getAllSessions(limit, offset)

    override suspend fun getSessionTabs(sessionId: String): Result<List<SessionTab>> =
        delegate.getSessionTabs(sessionId)

    override suspend fun getActiveSessionTab(sessionId: String): Result<SessionTab?> =
        delegate.getActiveSessionTab(sessionId)

    override suspend fun deleteSession(sessionId: String): Result<Unit> {
        val sessionResult = delegate.getSession(sessionId)
        val tabsResult = delegate.getSessionTabs(sessionId)
        val result = delegate.deleteSession(sessionId)
        result.onSuccess {
            sessionResult.getOrNull()?.let { session ->
                syncService.sendSessionUpdate(
                    session,
                    tabsResult.getOrNull() ?: emptyList(),
                    SyncOperation.DELETE
                )
            }
        }
        return result
    }

    override suspend fun deleteAllSessions(): Result<Unit> = delegate.deleteAllSessions()

    override suspend fun deleteOldSessions(timestamp: Instant): Result<Unit> =
        delegate.deleteOldSessions(timestamp)

    // ==================== Data Management ====================

    override suspend fun exportData(): Result<BrowserData> = delegate.exportData()

    override suspend fun importData(data: BrowserData): Result<Unit> = delegate.importData(data)

    override suspend fun clearAllData(): Result<Unit> = delegate.clearAllData()

    override suspend fun getDatabaseSize(): Result<Long> = delegate.getDatabaseSize()

    override suspend fun optimizeDatabase(): Result<Unit> = delegate.optimizeDatabase()

    override fun cleanup() {
        scope.cancel()
        delegate.cleanup()
    }

    // ==================== Remote Update Handling ====================

    private suspend fun handleRemoteUpdate(event: RemoteUpdateEvent) {
        try {
            when (event) {
                is RemoteTabUpdate -> handleRemoteTabUpdate(event)
                is RemoteFavoriteUpdate -> handleRemoteFavoriteUpdate(event)
                is RemoteHistoryUpdate -> handleRemoteHistoryUpdate(event)
                is RemoteDownloadUpdate -> handleRemoteDownloadUpdate(event)
                is RemoteSettingsUpdate -> handleRemoteSettingsUpdate(event)
                is RemoteSessionUpdate -> handleRemoteSessionUpdate(event)
            }
        } catch (e: Exception) {
            Napier.e("Failed to handle remote update: ${e.message}", e, tag = TAG)
        }
    }

    private suspend fun handleRemoteTabUpdate(event: RemoteTabUpdate) {
        when (event.operation) {
            SyncOperation.CREATE, SyncOperation.UPDATE -> {
                event.tab?.let { remoteTab ->
                    val localTab = delegate.getTab(remoteTab.id).getOrNull()
                    if (localTab != null) {
                        // Conflict resolution
                        val resolution = conflictResolver.resolveTabConflict(
                            local = localTab,
                            remote = remoteTab,
                            localTimestamp = localTab.lastAccessedAt,
                            remoteTimestamp = event.timestamp
                        )
                        when (resolution) {
                            is ConflictResult.Resolved -> delegate.updateTab(resolution.value)
                            is ConflictResult.ManualRequired -> {
                                // For now, prefer remote
                                delegate.updateTab(remoteTab)
                            }
                            is ConflictResult.Failed -> {
                                Napier.w("Tab conflict resolution failed: ${resolution.error}", tag = TAG)
                            }
                        }
                    } else {
                        delegate.createTab(remoteTab)
                    }
                }
            }
            SyncOperation.DELETE -> {
                event.tabId?.let { delegate.closeTab(it) }
            }
        }
    }

    private suspend fun handleRemoteFavoriteUpdate(event: RemoteFavoriteUpdate) {
        when (event.operation) {
            SyncOperation.CREATE, SyncOperation.UPDATE -> {
                event.favorite?.let { remoteFavorite ->
                    val localFavorite = delegate.getFavorite(remoteFavorite.id).getOrNull()
                    if (localFavorite != null) {
                        val resolution = conflictResolver.resolveFavoriteConflict(
                            local = localFavorite,
                            remote = remoteFavorite,
                            localTimestamp = localFavorite.createdAt,
                            remoteTimestamp = event.timestamp
                        )
                        when (resolution) {
                            is ConflictResult.Resolved -> delegate.updateFavorite(resolution.value)
                            else -> delegate.updateFavorite(remoteFavorite)
                        }
                    } else {
                        delegate.addFavorite(remoteFavorite)
                    }
                }
            }
            SyncOperation.DELETE -> {
                event.favoriteId?.let { delegate.removeFavorite(it) }
            }
        }
    }

    private suspend fun handleRemoteHistoryUpdate(event: RemoteHistoryUpdate) {
        when (event.operation) {
            SyncOperation.CREATE -> {
                event.historyEntry?.let { delegate.addHistoryEntry(it) }
            }
            SyncOperation.UPDATE -> {
                // History entries are typically not updated
            }
            SyncOperation.DELETE -> {
                event.entryId?.let { delegate.deleteHistoryEntry(it) }
            }
        }
    }

    private suspend fun handleRemoteDownloadUpdate(event: RemoteDownloadUpdate) {
        when (event.operation) {
            SyncOperation.CREATE, SyncOperation.UPDATE -> {
                event.download?.let { remoteDownload ->
                    val localDownload = delegate.getDownload(remoteDownload.id).getOrNull()
                    if (localDownload != null) {
                        val resolution = conflictResolver.resolveDownloadConflict(
                            local = localDownload,
                            remote = remoteDownload
                        )
                        when (resolution) {
                            is ConflictResult.Resolved -> delegate.updateDownload(resolution.value)
                            else -> delegate.updateDownload(remoteDownload)
                        }
                    } else {
                        delegate.addDownload(remoteDownload)
                    }
                }
            }
            SyncOperation.DELETE -> {
                event.downloadId?.let { delegate.deleteDownload(it) }
            }
        }
    }

    private suspend fun handleRemoteSettingsUpdate(event: RemoteSettingsUpdate) {
        val localSettings = delegate.getSettings().getOrNull() ?: return
        val resolution = conflictResolver.resolveSettingsConflict(
            local = localSettings,
            remote = event.settings,
            localTimestamp = kotlinx.datetime.Clock.System.now(), // Would need proper tracking
            remoteTimestamp = event.timestamp
        )
        when (resolution) {
            is ConflictResult.Resolved -> delegate.updateSettings(resolution.value)
            else -> delegate.updateSettings(event.settings)
        }
    }

    private suspend fun handleRemoteSessionUpdate(event: RemoteSessionUpdate) {
        when (event.operation) {
            SyncOperation.CREATE, SyncOperation.UPDATE -> {
                event.session?.let { session ->
                    event.tabs?.let { tabs ->
                        delegate.saveSession(session, tabs)
                    }
                }
            }
            SyncOperation.DELETE -> {
                event.sessionId?.let { delegate.deleteSession(it) }
            }
        }
    }
}

/**
 * Factory for creating sync-aware repository
 */
object SyncAwareBrowserRepositoryFactory {

    /**
     * Create a sync-aware repository wrapper
     *
     * @param delegate The underlying repository implementation
     * @param config WebSocket configuration
     * @return Sync-aware repository that sends changes to server
     */
    fun create(
        delegate: BrowserRepository,
        config: WebSocketConfig
    ): SyncAwareBrowserRepository {
        val syncService = WebSocketSyncServiceImpl()

        // Connect asynchronously
        CoroutineScope(Dispatchers.Default).launch {
            syncService.connect(config)
        }

        return SyncAwareBrowserRepository(
            delegate = delegate,
            syncService = syncService
        )
    }
}
