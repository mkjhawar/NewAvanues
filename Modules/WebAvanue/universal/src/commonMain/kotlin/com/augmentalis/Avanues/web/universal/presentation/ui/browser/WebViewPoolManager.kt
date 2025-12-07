package com.augmentalis.Avanues.web.universal.presentation.ui.browser

/**
 * WebViewPoolManager - Platform-specific WebView instance cleanup
 *
 * Provides a platform-agnostic interface for managing WebView lifecycle.
 * Each platform (Android, iOS, etc.) implements cleanup logic for their WebView instances.
 */
expect object WebViewPoolManager {
    /**
     * Remove and destroy WebView for a specific tab
     * @param tabId Tab ID to clean up
     */
    fun removeWebView(tabId: String)

    /**
     * Remove and destroy all WebView instances
     */
    fun clearAllWebViews()
}
