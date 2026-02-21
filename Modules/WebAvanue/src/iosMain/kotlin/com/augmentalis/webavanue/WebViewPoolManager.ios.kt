package com.augmentalis.webavanue

import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore

/**
 * iOS WebViewPoolManager implementation.
 *
 * Manages lifecycle of WKWebView instances keyed by tab ID, and handles
 * cookie/data-store cleanup on exit.
 */
actual object WebViewPoolManager {
    private val webViews = mutableMapOf<String, WKWebView>()

    /**
     * Remove and destroy the WKWebView associated with [tabId].
     */
    actual fun removeWebView(tabId: String) {
        val webView = webViews.remove(tabId) ?: return
        webView.stopLoading()
        webView.loadHTMLString("", baseURL = null)
        webView.removeFromSuperview()
    }

    /**
     * Remove and destroy all managed WKWebView instances.
     */
    actual fun clearAllWebViews() {
        val ids = webViews.keys.toList()
        ids.forEach { removeWebView(it) }
        webViews.clear()
    }

    /**
     * Clear all website cookies and cached data from the default data store.
     * Called on app exit to ensure no browsing data persists across sessions.
     */
    actual fun clearCookiesOnExit() {
        val dataStore = WKWebsiteDataStore.defaultDataStore()
        val allTypes = WKWebsiteDataStore.allWebsiteDataTypes()
        dataStore.removeDataOfTypes(
            dataTypes = allTypes,
            modifiedSince = platform.Foundation.NSDate.distantPast()
        ) {
            // Completion handler — no action required on exit
        }
    }

    /**
     * Register a WKWebView instance for a given tab ID so it can be managed.
     * Platform-specific convenience method — not part of the expect declaration.
     */
    fun registerWebView(tabId: String, webView: WKWebView) {
        webViews[tabId] = webView
    }
}
