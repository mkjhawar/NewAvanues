package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * WebViewContainer - Platform-specific WebView wrapper
 *
 * This is a common interface for platform-specific WebView implementations.
 * Actual implementation is provided by each platform (Android, iOS, Desktop, Web).
 *
 * Usage:
 * ```kotlin
 * WebViewContainer(
 *     url = "https://example.com",
 *     onUrlChange = { newUrl -> /* handle URL change */ },
 *     onLoadingChange = { isLoading -> /* handle loading state */ },
 *     onTitleChange = { title -> /* handle title change */ },
 *     modifier = Modifier.fillMaxSize()
 * )
 * ```
 *
 * Platform implementations:
 * - Android: AndroidView with WebView
 * - iOS: UIViewRepresentable with WKWebView
 * - Desktop: JavaFX WebView or CEF
 * - Web: iframe
 *
 * @param url URL to load
 * @param onUrlChange Callback when URL changes (navigation, redirects)
 * @param onLoadingChange Callback when loading state changes
 * @param onTitleChange Callback when page title changes
 * @param onProgressChange Callback when loading progress changes (0.0 to 1.0)
 * @param canGoBack Callback to report if can navigate back
 * @param canGoForward Callback to report if can navigate forward
 * @param modifier Modifier for customization
 */
@Composable
expect fun WebViewContainer(
    url: String,
    controller: WebViewController?,
    onUrlChange: (String) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onProgressChange: (Float) -> Unit,
    canGoBack: (Boolean) -> Unit,
    canGoForward: (Boolean) -> Unit,
    modifier: Modifier
)

/**
 * WebViewController - Controller for WebView operations
 *
 * Platform-specific implementation for controlling WebView.
 * Use this to programmatically control WebView (go back, forward, reload, etc.)
 */
expect class WebViewController() {
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
     * Load URL
     * @param url URL to load
     */
    fun loadUrl(url: String)

    /**
     * Evaluate JavaScript
     * @param script JavaScript code to execute
     * @param callback Callback with result
     */
    fun evaluateJavaScript(script: String, callback: (String?) -> Unit)

    /**
     * Clear cache
     */
    fun clearCache()

    /**
     * Clear cookies
     */
    fun clearCookies()

    /**
     * Clear history
     */
    fun clearHistory()

    /**
     * Set user agent
     * @param userAgent User agent string
     */
    fun setUserAgent(userAgent: String)

    /**
     * Enable/disable JavaScript
     * @param enabled Whether to enable JavaScript
     */
    fun setJavaScriptEnabled(enabled: Boolean)

    /**
     * Enable/disable cookies
     * @param enabled Whether to enable cookies
     */
    fun setCookiesEnabled(enabled: Boolean)

    /**
     * Set desktop mode
     * @param enabled Whether to request desktop version of sites
     */
    fun setDesktopMode(enabled: Boolean)

    // ========== Scrolling Controls ==========

    /**
     * Scroll up by a fraction of the viewport
     */
    fun scrollUp()

    /**
     * Scroll down by a fraction of the viewport
     */
    fun scrollDown()

    /**
     * Scroll left
     */
    fun scrollLeft()

    /**
     * Scroll right
     */
    fun scrollRight()

    /**
     * Scroll to top of page
     */
    fun scrollToTop()

    /**
     * Scroll to bottom of page
     */
    fun scrollToBottom()

    // ========== Zoom Controls ==========

    /**
     * Zoom in
     */
    fun zoomIn()

    /**
     * Zoom out
     */
    fun zoomOut()

    /**
     * Set zoom level (1-5)
     * @param level Zoom level from 1 (smallest) to 5 (largest)
     */
    fun setZoomLevel(level: Int)

    /**
     * Get current zoom level
     */
    fun getZoomLevel(): Int

    // ========== Touch/Interaction Controls ==========

    /**
     * Toggle freeze/unfreeze page scrolling
     * @param frozen Whether to freeze scrolling
     */
    fun setScrollFrozen(frozen: Boolean)

    /**
     * Perform a click at the center of the WebView
     */
    fun performClick()

    /**
     * Perform a double click at the center of the WebView
     */
    fun performDoubleClick()
}
