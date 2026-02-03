package com.augmentalis.webavanue.repository

import com.augmentalis.webavanue.Tab
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.toDbModel
import com.augmentalis.webavanue.toDomainModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository interface for tab operations.
 *
 * Handles all tab-related data persistence including:
 * - Tab CRUD operations
 * - Tab state management (active, pinned)
 * - Tab reordering
 * - Reactive observation via Flow
 */
interface TabRepository {
    /** Creates a new tab */
    suspend fun createTab(tab: Tab): Result<Tab>

    /** Gets a tab by ID */
    suspend fun getTab(tabId: String): Result<Tab?>

    /** Gets all open tabs */
    suspend fun getAllTabs(): Result<List<Tab>>

    /** Gets recent tabs (optimized for startup) */
    suspend fun getRecentTabs(limit: Int = 10): Result<List<Tab>>

    /** Observes all tabs with real-time updates */
    fun observeTabs(): Flow<List<Tab>>

    /** Updates an existing tab */
    suspend fun updateTab(tab: Tab): Result<Unit>

    /** Closes a tab */
    suspend fun closeTab(tabId: String): Result<Unit>

    /** Closes multiple tabs */
    suspend fun closeTabs(tabIds: List<String>): Result<Unit>

    /** Closes all tabs */
    suspend fun closeAllTabs(): Result<Unit>

    /** Sets the active tab */
    suspend fun setActiveTab(tabId: String): Result<Unit>

    /** Pins or unpins a tab */
    suspend fun setPinned(tabId: String, isPinned: Boolean): Result<Unit>

    /** Reorders tabs */
    suspend fun reorderTabs(tabIds: List<String>): Result<Unit>

    /** Refreshes in-memory state from database */
    suspend fun refresh()

    /** Updates in-memory state directly (for fast startup) */
    suspend fun updateState(tabs: List<Tab>)
}

/**
 * SQLDelight implementation of TabRepository.
 *
 * Thread Safety:
 * - All database operations run on Dispatchers.IO
 * - StateFlow updates are thread-safe
 *
 * @param database SQLDelight database instance
 */
class TabRepositoryImpl(
    private val database: BrowserDatabase
) : TabRepository {

    private val queries = database.browserDatabaseQueries
    private val _tabs = MutableStateFlow<List<Tab>>(emptyList())

    override suspend fun createTab(tab: Tab): Result<Tab> = withContext(Dispatchers.IO) {
        try {
            queries.insertTab(tab.toDbModel())
            refresh()
            Result.success(tab)
        } catch (e: Exception) {
            Napier.e("Error creating tab: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getTab(tabId: String): Result<Tab?> = withContext(Dispatchers.IO) {
        try {
            val dbTab = queries.selectTabById(tabId).executeAsOneOrNull()
            Result.success(dbTab?.toDomainModel())
        } catch (e: Exception) {
            Napier.e("Error getting tab: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getAllTabs(): Result<List<Tab>> = withContext(Dispatchers.IO) {
        try {
            val tabs = queries.selectAllTabs().executeAsList().map { it.toDomainModel() }
            Result.success(tabs)
        } catch (e: Exception) {
            Napier.e("Error getting all tabs: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getRecentTabs(limit: Int): Result<List<Tab>> = withContext(Dispatchers.IO) {
        try {
            val tabs = queries.selectRecentTabs(limit.toLong()).executeAsList().map { it.toDomainModel() }
            Result.success(tabs)
        } catch (e: Exception) {
            Napier.e("Error getting recent tabs: ${e.message}", e, tag = TAG)
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
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error updating tab: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun closeTab(tabId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteTab(tabId)
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error closing tab: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun closeTabs(tabIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionWithResult {
                tabIds.forEach { queries.deleteTab(it) }
            }
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error closing tabs: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun closeAllTabs(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionWithResult {
                queries.deleteAllTabs()
            }
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error closing all tabs: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun setActiveTab(tabId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            database.transactionWithResult {
                queries.clearActiveTab()
                queries.setTabActive(tabId)
            }
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error setting active tab: ${e.message}", e, tag = TAG)
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
                refresh()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error setting pinned: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun reorderTabs(tabIds: List<String>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
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
            refresh()
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error reordering tabs: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun refresh() {
        try {
            _tabs.value = queries.selectAllTabs().executeAsList().map { it.toDomainModel() }
        } catch (e: Exception) {
            Napier.e("Error refreshing tabs: ${e.message}", e, tag = TAG)
        }
    }

    override suspend fun updateState(tabs: List<Tab>) {
        _tabs.value = tabs
    }

    companion object {
        private const val TAG = "TabRepository"
    }
}
