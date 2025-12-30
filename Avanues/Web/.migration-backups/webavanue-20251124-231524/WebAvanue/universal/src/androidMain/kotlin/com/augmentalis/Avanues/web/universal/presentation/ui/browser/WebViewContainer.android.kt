package com.augmentalis.Avanues.web.universal.presentation.ui.browser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/**
 * WebViewContainer - Android implementation
 *
 * Uses AndroidView to wrap Android WebView.
 *
 * Features:
 * - URL navigation and history
 * - JavaScript support
 * - Cookie management
 * - Progress tracking
 * - Custom user agent (desktop mode)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun WebViewContainer(
    url: String,
    controller: WebViewController?,
    onUrlChange: (String) -> Unit,
    onLoadingChange: (Boolean) -> Unit,
    onTitleChange: (String) -> Unit,
    onProgressChange: (Float) -> Unit,
    canGoBack: (Boolean) -> Unit,
    canGoForward: (Boolean) -> Unit,
    modifier: Modifier
) {
    var webView: WebView? by remember { mutableStateOf(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // FIX: Handle WebView lifecycle to prevent crashes when Home button is pressed
    DisposableEffect(lifecycleOwner) {
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
            // Pause and destroy WebView when composable is disposed
            webView?.onPause()
            webView?.pauseTimers()
            webView?.destroy()
        }
    }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
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

                // Load initial URL
                if (url.isNotBlank()) {
                    loadUrl(url)
                }

                // Save reference
                webView = this

                // Set up controller
                controller?.setWebView(this)
            }
        },
        update = { view ->
            // Update WebView when url changes
            if (url.isNotBlank() && view.url != url) {
                view.loadUrl(url)
            }
        },
        modifier = modifier
    )
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
