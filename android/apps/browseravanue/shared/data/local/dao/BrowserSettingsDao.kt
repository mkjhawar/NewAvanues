package com.augmentalis.browseravanue.data.local.dao

import androidx.room.*
import com.augmentalis.browseravanue.data.local.entity.BrowserSettingsEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO interface for BrowserSettings operations
 *
 * Architecture:
 * - Interface-based design works with BOTH shared and standalone databases
 * - Shared mode: Implemented by AvanuesDatabase
 * - Standalone mode: Implemented by BrowserAvanueDatabase
 * - Single-row table (ID always = 1)
 * - Flow-based reactive queries
 * - Suspend functions for CRUD operations
 */
@Dao
interface BrowserSettingsDao {

    /**
     * Observe settings (reactive)
     * Always returns single settings object (ID = 1)
     */
    @Query("SELECT * FROM browser_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<BrowserSettingsEntity?>

    /**
     * Get settings (one-time query)
     * Returns null if not initialized
     */
    @Query("SELECT * FROM browser_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): BrowserSettingsEntity?

    /**
     * Insert or replace settings
     * Always uses ID = 1 for singleton pattern
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: BrowserSettingsEntity)

    /**
     * Update settings
     */
    @Update
    suspend fun updateSettings(settings: BrowserSettingsEntity)

    /**
     * Delete settings (for reset)
     */
    @Query("DELETE FROM browser_settings WHERE id = 1")
    suspend fun deleteSettings()

    /**
     * Update zoom level
     */
    @Query("UPDATE browser_settings SET default_zoom_level = :zoomLevel, updated_at = :timestamp WHERE id = 1")
    suspend fun updateZoomLevel(zoomLevel: String, timestamp: Long)

    /**
     * Update JavaScript enabled state
     */
    @Query("UPDATE browser_settings SET enable_javascript = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updateJavaScriptEnabled(enabled: Boolean, timestamp: Long)

    /**
     * Update cookies enabled state
     */
    @Query("UPDATE browser_settings SET enable_cookies = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updateCookiesEnabled(enabled: Boolean, timestamp: Long)

    /**
     * Update desktop mode default
     */
    @Query("UPDATE browser_settings SET default_to_desktop_mode = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updateDesktopModeDefault(enabled: Boolean, timestamp: Long)

    /**
     * Update custom user agent
     */
    @Query("UPDATE browser_settings SET custom_user_agent = :userAgent, updated_at = :timestamp WHERE id = 1")
    suspend fun updateCustomUserAgent(userAgent: String?, timestamp: Long)

    /**
     * Update search engine
     */
    @Query("UPDATE browser_settings SET default_search_engine = :searchEngine, updated_at = :timestamp WHERE id = 1")
    suspend fun updateSearchEngine(searchEngine: String, timestamp: Long)

    /**
     * Update cache size
     */
    @Query("UPDATE browser_settings SET cache_size = :cacheSize, updated_at = :timestamp WHERE id = 1")
    suspend fun updateCacheSize(cacheSize: String, timestamp: Long)

    /**
     * Update voice commands enabled state
     */
    @Query("UPDATE browser_settings SET enable_voice_commands = :enabled, updated_at = :timestamp WHERE id = 1")
    suspend fun updateVoiceCommandsEnabled(enabled: Boolean, timestamp: Long)

    /**
     * Bulk privacy settings update
     */
    @Query("""
        UPDATE browser_settings SET
        enable_cookies = :enableCookies,
        enable_third_party_cookies = :enableThirdPartyCookies,
        clear_cookies_on_exit = :clearCookiesOnExit,
        clear_history_on_exit = :clearHistoryOnExit,
        do_not_track = :doNotTrack,
        enable_geolocation = :enableGeolocation,
        enable_notifications = :enableNotifications,
        block_mixed_content = :blockMixedContent,
        updated_at = :timestamp
        WHERE id = 1
    """)
    suspend fun updatePrivacySettings(
        enableCookies: Boolean,
        enableThirdPartyCookies: Boolean,
        clearCookiesOnExit: Boolean,
        clearHistoryOnExit: Boolean,
        doNotTrack: Boolean,
        enableGeolocation: Boolean,
        enableNotifications: Boolean,
        blockMixedContent: Boolean,
        timestamp: Long
    )
}
