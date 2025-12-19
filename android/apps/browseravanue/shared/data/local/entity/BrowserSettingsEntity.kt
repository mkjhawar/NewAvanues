package com.augmentalis.browseravanue.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for BrowserSettings
 * Maps to 'browser_settings' table in database
 *
 * Architecture:
 * - Data layer entity (not domain model)
 * - Pure database representation
 * - Mapped to/from domain BrowserSettings via BrowserSettingsMapper
 * - Single row table (id always = 1)
 */
@Entity(tableName = "browser_settings")
data class BrowserSettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1, // Always 1 - single settings instance

    // Display Settings
    @ColumnInfo(name = "default_zoom_level")
    val defaultZoomLevel: String, // Enum stored as String

    @ColumnInfo(name = "enable_fullscreen")
    val enableFullscreen: Boolean,

    @ColumnInfo(name = "show_status_bar")
    val showStatusBar: Boolean,

    // Privacy Settings
    @ColumnInfo(name = "enable_javascript")
    val enableJavaScript: Boolean,

    @ColumnInfo(name = "enable_cookies")
    val enableCookies: Boolean,

    @ColumnInfo(name = "enable_third_party_cookies")
    val enableThirdPartyCookies: Boolean,

    @ColumnInfo(name = "clear_cookies_on_exit")
    val clearCookiesOnExit: Boolean,

    @ColumnInfo(name = "clear_history_on_exit")
    val clearHistoryOnExit: Boolean,

    @ColumnInfo(name = "do_not_track")
    val doNotTrack: Boolean,

    // Content Settings
    @ColumnInfo(name = "enable_images")
    val enableImages: Boolean,

    @ColumnInfo(name = "enable_popups")
    val enablePopups: Boolean,

    @ColumnInfo(name = "enable_geolocation")
    val enableGeolocation: Boolean,

    @ColumnInfo(name = "enable_notifications")
    val enableNotifications: Boolean,

    @ColumnInfo(name = "block_mixed_content")
    val blockMixedContent: Boolean,

    // User Agent & Desktop Mode
    @ColumnInfo(name = "default_to_desktop_mode")
    val defaultToDesktopMode: Boolean,

    @ColumnInfo(name = "custom_user_agent")
    val customUserAgent: String?,

    // Downloads
    @ColumnInfo(name = "download_path")
    val downloadPath: String?,

    @ColumnInfo(name = "ask_download_location")
    val askDownloadLocation: Boolean,

    // Search Settings
    @ColumnInfo(name = "default_search_engine")
    val defaultSearchEngine: String, // Enum stored as String

    @ColumnInfo(name = "search_suggestions")
    val searchSuggestions: Boolean,

    // Performance
    @ColumnInfo(name = "enable_cache")
    val enableCache: Boolean,

    @ColumnInfo(name = "cache_size")
    val cacheSize: String, // Enum stored as String

    @ColumnInfo(name = "enable_hardware_acceleration")
    val enableHardwareAcceleration: Boolean,

    // Voice Command Settings
    @ColumnInfo(name = "enable_voice_commands")
    val enableVoiceCommands: Boolean,

    @ColumnInfo(name = "voice_feedback")
    val voiceFeedback: Boolean,

    @ColumnInfo(name = "confirm_voice_actions")
    val confirmVoiceActions: Boolean,

    // Tab Settings
    @ColumnInfo(name = "open_links_in_new_tab")
    val openLinksInNewTab: Boolean,

    @ColumnInfo(name = "close_last_tab_exits")
    val closeLastTabExits: Boolean,

    @ColumnInfo(name = "restore_tabs_on_startup")
    val restoreTabsOnStartup: Boolean,

    // Advanced
    @ColumnInfo(name = "enable_dev_tools")
    val enableDevTools: Boolean,

    @ColumnInfo(name = "enable_safe_browsing")
    val enableSafeBrowsing: Boolean,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long
)
