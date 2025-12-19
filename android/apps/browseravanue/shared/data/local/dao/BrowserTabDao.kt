package com.augmentalis.browseravanue.data.local.dao

import androidx.room.*
import com.augmentalis.browseravanue.data.local.entity.TabEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO interface for Browser Tab operations
 *
 * Architecture:
 * - Interface-based design works with BOTH shared and standalone databases
 * - Shared mode: Implemented by AvanuesDatabase
 * - Standalone mode: Implemented by BrowserAvanueDatabase
 * - Flow-based reactive queries
 * - Suspend functions for CRUD operations
 */
@Dao
interface BrowserTabDao {

    /**
     * Observe all tabs ordered by last accessed (most recent first)
     */
    @Query("SELECT * FROM tabs ORDER BY last_accessed DESC")
    fun observeAllTabs(): Flow<List<TabEntity>>

    /**
     * Get all tabs (one-time query)
     */
    @Query("SELECT * FROM tabs ORDER BY last_accessed DESC")
    suspend fun getAllTabs(): List<TabEntity>

    /**
     * Observe specific tab by ID
     */
    @Query("SELECT * FROM tabs WHERE id = :tabId")
    fun observeTabById(tabId: String): Flow<TabEntity?>

    /**
     * Get specific tab by ID
     */
    @Query("SELECT * FROM tabs WHERE id = :tabId")
    suspend fun getTabById(tabId: String): TabEntity?

    /**
     * Get most recently accessed tab
     */
    @Query("SELECT * FROM tabs ORDER BY last_accessed DESC LIMIT 1")
    suspend fun getMostRecentTab(): TabEntity?

    /**
     * Get tabs by URL (for checking if URL already open)
     */
    @Query("SELECT * FROM tabs WHERE url = :url")
    suspend fun getTabsByUrl(url: String): List<TabEntity>

    /**
     * Get count of tabs
     */
    @Query("SELECT COUNT(*) FROM tabs")
    suspend fun getTabCount(): Int

    /**
     * Observe count of tabs
     */
    @Query("SELECT COUNT(*) FROM tabs")
    fun observeTabCount(): Flow<Int>

    /**
     * Get recently active tabs (within 5 minutes)
     */
    @Query("SELECT * FROM tabs WHERE last_accessed > :timestamp ORDER BY last_accessed DESC")
    suspend fun getRecentlyActiveTabs(timestamp: Long): List<TabEntity>

    /**
     * Get tabs in desktop mode
     */
    @Query("SELECT * FROM tabs WHERE is_desktop_mode = 1 ORDER BY last_accessed DESC")
    suspend fun getDesktopModeTabs(): List<TabEntity>

    /**
     * Search tabs by URL or title
     */
    @Query("""
        SELECT * FROM tabs
        WHERE url LIKE '%' || :query || '%'
        OR title LIKE '%' || :query || '%'
        ORDER BY last_accessed DESC
    """)
    suspend fun searchTabs(query: String): List<TabEntity>

    /**
     * Insert tab
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: TabEntity)

    /**
     * Insert multiple tabs
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTabs(tabs: List<TabEntity>)

    /**
     * Update tab
     */
    @Update
    suspend fun updateTab(tab: TabEntity)

    /**
     * Delete tab
     */
    @Delete
    suspend fun deleteTab(tab: TabEntity)

    /**
     * Delete tab by ID
     */
    @Query("DELETE FROM tabs WHERE id = :tabId")
    suspend fun deleteTabById(tabId: String)

    /**
     * Delete all tabs
     */
    @Query("DELETE FROM tabs")
    suspend fun deleteAllTabs()

    /**
     * Delete old tabs (older than specified timestamp)
     */
    @Query("DELETE FROM tabs WHERE created_at < :timestamp")
    suspend fun deleteOldTabs(timestamp: Long)

    /**
     * Update last accessed timestamp
     */
    @Query("UPDATE tabs SET last_accessed = :timestamp WHERE id = :tabId")
    suspend fun updateLastAccessed(tabId: String, timestamp: Long)

    /**
     * Update loading state
     */
    @Query("UPDATE tabs SET is_loading = :isLoading WHERE id = :tabId")
    suspend fun updateLoadingState(tabId: String, isLoading: Boolean)

    /**
     * Update navigation state
     */
    @Query("UPDATE tabs SET can_go_back = :canGoBack, can_go_forward = :canGoForward WHERE id = :tabId")
    suspend fun updateNavigationState(tabId: String, canGoBack: Boolean, canGoForward: Boolean)
}
