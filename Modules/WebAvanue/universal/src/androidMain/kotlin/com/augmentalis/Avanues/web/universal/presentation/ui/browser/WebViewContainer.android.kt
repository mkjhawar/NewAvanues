package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * WebViewPoolManager - Android implementation
 * Provides access to the internal WebViewPool for cleanup operations.
 */
actual object WebViewPoolManager {
    actual fun removeWebView(tabId: String) {
        WebViewPool.remove(tabId)
    }

    actual fun clearAllWebViews() {
        WebViewPool.clear()
    }
}

/**
 * WebViewPool - Manages WebView instances per tab ID
 *
 * Maintains WebView instances across tab switches to preserve navigation history.
 * This prevents the recreation of WebViews when switching between tabs.
 *
 * FIX: Issue #1 - Tab History Lost When Switching Tabs
 * Previously, key(tabState.tab.id) in BrowserScreen caused WebView recreation on every tab switch.
 * Now we cache WebView instances by tab ID, preserving navigation history.
 */
private object WebViewPool {
    private val webViews = mutableMapOf<String, WebView>()

    /**
     * Get or create WebView for a tab ID
     */
    fun getOrCreate(tabId: String, context: Context, factory: (Context) -> WebView): WebView {
        return webViews.getOrPut(tabId) {
            factory(context)
        }
    }

    /**
     * Remove and destroy WebView for a tab ID
     * Call this when a tab is closed
     */
    fun remove(tabId: String) {
        webViews.remove(tabId)?.let { webView ->
            webView.onPause()
            webView.pauseTimers()
            webView.destroy()
        }
    }

    /**
     * Clear all WebViews (for cleanup)
     */
    fun clear() {
        webViews.values.forEach { webView ->
            webView.onPause()
            webView.pauseTimers()
            webView.destroy()
        }
        webViews.clear()
    }

    /**
     * Get existing WebView for a tab ID (without creating)
     */
    fun get(tabId: String): WebView? {
        return webViews[tabId]
    }
}

/**
 * WebViewContainer - Android implementation
 *
 * Uses AndroidView to wrap Android WebView.
 *
 * Features:
 * - URL navigation and history preservation across tab switches
 * - JavaScript support
 * - Cookie management
 * - Progress tracking
 * - Custom user agent (desktop mode)
 *
 * FIX: Tab history is now preserved by reusing WebView instances from the pool
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebViewContainer(
    tabId: String,
    url: String,
    controller: WebViewController?,
    onUrlChange: (String) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onProgressChange: (Float) -> Unit,
    canGoBack: (Boolean) -> Unit,
    canGoForward: (Boolean) -> Unit,
    onDownloadStart: (url: String, filename: String, mimeType: String) -> Unit,
    modifier: Modifier
) {
    val context = LocalContext.current
    var webView: WebView? by remember(tabId) { mutableStateOf(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // FIX ISSUE #1: Use key(tabId) to force AndroidView recreation when tab changes
    // This ensures WebViewPool.getOrCreate is called for each tab, preserving navigation history
    // Without key(), the factory lambda only runs once and never switches to other tab's WebView
    key(tabId) {
        // FIX: Handle WebView lifecycle to prevent crashes when Home button is pressed
        DisposableEffect(lifecycleOwner, tabId) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        // Pause WebView when activity goes to background
                        webView?.onPause()
                        webView?.pauseTimers()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        // Resume WebView when activity comes to foreground
                        webView?.onResume()
                        webView?.resumeTimers()
                    }
                    else -> {}
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                // NOTE: We don't destroy the WebView here - it's managed by the pool
                // WebView will be destroyed when the tab is closed (via WebViewPool.remove())
            }
        }

        AndroidView(
        factory = { factoryContext ->
            // FIX: Use WebViewPool to get or create WebView for this tab
            // This preserves navigation history when switching between tabs
            WebViewPool.getOrCreate(tabId, factoryContext) { ctx ->
                WebView(ctx).apply {
                    // Set layout params to respect parent constraints
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    // FIX: WebView settings - ensure proper initialization for Android 16
                    settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    cacheMode = WebSettings.LOAD_DEFAULT

                    // Additional settings for compatibility
                    allowFileAccess = false // Security: disable file access
                    allowContentAccess = true
                    javaScriptCanOpenWindowsAutomatically = false
                    mediaPlaybackRequiresUserGesture = false

                    // Mixed content mode for HTTPS compatibility
                    @Suppress("DEPRECATION")
                    mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE

                    // ========== WebXR Support ==========
                    // REQ-XR-001: Enable WebXR Device API support
                    // REQ-XR-006: WebGL 2.0 Support for XR Rendering

                    // WebGL/OpenGL ES support (required for WebXR)
                    // Note: WebGL 2.0 maps to OpenGL ES 3.0 on Android
                    setRenderPriority(WebSettings.RenderPriority.HIGH)

                    // Enable hardware acceleration for GPU-accelerated rendering
                    // This is critical for maintaining 60fps in XR sessions
                    // Hardware acceleration uses OpenGL ES for rendering

                    // Storage APIs (required for WebXR session persistence)
                    // DOM storage already enabled above, but critical for XR

                    // Allow auto-play for XR sessions (no user gesture required)
                    // Already set to false above (mediaPlaybackRequiresUserGesture = false)
                }

                // WebViewClient (handles page navigation)
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        onLoadingChange(true)
                        url?.let { onUrlChange(it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        onLoadingChange(false)
                        view?.let {
                            canGoBack(it.canGoBack())
                            canGoForward(it.canGoForward())
                            it.title?.let { title -> onTitleChange(title) }
                        }
                    }

                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        // Let WebView handle the URL
                        return false
                    }
                }

                // WebChromeClient (handles JavaScript dialogs, progress, etc.)
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        onProgressChange(newProgress / 100f)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { onTitleChange(it) }
                    }

                    override fun onJsAlert(
                        view: WebView?,
                        url: String?,
                        message: String?,
                        result: JsResult?
                    ): Boolean {
                        // Handle JavaScript alerts
                        // For now, just confirm
                        result?.confirm()
                        return true
                    }

                    // ========== WebXR Permission Requests ==========
                    // REQ-XR-002: Camera Permission Management for AR sessions

                    override fun onPermissionRequest(request: PermissionRequest?) {
                        request?.let {
                            // WebXR requests camera permission via this API
                            // Resources requested: RESOURCE_VIDEO_CAPTURE (camera for AR)
                            val requestedResources = it.resources

                            // Check if camera is requested (for AR sessions)
                            if (requestedResources.contains(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                                // Grant permission if we have runtime permission
                                // TODO: Integrate with XRPermissionManager to check runtime permission
                                // For now, grant to allow WebXR API to be available
                                it.grant(requestedResources)
                            } else {
                                // Grant other permissions (audio, etc.)
                                it.grant(requestedResources)
                            }
                        }
                    }
                }

                // FIX BUG: Add download listener to handle file downloads
                setDownloadListener { downloadUrl, userAgent, contentDisposition, mimeType, contentLength ->
                    try {
                        val filename = android.webkit.URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)
                        println("WebViewContainer: Download started - $filename ($mimeType)")

                        // Notify the callback for ViewModel tracking
                        onDownloadStart(downloadUrl, filename, mimeType ?: "application/octet-stream")

                        // Use Android DownloadManager for actual download
                        val request = android.app.DownloadManager.Request(android.net.Uri.parse(downloadUrl))
                            .setTitle(filename)
                            .setDescription("Downloading file...")
                            .setMimeType(mimeType)
                            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, filename)
                            .addRequestHeader("User-Agent", userAgent)

                        val downloadManager = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                        downloadManager.enqueue(request)

                        android.widget.Toast.makeText(context, "Downloading: $filename", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        println("WebViewContainer: Download failed - ${e.message}")
                        android.widget.Toast.makeText(context, "Download failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }

                // Load initial URL (only for new WebViews)
                if (url.isNotBlank() && this.url == null) {
                    loadUrl(url)
                }

                // Save reference
                webView = this

                // Set up controller
                controller?.setWebView(this)
                }
            }.also { view ->
                // Save reference for lifecycle management
                webView = view
                // Set up controller
                controller?.setWebView(view)
                // Update navigation state
                canGoBack(view.canGoBack())
                canGoForward(view.canGoForward())
            }
        },
        update = { view ->
            // Update WebView when url changes (but don't reload if it's already the current URL)
            if (url.isNotBlank() && view.url != url) {
                view.loadUrl(url)
            }
            // Update controller reference (in case it changed)
            controller?.setWebView(view)
            // Update navigation state
            canGoBack(view.canGoBack())
            canGoForward(view.canGoForward())
        },
            modifier = modifier
        )
    } // End of key(tabId) block
}

/**
 * WebViewController - Android implementation
 */
actual class WebViewController {
    private var webView: WebView? = null

    /**
     * Set WebView instance (called from WebViewContainer)
     */
    fun setWebView(webView: WebView) {
        this.webView = webView
    }

    actual fun goBack() {
        webView?.goBack()
    }

    actual fun goForward() {
        webView?.goForward()
    }

    actual fun reload() {
        webView?.reload()
    }

    actual fun stopLoading() {
        webView?.stopLoading()
    }

    actual fun loadUrl(url: String) {
        webView?.loadUrl(url)
    }

    @SuppressLint("JavascriptInterface")
    actual fun evaluateJavaScript(script: String, callback: (String?) -> Unit) {
        webView?.evaluateJavascript(script) { result ->
            callback(result)
        }
    }

    actual fun clearCache() {
        webView?.clearCache(true)
    }

    actual fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    actual fun clearHistory() {
        webView?.clearHistory()
    }

    actual fun setUserAgent(userAgent: String) {
        webView?.settings?.userAgentString = userAgent
    }

    actual fun setJavaScriptEnabled(enabled: Boolean) {
        webView?.settings?.javaScriptEnabled = enabled
    }

    actual fun setCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptCookie(enabled)
    }

    actual fun setDesktopMode(enabled: Boolean) {
        webView?.settings?.apply {
            if (enabled) {
                // Desktop user agent (Chrome on Windows)
                userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Safari/537.36"
                useWideViewPort = true
                loadWithOverviewMode = true
            } else {
                // Reset to default (mobile)
                userAgentString = null
            }
        }
        // FIX: Reload page to apply new user agent
        webView?.reload()
    }

    // ========== Scrolling Controls ==========

    private var currentZoomLevel = 3 // Default zoom level (100%)

    actual fun scrollUp() {
        webView?.let { view ->
            val scrollAmount = view.height / 2
            view.scrollBy(0, -scrollAmount)
        }
    }

    actual fun scrollDown() {
        webView?.let { view ->
            val scrollAmount = view.height / 2
            view.scrollBy(0, scrollAmount)
        }
    }

    actual fun scrollLeft() {
        webView?.let { view ->
            val scrollAmount = view.width / 2
            view.scrollBy(-scrollAmount, 0)
        }
    }

    actual fun scrollRight() {
        webView?.let { view ->
            val scrollAmount = view.width / 2
            view.scrollBy(scrollAmount, 0)
        }
    }

    actual fun scrollToTop() {
        webView?.scrollTo(0, 0)
    }

    actual fun scrollToBottom() {
        webView?.let { view ->
            view.evaluateJavascript("window.scrollTo(0, document.body.scrollHeight);", null)
        }
    }

    // ========== Zoom Controls ==========

    actual fun zoomIn() {
        webView?.let { view ->
            view.settings.textZoom = (view.settings.textZoom * 1.1).toInt()
            if (currentZoomLevel < 5) currentZoomLevel++
        }
    }

    actual fun zoomOut() {
        webView?.let { view ->
            view.settings.textZoom = (view.settings.textZoom * 0.9).toInt()
            if (currentZoomLevel > 1) currentZoomLevel--
        }
    }

    actual fun setZoomLevel(level: Int) {
        webView?.let { view ->
            currentZoomLevel = level.coerceIn(1, 5)
            // Map levels 1-5 to text zoom percentages (50%, 75%, 100%, 125%, 150%)
            val zoomPercent = when (currentZoomLevel) {
                1 -> 50
                2 -> 75
                3 -> 100
                4 -> 125
                5 -> 150
                else -> 100
            }
            view.settings.textZoom = zoomPercent
        }
    }

    actual fun getZoomLevel(): Int = currentZoomLevel

    // ========== Touch/Interaction Controls ==========

    actual fun setScrollFrozen(frozen: Boolean) {
        webView?.let { view ->
            if (frozen) {
                // Disable scrolling
                view.setOnTouchListener { _, _ -> true }
            } else {
                // Re-enable scrolling
                view.setOnTouchListener(null)
            }
        }
    }

    actual fun performClick() {
        webView?.let { view ->
            val x = view.width / 2f
            val y = view.height / 2f
            val downTime = android.os.SystemClock.uptimeMillis()
            val eventTime = android.os.SystemClock.uptimeMillis()

            val downEvent = android.view.MotionEvent.obtain(
                downTime, eventTime,
                android.view.MotionEvent.ACTION_DOWN,
                x, y, 0
            )
            val upEvent = android.view.MotionEvent.obtain(
                downTime, eventTime + 100,
                android.view.MotionEvent.ACTION_UP,
                x, y, 0
            )

            view.dispatchTouchEvent(downEvent)
            view.dispatchTouchEvent(upEvent)

            downEvent.recycle()
            upEvent.recycle()
        }
    }

    actual fun performDoubleClick() {
        webView?.let { view ->
            val x = view.width / 2f
            val y = view.height / 2f
            val downTime = android.os.SystemClock.uptimeMillis()

            // First click
            val downEvent1 = android.view.MotionEvent.obtain(
                downTime, downTime,
                android.view.MotionEvent.ACTION_DOWN,
                x, y, 0
            )
            val upEvent1 = android.view.MotionEvent.obtain(
                downTime, downTime + 50,
                android.view.MotionEvent.ACTION_UP,
                x, y, 0
            )

            // Second click
            val downEvent2 = android.view.MotionEvent.obtain(
                downTime + 100, downTime + 100,
                android.view.MotionEvent.ACTION_DOWN,
                x, y, 0
            )
            val upEvent2 = android.view.MotionEvent.obtain(
                downTime + 100, downTime + 150,
                android.view.MotionEvent.ACTION_UP,
                x, y, 0
            )

            view.dispatchTouchEvent(downEvent1)
            view.dispatchTouchEvent(upEvent1)
            view.dispatchTouchEvent(downEvent2)
            view.dispatchTouchEvent(upEvent2)

            downEvent1.recycle()
            upEvent1.recycle()
            downEvent2.recycle()
            upEvent2.recycle()
        }
    }
}
