package com.augmentalis.webavanue.ui.screen.browser

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.augmentalis.webavanue.ui.viewmodel.SecurityViewModel
import com.augmentalis.webavanue.domain.model.BrowserSettings

/**
 * WebViewContainer - Platform-specific WebView wrapper
 *
 * This is a common interface for platform-specific WebView implementations.
 * Actual implementation is provided by each platform (Android, iOS, Desktop, Web).
 *
 * Usage:
 * ```kotlin
 * WebViewContainer(
 *     tabId = "tab-1",
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
 * @param tabId Unique identifier for the tab (used to preserve WebView instances)
 * @param url URL to load
 * @param onUrlChange Callback when URL changes (navigation, redirects)
 * @param onLoadingChange Callback when loading state changes
 * @param onTitleChange Callback when page title changes
 * @param onProgressChange Callback when loading progress changes (0.0 to 1.0)
 * @param canGoBack Callback to report if can navigate back
 * @param canGoForward Callback to report if can navigate forward
 * @param sessionData Persisted WebView state (navigation history, scroll position, etc.)
 * @param onSessionDataChange Callback when session data changes (for persistence)
 * @param modifier Modifier for customization
 */
/**
 * Data class for download request information
 */
data class DownloadRequest(
    val url: String,
    val filename: String,
    val mimeType: String?,
    val contentLength: Long,
    val userAgent: String?,
    val contentDisposition: String?
)

@Composable
expect fun WebViewContainer(
    tabId: String,
    url: String,
    controller: WebViewController?,
    onUrlChange: (String) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onProgressChange: (Float) -> Unit,
    canGoBack: (Boolean) -> Unit,
    canGoForward: (Boolean) -> Unit,
    onOpenInNewTab: (String) -> Unit,
    sessionData: String?,
    onSessionDataChange: (String?) -> Unit,
    securityViewModel: SecurityViewModel? = null,
    onDownloadStart: ((DownloadRequest) -> Unit)? = null,
    initialScale: Float = 0.75f,  // DEPRECATED: Initial page scale (use settings instead)
    settings: BrowserSettings? = null,  // Browser settings to apply
    isDesktopMode: Boolean = false,  // Whether tab is in desktop mode
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

    /**
     * Enable/disable auto-fit zoom for landscape mode
     * When enabled, automatically zooms out to fit full page width in landscape
     * @param enabled Whether to enable auto-fit zoom
     */
    fun setAutoFitZoom(enabled: Boolean)

    /**
     * Apply auto-fit zoom now (call when orientation changes to landscape)
     * Calculates and applies the appropriate zoom to show full page width
     */
    fun applyAutoFitZoom()

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

    // ========== Find in Page ==========

    /**
     * Find all occurrences of text in the page
     *
     * @param query Search query string
     * @param caseSensitive Whether to match case
     * @param onResultsFound Callback with (currentMatch, totalMatches)
     */
    fun findInPage(
        query: String,
        caseSensitive: Boolean,
        onResultsFound: (Int, Int) -> Unit
    )

    /**
     * Find next match in the page
     */
    fun findNext()

    /**
     * Find previous match in the page
     */
    fun findPrevious()

    /**
     * Clear find in page highlights
     */
    fun clearFindMatches()
}
