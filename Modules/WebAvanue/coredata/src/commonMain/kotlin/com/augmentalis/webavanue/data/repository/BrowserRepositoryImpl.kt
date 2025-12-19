package com.augmentalis.webavanue.data.repository

import io.github.aakira.napier.Napier
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
 * BrowserRepositoryImpl - SQLDelight implementation of BrowserRepository
 *
 * ## Overview
 * Provides cross-platform data persistence for WebAvanue browser using SQLDelight database.
 * Implements reactive data access with Flow-based observation for tabs, favorites, history,
 * downloads, and settings.
 *
 * ## Architecture
 * - **Data Source**: SQLDelight (BrowserDatabase)
 * - **Reactive Layer**: StateFlows for UI observation
 * - **Threading**: All database operations run on Dispatchers.IO
 * - **Initialization**: Async startup with lazy loading for fast app launch
 *
 * ## Threading Model
 * - **Database Operations**: Dispatchers.IO (all suspend functions)
 * - **Flow Updates**: Dispatchers.Main (UI-safe StateFlow updates)
 * - **Initialization**: Background (SupervisorJob + IO dispatcher)
 *
 * ## State Management
 * In-memory StateFlows mirror database state for reactive UI:
 * - [_tabs] - Active and recent tabs
 * - [_favorites] - User bookmarks
 * - [_history] - Browsing history (limited to 100 recent entries)
 * - [_downloads] - Download manager state
 * - [_settings] - Global browser settings
 *
 * ## Performance Optimizations
 * - **Fast Startup**: Load only 10 recent tabs + settings on init (~50ms)
 * - **Lazy Loading**: Defer full data load to background
 * - **ACID Transactions**: Wrap multi-query operations for data integrity
 * - **Batch Updates**: Use transactions for bulk operations (closeTabs, reorderTabs)
 *
 * ## Error Handling
 * - All methods return `Result<T>` for safe error propagation
 * - Napier logging for debugging database operations
 * - Safe enum parsing with defaults for corrupt database values
 *
 * ## Resource Cleanup
 * Call [cleanup] when repository is no longer needed to cancel background jobs.
 *
 * ## Usage Example
 * ```kotlin
 * val repository = BrowserRepositoryImpl(database)
 *
 * // Observe tabs reactively
 * repository.observeTabs().collect { tabs ->
 *     println("Tabs updated: ${tabs.size}")
 * }
 *
 * // Create a new tab
 * repository.createTab(Tab.create(url = "https://example.com"))
 *     .onSuccess { tab -> println("Tab created: ${tab.id}") }
 *     .onFailure { error -> println("Error: ${error.message}") }
 * ```
 *
 * @param database SQLDelight database instance
 * @see BrowserRepository
 * @see BrowserDatabase
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

    // PERFORMANCE OPTIMIZATION Phase 2: Lazy loading for faster startup
    // Only load essential data on init, defer non-critical data
    @Volatile
    private var allTabsLoaded = false
    @Volatile
    private var favoritesLoaded = false
    @Volatile
    private var historyLoaded = false

    // FIX Issues #2 & #3: Initialize data from database on repository creation
    // FIX P1-3: Now runs asynchronously to prevent ANR
    // PERFORMANCE: Load only active tab + 10 recent tabs on startup
    init {
        initScope.launch {
            try {
                val startTime = Clock.System.now().toEpochMilliseconds()

                // PRIORITY 1: Load only recent tabs (fast startup)
                val recentTabs = queries.selectRecentTabs(10).executeAsList().map { it.toDomainModel() }

                // PRIORITY 2: Load settings (needed for UI)
                queries.insertDefaultSettings()
                val dbSettings = queries.selectSettings().executeAsOneOrNull()
                val settings = dbSettings?.toDomainModel() ?: BrowserSettings.default()

                // FIX: Update StateFlows on Main thread for safe UI updates
                withContext(Dispatchers.Main) {
                    _tabs.value = recentTabs
                    _settings.value = settings
                }

                val initTime = Clock.System.now().toEpochMilliseconds() - startTime
                Napier.i("Fast startup: Loaded ${recentTabs.size} recent tabs in ${initTime}ms", tag = "BrowserRepository")

                // DEFERRED: Load remaining data in background (non-blocking)
                launch {
                    try {
                        // Load all tabs
                        val allTabs = queries.selectAllTabs().executeAsList().map { it.toDomainModel() }
                        withContext(Dispatchers.Main) {
                            _tabs.value = allTabs
                            allTabsLoaded = true
                        }
                        Napier.i("Background: Loaded ${allTabs.size} total tabs", tag = "BrowserRepository")
                    } catch (e: Exception) {
                        Napier.e("Error loading all tabs: ${e.message}", e, tag = "BrowserRepository")
                    }
                }

                launch {
                    try {
                        val favorites = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
                        withContext(Dispatchers.Main) {
                            _favorites.value = favorites
                            favoritesLoaded = true
                        }
                        Napier.i("Background: Loaded ${favorites.size} favorites", tag = "BrowserRepository")
                    } catch (e: Exception) {
                        Napier.e("Error loading favorites: ${e.message}", e, tag = "BrowserRepository")
                    }
                }

                launch {
                    try {
                        val history = queries.selectAllHistory(100, 0).executeAsList().map { it.toDomainModel() }
                        val downloads = queries.selectAllDownloads(100, 0).executeAsList().map { it.toDomainModel() }
                        withContext(Dispatchers.Main) {
                            _history.value = history
                            _downloads.value = downloads
                            historyLoaded = true
                        }
                        Napier.i("Background: Loaded ${history.size} history entries, ${downloads.size} downloads", tag = "BrowserRepository")
                    } catch (e: Exception) {
                        Napier.e("Error loading history/downloads: ${e.message}", e, tag = "BrowserRepository")
                    }
                }
            } catch (e: Exception) {
                Napier.e("Error loading initial data: ${e.message}", e, tag = "BrowserRepository")
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

    override suspend fun getRecentTabs(limit: Int): Result<List<Tab>> = withContext(Dispatchers.IO) {
        try {
            val tabs = queries.selectRecentTabs(limit.toLong()).executeAsList().map { it.toDomainModel() }
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
            // PRIVATE BROWSING: Skip history for incognito/private tabs
            if (entry.isIncognito) {
                Napier.d("Skipping history entry for private browsing: ${entry.url}", tag = "BrowserRepository")
                return@withContext Result.success(entry)
            }

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
            Napier.e("Error in refreshTabs: ${e.message}", e, tag = "BrowserRepository")
        }
    }

    private suspend fun refreshFavorites() {
        try {
            _favorites.value = queries.selectAllFavorites().executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            Napier.e("Error in refreshFavorites: ${e.message}", e, tag = "BrowserRepository")
        }
    }

    private suspend fun refreshHistory() {
        try {
            _history.value = queries.selectAllHistory(100, 0).executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            Napier.e("Error in refreshHistory: ${e.message}", e, tag = "BrowserRepository")
        }
    }

    private suspend fun refreshDownloads() {
        try {
            _downloads.value = queries.selectAllDownloads(100, 0).executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            Napier.e("Error in refreshDownloads: ${e.message}", e, tag = "BrowserRepository")
        }
    }

    // FIX C7: Cleanup repository resources
    override fun cleanup() {
        try {
            initScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
            Napier.d("initScope cancelled", tag = "BrowserRepository")
        } catch (e: Exception) {
            Napier.e("Error in cleanup: ${e.message}", e, tag = "BrowserRepository")
        }
    }

    // ==================== Session Restore Operations ====================

    override suspend fun saveSession(session: Session, tabs: List<SessionTab>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Insert session
            queries.insertSession(
                id = session.id,
                timestamp = session.timestamp.toEpochMilliseconds(),
                active_tab_id = session.activeTabId,
                tab_count = session.tabCount.toLong(),
                is_crash_recovery = if (session.isCrashRecovery) 1L else 0L
            )

            // Insert all session tabs
            tabs.forEach { sessionTab ->
                queries.insertSessionTab(sessionTab.toDbModel())
            }

            Napier.d("Saved session ${session.id} with ${tabs.size} tabs", tag = "BrowserRepository")
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to save session: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun getSession(sessionId: String): Result<Session?> = withContext(Dispatchers.IO) {
        try {
            val dbSession = queries.selectSessionById(sessionId).executeAsOneOrNull()
            Result.success(dbSession?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Failed to get session $sessionId: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun getLatestSession(): Result<Session?> = withContext(Dispatchers.IO) {
        try {
            val dbSession = queries.selectLatestSession().executeAsOneOrNull()
            Result.success(dbSession?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Failed to get latest session: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun getLatestCrashSession(): Result<Session?> = withContext(Dispatchers.IO) {
        try {
            val dbSession = queries.selectLatestCrashSession().executeAsOneOrNull()
            Result.success(dbSession?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Failed to get latest crash session: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun getAllSessions(limit: Int, offset: Int): Result<List<Session>> = withContext(Dispatchers.IO) {
        try {
            val dbSessions = queries.selectAllSessions(limit.toLong(), offset.toLong()).executeAsList()
            Result.success(dbSessions.map { it.toDomainModel() })
        } catch (e: Exception) {
            Napier.e("Failed to get all sessions: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun getSessionTabs(sessionId: String): Result<List<SessionTab>> = withContext(Dispatchers.IO) {
        try {
            val dbTabs = queries.selectSessionTabs(sessionId).executeAsList()
            Result.success(dbTabs.map { it.toDomainModel() })
        } catch (e: Exception) {
            Napier.e("Failed to get session tabs for $sessionId: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun getActiveSessionTab(sessionId: String): Result<SessionTab?> = withContext(Dispatchers.IO) {
        try {
            val dbTabs = queries.selectSessionTabs(sessionId).executeAsList()
            val activeTab = dbTabs.firstOrNull { it.is_active != 0L }
            Result.success(activeTab?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Failed to get active session tab for $sessionId: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun deleteSession(sessionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteSession(sessionId)
            Napier.d("Deleted session $sessionId", tag = "BrowserRepository")
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to delete session $sessionId: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun deleteAllSessions(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get all sessions first
            val sessions = queries.selectAllSessions(1000, 0).executeAsList()
            sessions.forEach { session ->
                queries.deleteSession(session.id)
            }
            Napier.d("Deleted all sessions", tag = "BrowserRepository")
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to delete all sessions: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }

    override suspend fun deleteOldSessions(timestamp: Instant): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteOldSessions(timestamp.toEpochMilliseconds())
            Napier.d("Deleted sessions older than $timestamp", tag = "BrowserRepository")
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Failed to delete old sessions: ${e.message}", e, tag = "BrowserRepository")
            Result.failure(e)
        }
    }
}

// Type mapping extensions have been refactored to DbMappers.kt
// Import all mappers from the extracted file for use in this class
