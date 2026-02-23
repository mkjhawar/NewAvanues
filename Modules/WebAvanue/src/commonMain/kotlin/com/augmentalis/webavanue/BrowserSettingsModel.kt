package com.augmentalis.webavanue

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
/**
 * Browser settings and preferences for WebAvanue.
 * Cross-platform settings that sync across devices.
 */
@Serializable
data class BrowserSettings(
    // Display Settings
    val theme: Theme = Theme.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val forceZoom: Boolean = false,
    val showImages: Boolean = true,
    val useDesktopMode: Boolean = false,

    // Scale Settings (Mode-Specific)
    val mobilePortraitScale: Float = 0f,   // Mobile portrait scale (default 100%)
    val mobileLandscapeScale: Float = 0.75f, // Mobile landscape scale (default 75%)

    // Desktop Mode Settings
    val desktopModeDefaultZoom: Int = 100, // Default zoom level (50-200%)
    val desktopModeWindowWidth: Int = 1280, // Simulated window width in pixels
    val desktopModeWindowHeight: Int = 800, // Simulated window height in pixels
    val desktopModeAutoFitZoom: Boolean = true, // Auto-adjust zoom to fit content in viewport

    // Privacy Settings
    val blockPopups: Boolean = true,
    val blockAds: Boolean = true,
    val blockTrackers: Boolean = true,
    val doNotTrack: Boolean = true,
    val clearCacheOnExit: Boolean = false,
    val clearHistoryOnExit: Boolean = false,
    val clearCookiesOnExit: Boolean = false,
    val enableCookies: Boolean = true,
    val enableJavaScript: Boolean = true,
    val enableWebRTC: Boolean = false,

    // Search Settings
    val defaultSearchEngine: SearchEngine = SearchEngine.GOOGLE,
    val customSearchEngineName: String = "Custom",
    val customSearchEngineUrl: String = "",
    val searchSuggestions: Boolean = true,
    val voiceSearch: Boolean = true,

    // Navigation Settings
    val homePage: String = "https://www.google.com",
    val newTabPage: NewTabPage = NewTabPage.TOP_SITES,
    val restoreTabsOnStartup: Boolean = true,
    val showRestoreDialog: Boolean = true, // Show "Restore previous session?" dialog
    val openLinksInBackground: Boolean = false,
    val openLinksInNewTab: Boolean = false,

    // Download Settings
    val downloadPath: String? = null, // null = default system path
    val askDownloadLocation: Boolean = false,
    val downloadOverWiFiOnly: Boolean = false,

    // Sync Settings
    val syncEnabled: Boolean = false,
    val syncBookmarks: Boolean = true,
    val syncHistory: Boolean = true,
    val syncPasswords: Boolean = false,
    val syncSettings: Boolean = true,

    // Advanced Settings
    val hardwareAcceleration: Boolean = true,
    val preloadPages: Boolean = true,
    val dataSaver: Boolean = false,
    val autoPlay: AutoPlay = AutoPlay.WIFI_ONLY,
    val textReflow: Boolean = true,

    // Security Settings (Phase 4)
    val enableDatabaseEncryption: Boolean = false,  // Default: unencrypted for performance
    val enableSecureStorage: Boolean = false,  // Default: unencrypted SharedPreferences

    // Voice & AI Settings
    val enableVoiceCommands: Boolean = true,
    val aiSummaries: Boolean = false,
    val aiTranslation: Boolean = false,
    val readAloud: Boolean = false,

    // UI State Settings (FR-010, FR-011, FR-004)
    val voiceDialogAutoClose: Boolean = true, // Auto-close voice dialog after command
    val voiceDialogAutoCloseDelayMs: Long = 2000, // Delay before auto-closing voice dialog (ms)
    val commandBarAutoHide: Boolean = true, // Auto-hide command bar after timeout
    val commandBarAutoHideDelayMs: Long = 10000, // Delay before auto-hiding (ms) - 10 seconds
    val commandBarOrientation: CommandBarOrientation = CommandBarOrientation.AUTO, // Command bar layout

    // WebXR Settings
    // REQ-XR-001: WebXR API Support
    // REQ-XR-002: Camera Permission Management
    // REQ-XR-007: Performance Optimization for XR
    val enableWebXR: Boolean = true, // Master switch for WebXR functionality
    val enableAR: Boolean = true, // Allow immersive-ar sessions
    val enableVR: Boolean = true, // Allow immersive-vr sessions
    val xrPerformanceMode: XRPerformanceMode = XRPerformanceMode.BALANCED, // Performance vs battery
    val xrAutoPauseTimeout: Int = 30, // Minutes before auto-pause (battery protection)
    val xrShowFPSIndicator: Boolean = false, // Show frame rate indicator in XR sessions
    val xrRequireWiFi: Boolean = false, // Only allow XR on WiFi (data usage protection)

    // Reading Mode Settings
    val readingModeTheme: ReadingModeTheme = ReadingModeTheme.LIGHT,
    val readingModeFontSize: Float = 1.0f,  // Font size multiplier (0.75 - 2.0)
    val readingModeLineHeight: Float = 1.5f,  // Line height multiplier (1.0 - 2.0)
    val readingModeFontFamily: ReadingModeFontFamily = ReadingModeFontFamily.SYSTEM,
    val autoDetectArticles: Boolean = true  // Auto-show reader view available indicator
) {
    /**
     * Theme options
     */
    enum class Theme {
        LIGHT,
        DARK,
        SYSTEM,
        AUTO // Based on time of day
    }

    /**
     * Font size options
     */
    enum class FontSize(val scale: Float) {
        TINY(0.75f),
        SMALL(0.875f),
        MEDIUM(1.0f),
        LARGE(1.125f),
        HUGE(1.25f)
    }

    /**
     * Search engine options
     */
    enum class SearchEngine(val baseUrl: String, val queryParam: String) {
        GOOGLE("https://www.google.com/search", "q"),
        DUCKDUCKGO("https://duckduckgo.com/", "q"),
        BING("https://www.bing.com/search", "q"),
        BRAVE("https://search.brave.com/search", "q"),
        ECOSIA("https://www.ecosia.org/search", "q"),
        CUSTOM("", "q")
    }

    /**
     * New tab page options
     */
    enum class NewTabPage {
        BLANK,
        HOME_PAGE,
        TOP_SITES,
        MOST_VISITED,
        SPEED_DIAL,
        NEWS_FEED
    }

    /**
     * Auto-play media options
     */
    enum class AutoPlay {
        ALWAYS,
        WIFI_ONLY,
        NEVER,
        ASK
    }

    /**
     * WebXR performance mode options
     * REQ-XR-007: Performance Optimization for XR
     */
    enum class XRPerformanceMode {
        /** Maximum quality, higher battery drain (90fps target) */
        HIGH_QUALITY,

        /** Balanced quality and performance (60fps target) */
        BALANCED,

        /** Battery-saving mode (45fps target, reduced effects) */
        BATTERY_SAVER
    }

    /**
     * Reading mode theme options
     */
    enum class ReadingModeTheme {
        LIGHT,
        DARK,
        SEPIA
    }

    /**
     * Reading mode font family options
     */
    enum class ReadingModeFontFamily {
        SYSTEM,
        SERIF,
        SANS_SERIF,
        MONOSPACE
    }

    /**
     * Command bar orientation preference.
     * AUTO follows device orientation, HORIZONTAL/VERTICAL force a specific layout.
     */
    enum class CommandBarOrientation {
        AUTO,        // Portrait → horizontal, landscape → vertical
        HORIZONTAL,  // Always horizontal bar at bottom
        VERTICAL     // Always vertical bar on right side
    }

    /**
     * Calculate the appropriate scale based on mode and orientation
     */
    fun getScaleForMode(isDesktopMode: Boolean, isLandscape: Boolean): Float {
        return when {
            isDesktopMode -> desktopModeDefaultZoom / 100f
            isLandscape -> mobileLandscapeScale
            else -> mobilePortraitScale
        }
    }

    override fun toString(): String {
        return "BrowserSettings(theme=$theme, fontSize=$fontSize, forceZoom=$forceZoom, showImages=$showImages, useDesktopMode=$useDesktopMode, mobilePortraitScale=$mobilePortraitScale, mobileLandscapeScale=$mobileLandscapeScale, desktopModeDefaultZoom=$desktopModeDefaultZoom, desktopModeWindowWidth=$desktopModeWindowWidth, desktopModeWindowHeight=$desktopModeWindowHeight, desktopModeAutoFitZoom=$desktopModeAutoFitZoom, blockPopups=$blockPopups, blockAds=$blockAds, blockTrackers=$blockTrackers, doNotTrack=$doNotTrack, clearCacheOnExit=$clearCacheOnExit, clearHistoryOnExit=$clearHistoryOnExit, clearCookiesOnExit=$clearCookiesOnExit, enableCookies=$enableCookies, enableJavaScript=$enableJavaScript, enableWebRTC=$enableWebRTC, defaultSearchEngine=$defaultSearchEngine, searchSuggestions=$searchSuggestions, voiceSearch=$voiceSearch, homePage='$homePage', newTabPage=$newTabPage, restoreTabsOnStartup=$restoreTabsOnStartup, showRestoreDialog=$showRestoreDialog, openLinksInBackground=$openLinksInBackground, openLinksInNewTab=$openLinksInNewTab, downloadPath=$downloadPath, askDownloadLocation=$askDownloadLocation, downloadOverWiFiOnly=$downloadOverWiFiOnly, syncEnabled=$syncEnabled, syncBookmarks=$syncBookmarks, syncHistory=$syncHistory, syncPasswords=$syncPasswords, syncSettings=$syncSettings, hardwareAcceleration=$hardwareAcceleration, preloadPages=$preloadPages, dataSaver=$dataSaver, autoPlay=$autoPlay, textReflow=$textReflow, enableDatabaseEncryption=$enableDatabaseEncryption, enableSecureStorage=$enableSecureStorage, enableVoiceCommands=$enableVoiceCommands, aiSummaries=$aiSummaries, aiTranslation=$aiTranslation, readAloud=$readAloud, voiceDialogAutoClose=$voiceDialogAutoClose, voiceDialogAutoCloseDelayMs=$voiceDialogAutoCloseDelayMs, commandBarAutoHide=$commandBarAutoHide, commandBarAutoHideDelayMs=$commandBarAutoHideDelayMs, commandBarOrientation=$commandBarOrientation, enableWebXR=$enableWebXR, enableAR=$enableAR, enableVR=$enableVR, xrPerformanceMode=$xrPerformanceMode, xrAutoPauseTimeout=$xrAutoPauseTimeout, xrShowFPSIndicator=$xrShowFPSIndicator, xrRequireWiFi=$xrRequireWiFi, readingModeTheme=$readingModeTheme, readingModeFontSize=$readingModeFontSize, readingModeLineHeight=$readingModeLineHeight, readingModeFontFamily=$readingModeFontFamily, autoDetectArticles=$autoDetectArticles)"
    }

    companion object {
        /**
         * Default settings for new users
         */
        fun default(): BrowserSettings = BrowserSettings()

        /**
         * Privacy-focused settings preset
         */
        fun privacyMode(): BrowserSettings = BrowserSettings(
            blockPopups = true,
            blockAds = true,
            blockTrackers = true,
            doNotTrack = true,
            clearCacheOnExit = true,
            clearHistoryOnExit = true,
            clearCookiesOnExit = true,
            enableWebRTC = false,
            defaultSearchEngine = SearchEngine.DUCKDUCKGO,
            searchSuggestions = false,
            syncEnabled = false
        )

        /**
         * Performance-focused settings preset
         */
        fun performanceMode(): BrowserSettings = BrowserSettings(
            showImages = false,
            enableJavaScript = false,
            hardwareAcceleration = true,
            preloadPages = false,
            dataSaver = true,
            autoPlay = AutoPlay.NEVER,
            blockAds = true
        )
    }
}