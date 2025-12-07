package com.augmentalis.webavanue.data.repository

import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.domain.model.*
import com.augmentalis.webavanue.domain.repository.BrowserData
import com.augmentalis.webavanue.domain.repository.BrowserRepository
import com.augmentalis.webavanue.domain.repository.SettingsPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * SQLDelight implementation of BrowserRepository.
 * Provides cross-platform data persistence for WebAvanue browser.
 */
class BrowserRepositoryImpl(
    private val database: BrowserDatabase
) : BrowserRepository {

    private val queries = database.browserDatabaseQueries

    // In-memory state for reactive flows
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())
    private val _favorites = MutableStateFlow<List<Favorite>>(emptyList())
    private val _history = MutableStateFlow<List<HistoryEntry>>(emptyList())
    private val _downloads = MutableStateFlow<List<Download>>(emptyList())
    private val _settings = MutableStateFlow(BrowserSettings.default())

    // FIX P1-3: Async initialization scope to prevent blocking Main thread
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // FIX Issues #2 & #3: Initialize data from database on repository creation
    // FIX P1-3: Now runs asynchronously to prevent ANR
    init {
        initScope.launch {
            try {
                val tabs = queries.selectAllTabs().executeAsList().map { it.toDomainModel() }
                val favorites = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
                val history = queries.selectAllHistory(100, 0).executeAsList().map { it.toDomainModel() }
                val downloads = queries.selectAllDownloads(100, 0).executeAsList().map { it.toDomainModel() }

                // Load settings with default insert if not exists
                queries.insertDefaultSettings()
                val dbSettings = queries.selectSettings().executeAsOneOrNull()
                val settings = dbSettings?.toDomainModel() ?: BrowserSettings.default()

                // FIX: Update StateFlows on Main thread for safe UI updates
                withContext(Dispatchers.Main) {
                    _tabs.value = tabs
                    _favorites.value = favorites
                    _history.value = history
                    _downloads.value = downloads
                    _settings.value = settings
                }

                println("BrowserRepositoryImpl: Loaded ${tabs.size} tabs, ${favorites.size} favorites, ${history.size} history entries, ${downloads.size} downloads")
            } catch (e: Exception) {
                println("BrowserRepositoryImpl: Error loading initial data: ${e.message}")
            }
        }
    }

    // ==================== Tab Operations ====================

    override suspend fun createTab(tab: Tab): Result<Tab> = withContext(Dispatchers.IO) {
        try {
            queries.insertTab(tab.toDbModel())
            refreshTabs()
            Result.success(tab)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTab(tabId: String): Result<Tab?> = withContext(Dispatchers.IO) {
        try {
            val dbTab = queries.selectTabById(tabId).executeAsOneOrNull()
            Result.success(dbTab?.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllTabs(): Result<List<Tab>> = withContext(Dispatchers.IO) {
        try {
            val tabs = queries.selectAllTabs().executeAsList().map { it.toDomainModel() }
            Result.success(tabs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeTabs(): Flow<List<Tab>> = _tabs.asStateFlow()

    override suspend fun updateTab(tab: Tab): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateTab(
                url = tab.url,
                title = tab.title,
                favicon = tab.favicon,
                is_active = if (tab.isActive) 1L else 0L,
                is_pinned = if (tab.isPinned) 1L else 0L,
                last_accessed_at = tab.lastAccessedAt.toEpochMilliseconds(),
                position = tab.position.toLong(),
                session_data = tab.sessionData,
                scroll_x_position = tab.scrollXPosition.toLong(),
                scroll_y_position = tab.scrollYPosition.toLong(),
                zoom_level = tab.zoomLevel.toLong(),
                is_desktop_mode = if (tab.isDesktopMode) 1L else 0L,
                id = tab.id
            )
            refreshTabs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeTab(tabId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteTab(tabId)
            refreshTabs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeTabs(tabIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Wrap multiple deletes to prevent partial tab closures
            database.transactionWithResult {
                tabIds.forEach { queries.deleteTab(it) }
            }
            refreshTabs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun closeAllTabs(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Ensure atomic deletion of all tabs
            database.transactionWithResult {
                queries.deleteAllTabs()
            }
            refreshTabs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setActiveTab(tabId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Wrap 2-query operation to prevent partial writes
            database.transactionWithResult {
                queries.clearActiveTab()
                queries.setTabActive(tabId)
            }
            refreshTabs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setPinned(tabId: String, isPinned: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tab = queries.selectTabById(tabId).executeAsOneOrNull()
            if (tab != null) {
                queries.updateTab(
                    url = tab.url,
                    title = tab.title,
                    favicon = tab.favicon,
                    is_active = tab.is_active,
                    is_pinned = if (isPinned) 1L else 0L,
                    last_accessed_at = tab.last_accessed_at,
                    position = tab.position,
                    session_data = tab.session_data,
                    scroll_x_position = tab.scroll_x_position,
                    scroll_y_position = tab.scroll_y_position,
                    zoom_level = tab.zoom_level,
                    is_desktop_mode = tab.is_desktop_mode,
                    id = tab.id
                )
                refreshTabs()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reorderTabs(tabIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Wrap multiple updates to prevent partial reordering
            database.transactionWithResult {
                tabIds.forEachIndexed { index, tabId ->
                    val tab = queries.selectTabById(tabId).executeAsOneOrNull()
                    if (tab != null) {
                        queries.updateTab(
                            url = tab.url,
                            title = tab.title,
                            favicon = tab.favicon,
                            is_active = tab.is_active,
                            is_pinned = tab.is_pinned,
                            last_accessed_at = tab.last_accessed_at,
                            position = index.toLong(),
                            session_data = tab.session_data,
                            scroll_x_position = tab.scroll_x_position,
                            scroll_y_position = tab.scroll_y_position,
                            zoom_level = tab.zoom_level,
                            is_desktop_mode = tab.is_desktop_mode,
                            id = tab.id
                        )
                    }
                }
            }
            refreshTabs()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Favorite Operations ====================

    override suspend fun addFavorite(favorite: Favorite): Result<Favorite> = withContext(Dispatchers.IO) {
        try {
            queries.insertFavorite(favorite.toDbModel())
            refreshFavorites()
            Result.success(favorite)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavorite(favoriteId: String): Result<Favorite?> = withContext(Dispatchers.IO) {
        try {
            val dbFavorite = queries.selectFavoriteById(favoriteId).executeAsOneOrNull()
            Result.success(dbFavorite?.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllFavorites(): Result<List<Favorite>> = withContext(Dispatchers.IO) {
        try {
            val favorites = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFavoritesInFolder(folderId: String?): Result<List<Favorite>> = withContext(Dispatchers.IO) {
        try {
            val favorites = queries.selectFavoritesInFolder(folderId).executeAsList().map { it.toDomainModel() }
            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeFavorites(): Flow<List<Favorite>> = _favorites.asStateFlow()

    override suspend fun updateFavorite(favorite: Favorite): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateFavorite(
                title = favorite.title,
                favicon = favorite.favicon,
                folder_id = favorite.folderId,
                description = favorite.description,
                last_modified_at = favorite.lastModifiedAt.toEpochMilliseconds(),
                visit_count = favorite.visitCount.toLong(),
                position = favorite.position.toLong(),
                id = favorite.id
            )
            refreshFavorites()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorite(favoriteId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteFavorite(favoriteId)
            refreshFavorites()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeFavorites(favoriteIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Wrap multiple deletes to prevent partial favorite removal
            database.transactionWithResult {
                favoriteIds.forEach { queries.deleteFavorite(it) }
            }
            refreshFavorites()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun isFavorite(url: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val favorite = queries.selectFavoriteByUrl(url).executeAsOneOrNull()
            Result.success(favorite != null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchFavorites(query: String): Result<List<Favorite>> = withContext(Dispatchers.IO) {
        try {
            val favorites = queries.searchFavorites(query, query, query).executeAsList().map { it.toDomainModel() }
            Result.success(favorites)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createFolder(folder: FavoriteFolder): Result<FavoriteFolder> = withContext(Dispatchers.IO) {
        Result.success(folder)
    }

    override suspend fun getAllFolders(): Result<List<FavoriteFolder>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    override suspend fun deleteFolder(folderId: String, deleteContents: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    // ==================== History Operations ====================

    override suspend fun addHistoryEntry(entry: HistoryEntry): Result<HistoryEntry> = withContext(Dispatchers.IO) {
        try {
            queries.insertHistoryEntry(entry.toDbModel())
            refreshHistory()
            Result.success(entry)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistory(limit: Int, offset: Int): Result<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val history = queries.selectAllHistory(limit.toLong(), offset.toLong()).executeAsList().map { it.toDomainModel() }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistoryByDateRange(startDate: Instant, endDate: Instant): Result<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val history = queries.selectHistoryByDateRange(
                startDate.toEpochMilliseconds(),
                endDate.toEpochMilliseconds()
            ).executeAsList().map { it.toDomainModel() }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchHistory(query: String, limit: Int): Result<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val history = queries.searchHistory(query, query, query, limit.toLong()).executeAsList().map { it.toDomainModel() }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMostVisited(limit: Int): Result<List<HistoryEntry>> = withContext(Dispatchers.IO) {
        try {
            val results = queries.selectMostVisited(limit.toLong()).executeAsList()
            val history = results.map { result ->
                HistoryEntry(
                    id = "",
                    url = result.url,
                    title = result.title,
                    favicon = result.favicon,
                    visitedAt = Clock.System.now(),
                    visitCount = result.max_visits?.toInt() ?: 0,
                    visitDuration = 0,
                    referrer = null,
                    searchTerms = null,
                    isIncognito = false,
                    deviceId = null
                )
            }
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeHistory(): Flow<List<HistoryEntry>> = _history.asStateFlow()

    override suspend fun deleteHistoryEntry(entryId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteHistoryEntry(entryId)
            refreshHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteHistoryForUrl(url: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteHistoryByUrl(url)
            refreshHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearHistoryByDateRange(startDate: Instant, endDate: Instant): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Ensure atomic deletion of history entries by date range
            database.transactionWithResult {
                queries.deleteHistoryByDateRange(
                    startDate.toEpochMilliseconds(),
                    endDate.toEpochMilliseconds()
                )
            }
            refreshHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllHistory(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Ensure atomic deletion of all history
            database.transactionWithResult {
                queries.deleteAllHistory()
            }
            refreshHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getHistorySessions(limit: Int): Result<List<HistorySession>> = withContext(Dispatchers.IO) {
        Result.success(emptyList())
    }

    // ==================== Download Operations ====================

    override fun observeDownloads(): Flow<List<Download>> = _downloads.asStateFlow()

    override suspend fun addDownload(download: Download): Result<Download> = withContext(Dispatchers.IO) {
        try {
            queries.insertDownload(download.toDbModel())
            refreshDownloads()
            Result.success(download)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDownload(downloadId: String): Result<Download?> = withContext(Dispatchers.IO) {
        try {
            val dbDownload = queries.selectDownloadById(downloadId).executeAsOneOrNull()
            Result.success(dbDownload?.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDownloadByManagerId(managerId: Long): Download? = withContext(Dispatchers.IO) {
        try {
            queries.selectDownloadByManagerId(managerId).executeAsOneOrNull()?.toDomainModel()
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getAllDownloads(limit: Int, offset: Int): Result<List<Download>> = withContext(Dispatchers.IO) {
        try {
            val downloads = queries.selectAllDownloads(limit.toLong(), offset.toLong())
                .executeAsList()
                .map { it.toDomainModel() }
            Result.success(downloads)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getDownloadsByStatus(status: DownloadStatus): Result<List<Download>> = withContext(Dispatchers.IO) {
        try {
            val downloads = queries.selectDownloadsByStatus(status.name)
                .executeAsList()
                .map { it.toDomainModel() }
            Result.success(downloads)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDownload(download: Download): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.insertDownload(download.toDbModel())
            refreshDownloads()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateDownloadProgress(
        downloadId: String,
        downloadedSize: Long,
        status: DownloadStatus
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateDownloadProgress(downloadedSize, status.name, downloadId)
            refreshDownloads()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setDownloadManagerId(downloadId: String, managerId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateDownloadManagerId(managerId, downloadId)
            refreshDownloads()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun completeDownload(
        downloadId: String,
        filepath: String,
        downloadedSize: Long
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateDownloadComplete(
                filepath,
                downloadedSize,
                Clock.System.now().toEpochMilliseconds(),
                downloadId
            )
            refreshDownloads()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun failDownload(downloadId: String, errorMessage: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateDownloadFailed(errorMessage, downloadId)
            refreshDownloads()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteDownload(downloadId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteDownload(downloadId)
            refreshDownloads()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllDownloads(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteAllDownloads()
            refreshDownloads()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun searchDownloads(query: String): Result<List<Download>> = withContext(Dispatchers.IO) {
        try {
            val downloads = queries.searchDownloads(query)
                .executeAsList()
                .map { it.toDomainModel() }
            Result.success(downloads)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Settings Operations ====================

    override suspend fun getSettings(): Result<BrowserSettings> = withContext(Dispatchers.IO) {
        try {
            queries.insertDefaultSettings()
            val dbSettings = queries.selectSettings().executeAsOneOrNull()
            val settings = dbSettings?.toDomainModel() ?: BrowserSettings.default()
            Result.success(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observeSettings(): Flow<BrowserSettings> = _settings.asStateFlow()

    override suspend fun updateSettings(settings: BrowserSettings): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.updateSettings(
                theme = settings.theme.name,
                font_size = settings.fontSize.name,
                force_zoom = if (settings.forceZoom) 1L else 0L,
                show_images = if (settings.showImages) 1L else 0L,
                use_desktop_mode = if (settings.useDesktopMode) 1L else 0L,
                block_popups = if (settings.blockPopups) 1L else 0L,
                block_ads = if (settings.blockAds) 1L else 0L,
                block_trackers = if (settings.blockTrackers) 1L else 0L,
                do_not_track = if (settings.doNotTrack) 1L else 0L,
                clear_cache_on_exit = if (settings.clearCacheOnExit) 1L else 0L,
                clear_history_on_exit = if (settings.clearHistoryOnExit) 1L else 0L,
                clear_cookies_on_exit = if (settings.clearCookiesOnExit) 1L else 0L,
                enable_cookies = if (settings.enableCookies) 1L else 0L,
                enable_javascript = if (settings.enableJavaScript) 1L else 0L,
                enable_webrtc = if (settings.enableWebRTC) 1L else 0L,
                default_search_engine = settings.defaultSearchEngine.name,
                search_suggestions = if (settings.searchSuggestions) 1L else 0L,
                voice_search = if (settings.voiceSearch) 1L else 0L,
                home_page = settings.homePage,
                new_tab_page = settings.newTabPage.name,
                restore_tabs_on_startup = if (settings.restoreTabsOnStartup) 1L else 0L,
                open_links_in_background = if (settings.openLinksInBackground) 1L else 0L,
                open_links_in_new_tab = if (settings.openLinksInNewTab) 1L else 0L,
                download_path = settings.downloadPath,
                ask_download_location = if (settings.askDownloadLocation) 1L else 0L,
                download_over_wifi_only = if (settings.downloadOverWiFiOnly) 1L else 0L,
                sync_enabled = if (settings.syncEnabled) 1L else 0L,
                sync_bookmarks = if (settings.syncBookmarks) 1L else 0L,
                sync_history = if (settings.syncHistory) 1L else 0L,
                sync_passwords = if (settings.syncPasswords) 1L else 0L,
                sync_settings = if (settings.syncSettings) 1L else 0L,
                hardware_acceleration = if (settings.hardwareAcceleration) 1L else 0L,
                preload_pages = if (settings.preloadPages) 1L else 0L,
                data_saver = if (settings.dataSaver) 1L else 0L,
                auto_play = settings.autoPlay.name,
                text_reflow = if (settings.textReflow) 1L else 0L,
                enable_voice_commands = if (settings.enableVoiceCommands) 1L else 0L,
                ai_summaries = if (settings.aiSummaries) 1L else 0L,
                ai_translation = if (settings.aiTranslation) 1L else 0L,
                read_aloud = if (settings.readAloud) 1L else 0L
            )
            _settings.value = settings
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun <T> updateSetting(key: String, value: T): Result<Unit> = withContext(Dispatchers.IO) {
        Result.success(Unit)
    }

    override suspend fun resetSettings(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            updateSettings(BrowserSettings.default())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun applyPreset(preset: SettingsPreset): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val settings = when (preset) {
                SettingsPreset.DEFAULT -> BrowserSettings.default()
                SettingsPreset.PRIVACY -> BrowserSettings.default().copy(
                    blockAds = true,
                    blockTrackers = true,
                    doNotTrack = true,
                    clearCacheOnExit = true,
                    clearHistoryOnExit = true,
                    clearCookiesOnExit = true,
                    enableWebRTC = false
                )
                SettingsPreset.PERFORMANCE -> BrowserSettings.default().copy(
                    hardwareAcceleration = true,
                    preloadPages = true,
                    showImages = true,
                    dataSaver = false
                )
                SettingsPreset.ACCESSIBILITY -> BrowserSettings.default().copy(
                    forceZoom = true,
                    textReflow = true,
                    fontSize = BrowserSettings.FontSize.LARGE
                )
            }
            updateSettings(settings)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

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
            Result.failure(e)
        }
    }

    override suspend fun importData(data: BrowserData): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Wrap all imports to ensure all-or-nothing data restoration
            database.transactionWithResult {
                data.tabs.forEach { queries.insertTab(it.toDbModel()) }
                data.favorites.forEach { queries.insertFavorite(it.toDbModel()) }
                data.history.forEach { queries.insertHistoryEntry(it.toDbModel()) }
            }
            updateSettings(data.settings)
            refreshTabs()
            refreshFavorites()
            refreshHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // PHASE 1: ACID Transaction (Data Integrity Fix)
            // Wrap all deletes to ensure complete data wipe or rollback
            database.transactionWithResult {
                queries.deleteAllTabs()
                queries.deleteAllHistory()
                queries.selectAllFavorites().executeAsList().forEach { queries.deleteFavorite(it.id) }
            }
            resetSettings()
            refreshTabs()
            refreshFavorites()
            refreshHistory()
            Result.success(Unit)
        } catch (e: Exception) {
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
            Result.failure(e)
        }
    }

    // ==================== Site Permission Operations ====================

    override suspend fun getSitePermission(
        domain: String,
        permissionType: String
    ): Result<SitePermission?> = withContext(Dispatchers.IO) {
        try {
            val permission = queries.getSitePermission(domain, permissionType)
                .executeAsOneOrNull()
                ?.toDomainModel()
            Result.success(permission)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun insertSitePermission(
        domain: String,
        permissionType: String,
        granted: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.insertSitePermission(
                domain = domain,
                permission_type = permissionType,
                granted = if (granted) 1L else 0L,
                timestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteSitePermission(
        domain: String,
        permissionType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteSitePermission(domain, permissionType)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAllSitePermissions(domain: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteAllSitePermissions(domain)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllSitePermissions(): Result<List<SitePermission>> = withContext(Dispatchers.IO) {
        try {
            val permissions = queries.getAllSitePermissions()
                .executeAsList()
                .map { it.toDomainModel() }
            Result.success(permissions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== Private Helpers ====================

    private suspend fun refreshTabs() {
        try {
            _tabs.value = queries.selectAllTabs().executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            println("BrowserRepositoryImpl: Error in refreshTabs: ${e.message}")
        }
    }

    private suspend fun refreshFavorites() {
        try {
            _favorites.value = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            println("BrowserRepositoryImpl: Error in refreshFavorites: ${e.message}")
        }
    }

    private suspend fun refreshHistory() {
        try {
            _history.value = queries.selectAllHistory(100, 0).executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            println("BrowserRepositoryImpl: Error in refreshHistory: ${e.message}")
        }
    }

    private suspend fun refreshDownloads() {
        try {
            _downloads.value = queries.selectAllDownloads(100, 0).executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            println("BrowserRepositoryImpl: Error in refreshDownloads: ${e.message}")
        }
    }

    // FIX C7: Cleanup repository resources
    override fun cleanup() {
        try {
            initScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            println("BrowserRepositoryImpl: initScope cancelled")
        } catch (e: Exception) {
            println("BrowserRepositoryImpl: Error in cleanup: ${e.message}")
        }
    }
}

// ==================== Type Mapping Extensions ====================

private fun Tab.toDbModel() = com.augmentalis.webavanue.data.Tab(
    id = id, url = url, title = title, favicon = favicon,
    is_active = if (isActive) 1L else 0L,
    is_pinned = if (isPinned) 1L else 0L,
    is_incognito = if (isIncognito) 1L else 0L,
    created_at = createdAt.toEpochMilliseconds(),
    last_accessed_at = lastAccessedAt.toEpochMilliseconds(),
    position = position.toLong(),
    parent_tab_id = parentTabId,
    group_id = groupId,  // Added for tab groups
    session_data = sessionData,
    scroll_x_position = scrollXPosition.toLong(),
    scroll_y_position = scrollYPosition.toLong(),
    zoom_level = zoomLevel.toLong(),
    is_desktop_mode = if (isDesktopMode) 1L else 0L
)

private fun com.augmentalis.webavanue.data.Tab.toDomainModel() = Tab(
    id = id, url = url, title = title, favicon = favicon,
    isActive = is_active != 0L, isPinned = is_pinned != 0L, isIncognito = is_incognito != 0L,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    lastAccessedAt = Instant.fromEpochMilliseconds(last_accessed_at),
    position = position.toInt(), parentTabId = parent_tab_id, groupId = group_id, sessionData = session_data,
    scrollXPosition = scroll_x_position.toInt(),
    scrollYPosition = scroll_y_position.toInt(),
    zoomLevel = zoom_level.toInt(),
    isDesktopMode = is_desktop_mode != 0L
)

private fun Favorite.toDbModel() = com.augmentalis.webavanue.data.Favorite(
    id = id, url = url, title = title, favicon = favicon, folder_id = folderId,
    description = description,
    created_at = createdAt.toEpochMilliseconds(),
    last_modified_at = lastModifiedAt.toEpochMilliseconds(),
    visit_count = visitCount.toLong(), position = position.toLong()
)

private fun com.augmentalis.webavanue.data.Favorite.toDomainModel() = Favorite(
    id = id, url = url, title = title, favicon = favicon, folderId = folder_id,
    description = description,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    lastModifiedAt = Instant.fromEpochMilliseconds(last_modified_at),
    visitCount = visit_count.toInt(), position = position.toInt(), tags = emptyList()
)

private fun HistoryEntry.toDbModel() = com.augmentalis.webavanue.data.History_entry(
    id = id, url = url, title = title, favicon = favicon,
    visited_at = visitedAt.toEpochMilliseconds(),
    visit_count = visitCount.toLong(), visit_duration = visitDuration.toLong(),
    referrer = referrer, search_terms = searchTerms,
    is_incognito = if (isIncognito) 1L else 0L, device_id = deviceId
)

private fun com.augmentalis.webavanue.data.History_entry.toDomainModel() = HistoryEntry(
    id = id, url = url, title = title, favicon = favicon,
    visitedAt = Instant.fromEpochMilliseconds(visited_at),
    visitCount = visit_count.toInt(), visitDuration = visit_duration,
    referrer = referrer, searchTerms = search_terms,
    isIncognito = is_incognito != 0L, deviceId = device_id
)

// FIX: Safe enum parsing helper to prevent crashes from invalid DB values
private inline fun <reified T : Enum<T>> safeEnumValueOf(value: String, default: T): T {
    return try {
        enumValueOf<T>(value)
    } catch (e: IllegalArgumentException) {
        println("BrowserRepositoryImpl: Invalid enum value '$value' for ${T::class.simpleName}, using default: $default")
        default
    }
}

private fun com.augmentalis.webavanue.data.Browser_settings.toDomainModel() = BrowserSettings(
    theme = safeEnumValueOf(theme, BrowserSettings.Theme.SYSTEM),
    fontSize = safeEnumValueOf(font_size, BrowserSettings.FontSize.MEDIUM),
    forceZoom = force_zoom != 0L, showImages = show_images != 0L,
    useDesktopMode = use_desktop_mode != 0L, blockPopups = block_popups != 0L,
    blockAds = block_ads != 0L, blockTrackers = block_trackers != 0L,
    doNotTrack = do_not_track != 0L, clearCacheOnExit = clear_cache_on_exit != 0L,
    clearHistoryOnExit = clear_history_on_exit != 0L, clearCookiesOnExit = clear_cookies_on_exit != 0L,
    enableCookies = enable_cookies != 0L, enableJavaScript = enable_javascript != 0L,
    enableWebRTC = enable_webrtc != 0L,
    defaultSearchEngine = safeEnumValueOf(default_search_engine, BrowserSettings.SearchEngine.GOOGLE),
    searchSuggestions = search_suggestions != 0L, voiceSearch = voice_search != 0L,
    homePage = home_page, newTabPage = safeEnumValueOf(new_tab_page, BrowserSettings.NewTabPage.BLANK),
    restoreTabsOnStartup = restore_tabs_on_startup != 0L,
    openLinksInBackground = open_links_in_background != 0L,
    openLinksInNewTab = open_links_in_new_tab != 0L,
    downloadPath = download_path, askDownloadLocation = ask_download_location != 0L,
    downloadOverWiFiOnly = download_over_wifi_only != 0L, syncEnabled = sync_enabled != 0L,
    syncBookmarks = sync_bookmarks != 0L, syncHistory = sync_history != 0L,
    syncPasswords = sync_passwords != 0L, syncSettings = sync_settings != 0L,
    hardwareAcceleration = hardware_acceleration != 0L, preloadPages = preload_pages != 0L,
    dataSaver = data_saver != 0L, autoPlay = safeEnumValueOf(auto_play, BrowserSettings.AutoPlay.NEVER),
    textReflow = text_reflow != 0L, enableVoiceCommands = enable_voice_commands != 0L,
    aiSummaries = ai_summaries != 0L, aiTranslation = ai_translation != 0L,
    readAloud = read_aloud != 0L
)

private fun com.augmentalis.webavanue.data.Site_permission.toDomainModel() = SitePermission(
    domain = domain,
    permissionType = permission_type,
    granted = granted != 0L,
    timestamp = Instant.fromEpochMilliseconds(timestamp)
)

private fun Download.toDbModel() = com.augmentalis.webavanue.data.Download(
    id = id,
    url = url,
    filename = filename,
    filepath = filepath,
    mime_type = mimeType,
    file_size = fileSize,
    downloaded_size = downloadedSize,
    status = status.name,
    error_message = errorMessage,
    download_manager_id = downloadManagerId,
    created_at = createdAt.toEpochMilliseconds(),
    completed_at = completedAt?.toEpochMilliseconds(),
    source_page_url = sourcePageUrl,
    source_page_title = sourcePageTitle
)

private fun com.augmentalis.webavanue.data.Download.toDomainModel() = Download(
    id = id,
    url = url,
    filename = filename,
    filepath = filepath,
    mimeType = mime_type,
    fileSize = file_size,
    downloadedSize = downloaded_size,
    status = DownloadStatus.fromString(status),
    errorMessage = error_message,
    downloadManagerId = download_manager_id,
    createdAt = Instant.fromEpochMilliseconds(created_at),
    completedAt = completed_at?.let { Instant.fromEpochMilliseconds(it) },
    sourcePageUrl = source_page_url,
    sourcePageTitle = source_page_title
)
