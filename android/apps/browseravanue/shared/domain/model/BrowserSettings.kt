package com.augmentalis.browseravanue.domain.model

/**
 * Domain model for browser settings and preferences
 * Pure Kotlin, no Android/Room dependencies
 *
 * Following SOLID principles:
 * - Single Responsibility: Browser settings only
 * - Open/Closed: Immutable data class with typed properties
 */
data class BrowserSettings(
    val id: Int = 1, // Single settings instance

    // Display Settings
    val defaultZoomLevel: ZoomLevel = ZoomLevel.NORMAL,
    val enableFullscreen: Boolean = false,
    val showStatusBar: Boolean = true,

    // Privacy Settings
    val enableJavaScript: Boolean = true,
    val enableCookies: Boolean = true,
    val enableThirdPartyCookies: Boolean = false,
    val clearCookiesOnExit: Boolean = false,
    val clearHistoryOnExit: Boolean = false,
    val doNotTrack: Boolean = true,

    // Content Settings
    val enableImages: Boolean = true,
    val enablePopups: Boolean = false,
    val enableGeolocation: Boolean = false,
    val enableNotifications: Boolean = false,
    val blockMixedContent: Boolean = true,

    // User Agent & Desktop Mode
    val defaultToDesktopMode: Boolean = false,
    val customUserAgent: String? = null,

    // Downloads
    val downloadPath: String? = null,
    val askDownloadLocation: Boolean = true,

    // Search Settings
    val defaultSearchEngine: SearchEngine = SearchEngine.GOOGLE,
    val searchSuggestions: Boolean = true,

    // Performance
    val enableCache: Boolean = true,
    val cacheSize: CacheSize = CacheSize.MEDIUM,
    val enableHardwareAcceleration: Boolean = true,

    // Voice Command Settings
    val enableVoiceCommands: Boolean = true,
    val voiceFeedback: Boolean = true,
    val confirmVoiceActions: Boolean = false,

    // Tab Settings
    val openLinksInNewTab: Boolean = false,
    val closeLastTabExits: Boolean = false,
    val restoreTabsOnStartup: Boolean = true,

    // Advanced
    val enableDevTools: Boolean = false,
    val enableSafeBrowsing: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    /**
     * Update zoom level
     */
    fun updateZoomLevel(level: ZoomLevel): BrowserSettings {
        return copy(defaultZoomLevel = level, updatedAt = System.currentTimeMillis())
    }

    /**
     * Toggle JavaScript
     */
    fun toggleJavaScript(): BrowserSettings {
        return copy(enableJavaScript = !enableJavaScript, updatedAt = System.currentTimeMillis())
    }

    /**
     * Toggle cookies
     */
    fun toggleCookies(): BrowserSettings {
        return copy(enableCookies = !enableCookies, updatedAt = System.currentTimeMillis())
    }

    /**
     * Toggle desktop mode
     */
    fun toggleDesktopMode(): BrowserSettings {
        return copy(defaultToDesktopMode = !defaultToDesktopMode, updatedAt = System.currentTimeMillis())
    }

    /**
     * Set custom user agent
     */
    fun setUserAgent(userAgent: String?): BrowserSettings {
        return copy(customUserAgent = userAgent, updatedAt = System.currentTimeMillis())
    }

    /**
     * Update search engine
     */
    fun updateSearchEngine(engine: SearchEngine): BrowserSettings {
        return copy(defaultSearchEngine = engine, updatedAt = System.currentTimeMillis())
    }

    /**
     * Update cache size
     */
    fun updateCacheSize(size: CacheSize): BrowserSettings {
        return copy(cacheSize = size, updatedAt = System.currentTimeMillis())
    }

    /**
     * Enable/disable privacy mode (multiple privacy settings)
     */
    fun setPrivacyMode(enabled: Boolean): BrowserSettings {
        return copy(
            enableCookies = !enabled,
            enableThirdPartyCookies = false,
            clearCookiesOnExit = enabled,
            clearHistoryOnExit = enabled,
            doNotTrack = enabled,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Reset to defaults
     */
    fun resetToDefaults(): BrowserSettings {
        return BrowserSettings(id = id)
    }

    companion object {
        /**
         * Default settings instance
         */
        fun default(): BrowserSettings = BrowserSettings()

        /**
         * Privacy-focused settings
         */
        fun privacyFocused(): BrowserSettings {
            return BrowserSettings(
                enableCookies = false,
                enableThirdPartyCookies = false,
                clearCookiesOnExit = true,
                clearHistoryOnExit = true,
                doNotTrack = true,
                enableGeolocation = false,
                enableNotifications = false,
                blockMixedContent = true
            )
        }

        /**
         * Performance-focused settings
         */
        fun performanceFocused(): BrowserSettings {
            return BrowserSettings(
                enableImages = true,
                enableCache = true,
                cacheSize = CacheSize.LARGE,
                enableHardwareAcceleration = true
            )
        }
    }
}

/**
 * Zoom level enum
 */
enum class ZoomLevel(val scale: Float) {
    VERY_SMALL(0.5f),
    SMALL(0.75f),
    NORMAL(1.0f),
    LARGE(1.25f),
    VERY_LARGE(1.5f);

    fun getDisplayName(): String = when (this) {
        VERY_SMALL -> "50%"
        SMALL -> "75%"
        NORMAL -> "100%"
        LARGE -> "125%"
        VERY_LARGE -> "150%"
    }
}

/**
 * Search engine enum
 */
enum class SearchEngine(val displayName: String, val searchUrl: String) {
    GOOGLE("Google", "https://www.google.com/search?q="),
    BING("Bing", "https://www.bing.com/search?q="),
    DUCKDUCKGO("DuckDuckGo", "https://duckduckgo.com/?q="),
    YAHOO("Yahoo", "https://search.yahoo.com/search?p="),
    ECOSIA("Ecosia", "https://www.ecosia.org/search?q=");

    /**
     * Build search URL with query
     */
    fun buildSearchUrl(query: String): String {
        return searchUrl + java.net.URLEncoder.encode(query, "UTF-8")
    }
}

/**
 * Cache size enum
 */
enum class CacheSize(val sizeInMB: Int) {
    SMALL(50),
    MEDIUM(100),
    LARGE(200),
    VERY_LARGE(500);

    fun getDisplayName(): String = "$sizeInMB MB"
}
