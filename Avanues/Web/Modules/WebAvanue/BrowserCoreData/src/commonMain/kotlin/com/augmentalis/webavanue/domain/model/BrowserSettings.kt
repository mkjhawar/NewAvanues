package com.augmentalis.webavanue.domain.model

import kotlinx.serialization.Serializable

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
    val searchSuggestions: Boolean = true,
    val voiceSearch: Boolean = true,

    // Navigation Settings
    val homePage: String = "https://www.google.com",
    val newTabPage: NewTabPage = NewTabPage.TOP_SITES,
    val restoreTabsOnStartup: Boolean = true,
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

    // Voice & AI Settings
    val enableVoiceCommands: Boolean = true,
    val aiSummaries: Boolean = false,
    val aiTranslation: Boolean = false,
    val readAloud: Boolean = false,

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
    val xrRequireWiFi: Boolean = false // Only allow XR on WiFi (data usage protection)
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