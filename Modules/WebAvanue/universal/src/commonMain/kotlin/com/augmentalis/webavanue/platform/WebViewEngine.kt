package com.augmentalis.webavanue.platform

/**
 * Platform-agnostic WebView interface
 *
 * Implementations:
 * - Android: android.webkit.WebView
 * - iOS: WKWebView
 * - Desktop: JCEF (Java Chromium Embedded Framework)
 */
expect class WebViewEngine {
    /**
     * Load a URL in the web view
     */
    fun loadUrl(url: String)

    /**
     * Navigate back in history
     */
    fun goBack()

    /**
     * Navigate forward in history
     */
    fun goForward()

    /**
     * Reload current page
     */
    fun reload()

    /**
     * Stop loading current page
     */
    fun stopLoading()

    /**
     * Check if can navigate back
     */
    fun canGoBack(): Boolean

    /**
     * Check if can navigate forward
     */
    fun canGoForward(): Boolean

    /**
     * Get current URL
     */
    fun getCurrentUrl(): String?

    /**
     * Get current title
     */
    fun getCurrentTitle(): String?

    /**
     * Evaluate JavaScript code
     */
    fun evaluateJavaScript(script: String, callback: ((String?) -> Unit)? = null)

    /**
     * Clear cache
     */
    fun clearCache()

    /**
     * Clear cookies
     */
    fun clearCookies()

    /**
     * Set user agent
     */
    fun setUserAgent(userAgent: String)

    /**
     * Enable/disable JavaScript
     */
    fun setJavaScriptEnabled(enabled: Boolean)

    /**
     * Destroy the web view
     */
    fun destroy()
}
