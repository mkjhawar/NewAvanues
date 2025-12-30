package com.augmentalis.webavanue.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Cross-platform WebView interface for WebAvanue.
 * Platform-specific implementations provide the actual WebView rendering.
 */
interface WebView {
    /**
     * Current URL being displayed
     */
    val currentUrl: StateFlow<String>

    /**
     * Page title
     */
    val pageTitle: StateFlow<String>

    /**
     * Loading progress (0-100)
     */
    val loadingProgress: StateFlow<Int>

    /**
     * Whether the page is currently loading
     */
    val isLoading: StateFlow<Boolean>

    /**
     * Whether the user can go back
     */
    val canGoBack: StateFlow<Boolean>

    /**
     * Whether the user can go forward
     */
    val canGoForward: StateFlow<Boolean>

    /**
     * Loads a URL
     */
    fun loadUrl(url: String)

    /**
     * Reloads the current page
     */
    fun reload()

    /**
     * Stops loading the current page
     */
    fun stopLoading()

    /**
     * Goes back in history
     */
    fun goBack()

    /**
     * Goes forward in history
     */
    fun goForward()

    /**
     * Clears the browsing history
     */
    fun clearHistory()

    /**
     * Clears the cache
     */
    fun clearCache(includeDiskFiles: Boolean = false)

    /**
     * Evaluates JavaScript in the current page
     */
    suspend fun evaluateJavaScript(script: String): String?

    /**
     * Takes a screenshot of the current page
     */
    suspend fun captureScreenshot(): ByteArray?

    /**
     * Finds text in the page
     */
    fun findInPage(text: String)

    /**
     * Clears the find in page highlights
     */
    fun clearFindInPage()

    /**
     * Sets the user agent string
     */
    fun setUserAgent(userAgent: String)

    /**
     * Enables or disables JavaScript
     */
    fun setJavaScriptEnabled(enabled: Boolean)

    /**
     * Sets desktop mode
     */
    fun setDesktopMode(enabled: Boolean)

    /**
     * Zooms in
     */
    fun zoomIn()

    /**
     * Zooms out
     */
    fun zoomOut()

    /**
     * Resets zoom to default
     */
    fun resetZoom()

    /**
     * Disposes of resources
     */
    fun dispose()
}

/**
 * Factory for creating platform-specific WebView instances
 */
expect class WebViewFactory {
    fun createWebView(config: WebViewConfig): WebView
}

/**
 * Configuration for WebView
 */
data class WebViewConfig(
    val initialUrl: String = "about:blank",
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val userAgent: String? = null,
    val allowFileAccess: Boolean = false,
    val allowContentAccess: Boolean = false,
    val blockNetworkImage: Boolean = false,
    val blockNetworkLoads: Boolean = false,
    val cacheMode: CacheMode = CacheMode.DEFAULT,
    val mixedContentMode: MixedContentMode = MixedContentMode.NEVER,
    val safeBrowsingEnabled: Boolean = true,
    val useWideViewPort: Boolean = true,
    val loadWithOverviewMode: Boolean = true,
    val builtInZoomControls: Boolean = true,
    val displayZoomControls: Boolean = false,
    val textZoom: Int = 100
)

/**
 * Cache mode options
 */
enum class CacheMode {
    DEFAULT,
    NO_CACHE,
    CACHE_ONLY,
    CACHE_ELSE_NETWORK
}

/**
 * Mixed content mode options
 */
enum class MixedContentMode {
    ALWAYS,
    NEVER,
    COMPATIBILITY
}

/**
 * WebView events
 */
sealed class WebViewEvent {
    data class PageStarted(val url: String) : WebViewEvent()
    data class PageFinished(val url: String) : WebViewEvent()
    data class PageError(val errorCode: Int, val description: String, val failingUrl: String) : WebViewEvent()
    data class ProgressChanged(val progress: Int) : WebViewEvent()
    data class TitleReceived(val title: String) : WebViewEvent()
    data class DownloadRequest(val url: String, val mimeType: String?, val contentLength: Long) : WebViewEvent()
    data class ConsoleMessage(val message: String, val level: ConsoleLevel) : WebViewEvent()
    data class JsAlert(val message: String) : WebViewEvent()
    data class JsConfirm(val message: String) : WebViewEvent()
    data class JsPrompt(val message: String, val defaultValue: String?) : WebViewEvent()
    data class PermissionRequest(val permissions: List<Permission>) : WebViewEvent()
}

/**
 * Console message levels
 */
enum class ConsoleLevel {
    TIP,
    LOG,
    WARNING,
    ERROR,
    DEBUG
}

/**
 * Permission types
 */
enum class Permission {
    CAMERA,
    MICROPHONE,
    LOCATION,
    MIDI,
    PROTECTED_MEDIA
}

/**
 * Composable function for displaying a WebView
 */
@Composable
expect fun WebViewComposable(
    url: String,
    modifier: Modifier = Modifier,
    config: WebViewConfig = WebViewConfig(),
    onEvent: (WebViewEvent) -> Unit = {}
)