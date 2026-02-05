package com.augmentalis.browseravanue.data.mapper

import com.augmentalis.browseravanue.data.local.entity.BrowserSettingsEntity
import com.augmentalis.browseravanue.domain.model.BrowserSettings
import com.augmentalis.browseravanue.domain.model.CacheSize
import com.augmentalis.browseravanue.domain.model.SearchEngine
import com.augmentalis.browseravanue.domain.model.ZoomLevel

/**
 * Mapper for converting between BrowserSettings domain model and BrowserSettingsEntity
 *
 * Architecture:
 * - Pure mapping logic, no business rules
 * - Bidirectional conversion (toEntity, toDomain)
 * - Handles enum conversions (String ↔ Enum)
 * - Extension functions for clean usage
 *
 * Special Conversions:
 * - ZoomLevel enum ↔ String
 * - SearchEngine enum ↔ String
 * - CacheSize enum ↔ String
 *
 * Usage:
 * ```
 * val domain: BrowserSettings = entity.toDomain()
 * val entity: BrowserSettingsEntity = domain.toEntity()
 * ```
 */
object BrowserSettingsMapper {

    /**
     * Convert BrowserSettingsEntity to BrowserSettings domain model
     */
    fun BrowserSettingsEntity.toDomain(): BrowserSettings {
        return BrowserSettings(
            id = id,
            defaultZoomLevel = parseZoomLevel(defaultZoomLevel),
            enableFullscreen = enableFullscreen,
            showStatusBar = showStatusBar,
            enableJavaScript = enableJavaScript,
            enableCookies = enableCookies,
            enableThirdPartyCookies = enableThirdPartyCookies,
            clearCookiesOnExit = clearCookiesOnExit,
            clearHistoryOnExit = clearHistoryOnExit,
            doNotTrack = doNotTrack,
            enableImages = enableImages,
            enablePopups = enablePopups,
            enableGeolocation = enableGeolocation,
            enableNotifications = enableNotifications,
            blockMixedContent = blockMixedContent,
            defaultToDesktopMode = defaultToDesktopMode,
            customUserAgent = customUserAgent,
            downloadPath = downloadPath,
            askDownloadLocation = askDownloadLocation,
            defaultSearchEngine = parseSearchEngine(defaultSearchEngine),
            searchSuggestions = searchSuggestions,
            enableCache = enableCache,
            cacheSize = parseCacheSize(cacheSize),
            enableHardwareAcceleration = enableHardwareAcceleration,
            enableVoiceCommands = enableVoiceCommands,
            voiceFeedback = voiceFeedback,
            confirmVoiceActions = confirmVoiceActions,
            openLinksInNewTab = openLinksInNewTab,
            closeLastTabExits = closeLastTabExits,
            restoreTabsOnStartup = restoreTabsOnStartup,
            enableDevTools = enableDevTools,
            enableSafeBrowsing = enableSafeBrowsing,
            updatedAt = updatedAt
        )
    }

    /**
     * Convert BrowserSettings domain model to BrowserSettingsEntity
     */
    fun BrowserSettings.toEntity(): BrowserSettingsEntity {
        return BrowserSettingsEntity(
            id = id,
            defaultZoomLevel = defaultZoomLevel.name,
            enableFullscreen = enableFullscreen,
            showStatusBar = showStatusBar,
            enableJavaScript = enableJavaScript,
            enableCookies = enableCookies,
            enableThirdPartyCookies = enableThirdPartyCookies,
            clearCookiesOnExit = clearCookiesOnExit,
            clearHistoryOnExit = clearHistoryOnExit,
            doNotTrack = doNotTrack,
            enableImages = enableImages,
            enablePopups = enablePopups,
            enableGeolocation = enableGeolocation,
            enableNotifications = enableNotifications,
            blockMixedContent = blockMixedContent,
            defaultToDesktopMode = defaultToDesktopMode,
            customUserAgent = customUserAgent,
            downloadPath = downloadPath,
            askDownloadLocation = askDownloadLocation,
            defaultSearchEngine = defaultSearchEngine.name,
            searchSuggestions = searchSuggestions,
            enableCache = enableCache,
            cacheSize = cacheSize.name,
            enableHardwareAcceleration = enableHardwareAcceleration,
            enableVoiceCommands = enableVoiceCommands,
            voiceFeedback = voiceFeedback,
            confirmVoiceActions = confirmVoiceActions,
            openLinksInNewTab = openLinksInNewTab,
            closeLastTabExits = closeLastTabExits,
            restoreTabsOnStartup = restoreTabsOnStartup,
            enableDevTools = enableDevTools,
            enableSafeBrowsing = enableSafeBrowsing,
            updatedAt = updatedAt
        )
    }

    /**
     * Parse ZoomLevel from String
     * Returns NORMAL if invalid
     */
    private fun parseZoomLevel(value: String): ZoomLevel {
        return try {
            ZoomLevel.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ZoomLevel.NORMAL
        }
    }

    /**
     * Parse SearchEngine from String
     * Returns GOOGLE if invalid
     */
    private fun parseSearchEngine(value: String): SearchEngine {
        return try {
            SearchEngine.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SearchEngine.GOOGLE
        }
    }

    /**
     * Parse CacheSize from String
     * Returns MEDIUM if invalid
     */
    private fun parseCacheSize(value: String): CacheSize {
        return try {
            CacheSize.valueOf(value)
        } catch (e: IllegalArgumentException) {
            CacheSize.MEDIUM
        }
    }
}
