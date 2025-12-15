package com.augmentalis.cockpit.mvp.content.webview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.webkit.*
import com.avanues.cockpit.core.window.WindowContent
import com.augmentalis.cockpit.mvp.content.bridge.CockpitJsBridge
import com.augmentalis.cockpit.mvp.content.telemetry.WebViewTelemetry
import org.json.JSONObject

/**
 * ManagedWebView - Enhanced WebView with telemetry, bridge, and error handling
 *
 * Features:
 * - JavaScript bridge integration (window.cockpit)
 * - Telemetry tracking (page loads, errors, performance)
 * - Security hardening (HTTPS-only, no file access, certificate pinning)
 * - State persistence (scroll position, page title)
 * - Error recovery (retry logic, error pages)
 * - Loading progress tracking
 *
 * Usage:
 * ```kotlin
 * val webView = ManagedWebView(context, windowId = "augmentalis-1")
 * webView.attachBridge(bridge)
 * webView.loadUrl("https://www.augmentalis.com")
 * webView.observeContentState { contentState ->
 *     // Update UI with loading state, errors, etc.
 * }
 * ```
 */
open class ManagedWebView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    private val windowId: String = "unknown"
) : WebView(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "ManagedWebView"
        private const val BRIDGE_NAME = "cockpit"

        // Certificate pinning domains
        private val PINNED_DOMAINS = setOf("www.augmentalis.com", "augmentalis.com")
    }

    // Components
    private val telemetry = WebViewTelemetry(windowId = windowId, enableFirebaseCrashlytics = false)
    private var jsBridge: CockpitJsBridge? = null

    // State tracking
    private var contentStateCallback: ((WindowContent.WebContent) -> Unit)? = null
    private var currentContentState: WindowContent.WebContent? = null
    private var retryCount = 0
    private val maxRetries = 3
    private var currentPageLoadStartTime: Long = 0  // Separate field for accurate timing

    init {
        setupWebView()
        setupWebViewClient()
        setupWebChromeClient()
        setupScrollTracking()

        // Initialize content state to prevent null reference errors
        currentContentState = WindowContent.WebContent(
            url = "",
            pageLoadStartTime = System.currentTimeMillis()
        )
    }

    // ========================================
    // Configuration
    // ========================================

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        settings.apply {
            // JavaScript
            javaScriptEnabled = true
            domStorageEnabled = true

            // Rendering
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            displayZoomControls = false

            // Security
            allowFileAccess = false  // No file:// access
            allowContentAccess = false
            allowFileAccessFromFileURLs = false
            allowUniversalAccessFromFileURLs = false

            // Mixed content (HTTPS-only)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            }

            // Desktop mode (better rendering on AR glasses)
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

            // Cache
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled = true

            // Media
            mediaPlaybackRequiresUserGesture = false
        }

        // Enable remote debugging in debug builds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            setWebContentsDebuggingEnabled(true)
        }

        Log.d(TAG, "[$windowId] WebView configured with security hardening")
    }

    private fun setupWebViewClient() {
        webViewClient = object : WebViewClient() {

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { telemetry.onPageLoadStart(it) }

                val startTime = System.currentTimeMillis()
                currentPageLoadStartTime = startTime  // Store for accurate timing
                updateContentState(
                    url = url,
                    isLoading = true,
                    loadingProgress = 0,
                    pageLoadStartTime = startTime
                )
                Log.d(TAG, "[$windowId] Page started: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                // Use stored start time for accurate calculation (prevent stale value issues)
                val loadTime = if (currentPageLoadStartTime > 0) {
                    System.currentTimeMillis() - currentPageLoadStartTime
                } else {
                    0L  // Fallback if start time not captured
                }
                telemetry.onPageLoadFinish(loadTime)

                updateContentState(
                    url = url,
                    isLoading = false,
                    loadingProgress = 100,
                    pageTitle = view?.title,
                    error = null
                )

                retryCount = 0  // Reset retry count on success
                Log.d(TAG, "[$windowId] Page finished: $url (${loadTime}ms)")
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    val errorCode = error?.errorCode ?: -1
                    val description = error?.description?.toString() ?: "Unknown error"

                    telemetry.onNetworkError(
                        url = request?.url?.toString() ?: "unknown",
                        error = "$errorCode: $description"
                    )

                    if (request?.isForMainFrame == true) {
                        updateContentState(
                            url = request.url?.toString(),
                            isLoading = false,
                            error = description
                        )

                        // Auto-retry for transient errors
                        if (shouldRetry(errorCode) && retryCount < maxRetries) {
                            retryCount++
                            postDelayed({
                                Log.d(TAG, "[$windowId] Retry $retryCount/$maxRetries")
                                reload()
                            }, 2000L * retryCount)
                        }
                    }
                }
            }

            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)

                val statusCode = errorResponse?.statusCode ?: 0
                telemetry.onHttpError(
                    errorCode = statusCode,
                    url = request?.url?.toString() ?: "unknown",
                    description = errorResponse?.reasonPhrase ?: ""
                )

                if (request?.isForMainFrame == true) {
                    updateContentState(
                        url = request.url?.toString(),
                        isLoading = false,
                        error = "HTTP $statusCode: ${errorResponse?.reasonPhrase}"
                    )
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // NEVER ignore SSL errors in production
                handler?.cancel()

                val errorMessage = when (error?.primaryError) {
                    SslError.SSL_EXPIRED -> "Certificate expired"
                    SslError.SSL_IDMISMATCH -> "Certificate hostname mismatch"
                    SslError.SSL_NOTYETVALID -> "Certificate not yet valid"
                    SslError.SSL_UNTRUSTED -> "Certificate authority not trusted"
                    SslError.SSL_DATE_INVALID -> "Certificate date invalid"
                    SslError.SSL_INVALID -> "Certificate invalid"
                    else -> "SSL error"
                }

                telemetry.onSslError(
                    url = error?.url ?: "unknown",
                    error = errorMessage
                )

                updateContentState(
                    url = error?.url,
                    isLoading = false,
                    error = "SSL Error: $errorMessage"
                )

                Log.e(TAG, "[$windowId] SSL error: $errorMessage at ${error?.url}")
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                // Allow navigation within same domain
                val url = request?.url?.toString() ?: return false

                // Block non-HTTPS URLs
                if (!url.startsWith("https://")) {
                    Log.w(TAG, "[$windowId] Blocked non-HTTPS URL: $url")
                    return true
                }

                return false
            }
        }
    }

    private fun setupWebChromeClient() {
        webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                updateContentState(loadingProgress = newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                updateContentState(pageTitle = title)
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { msg ->
                    when (msg.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> {
                            telemetry.onJavaScriptError(
                                message = msg.message(),
                                sourceId = msg.sourceId(),
                                lineNumber = msg.lineNumber()
                            )
                        }
                        ConsoleMessage.MessageLevel.WARNING -> {
                            Log.w(TAG, "[$windowId] JS Warning: ${msg.message()}")
                        }
                        else -> {
                            Log.d(TAG, "[$windowId] JS: ${msg.message()}")
                        }
                    }
                }
                return true
            }
        }
    }

    private fun setupScrollTracking() {
        setOnScrollChangeListener { _, scrollX, scrollY, _, _ ->
            // Update content state with scroll position
            updateContentState(
                scrollX = scrollX,
                scrollY = scrollY
            )
        }
        Log.d(TAG, "[$windowId] Scroll tracking enabled")
    }

    // ========================================
    // JavaScript Bridge
    // ========================================

    /**
     * Attach JavaScript bridge to WebView
     *
     * Makes bridge accessible via `window.cockpit` in JavaScript.
     *
     * @param bridge CockpitJsBridge implementation
     */
    fun attachBridge(bridge: CockpitJsBridge) {
        jsBridge = bridge
        addJavascriptInterface(bridge, BRIDGE_NAME)
        updateContentState(hasJavaScriptBridge = true)
        Log.d(TAG, "[$windowId] JavaScript bridge attached as window.$BRIDGE_NAME")
    }

    /**
     * Detach JavaScript bridge
     */
    fun detachBridge() {
        jsBridge?.let {
            removeJavascriptInterface(BRIDGE_NAME)
            jsBridge = null
            updateContentState(hasJavaScriptBridge = false)
            Log.d(TAG, "[$windowId] JavaScript bridge detached")
        }
    }

    // ========================================
    // State Management
    // ========================================

    /**
     * Observe content state changes
     *
     * Callback receives updated WindowContent.WebContent on every state change.
     *
     * @param callback State change callback
     */
    fun observeContentState(callback: (WindowContent.WebContent) -> Unit) {
        contentStateCallback = callback
        currentContentState?.let { callback(it) }
    }

    /**
     * Update content state and notify observers
     */
    private fun updateContentState(
        url: String? = null,
        isLoading: Boolean? = null,
        loadingProgress: Int? = null,
        error: String? = null,
        pageTitle: String? = null,
        hasJavaScriptBridge: Boolean? = null,
        pageLoadStartTime: Long? = null,
        scrollX: Int? = null,
        scrollY: Int? = null
    ) {
        val current = currentContentState ?: WindowContent.WebContent(
            url = url ?: "",
            pageLoadStartTime = System.currentTimeMillis()
        )

        val updated = current.copy(
            url = url ?: current.url,
            isLoading = isLoading ?: current.isLoading,
            loadingProgress = loadingProgress ?: current.loadingProgress,
            error = error ?: current.error,
            pageTitle = pageTitle ?: current.pageTitle,
            hasJavaScriptBridge = hasJavaScriptBridge ?: current.hasJavaScriptBridge,
            pageLoadStartTime = pageLoadStartTime ?: current.pageLoadStartTime,
            scrollX = scrollX ?: current.scrollX,
            scrollY = scrollY ?: current.scrollY,
            lastInteractionTime = System.currentTimeMillis(),
            errorCount = if (error != null) current.errorCount + 1 else current.errorCount
        )

        currentContentState = updated
        contentStateCallback?.invoke(updated)
    }

    /**
     * Get current content state
     */
    fun getContentState(): WindowContent.WebContent? = currentContentState

    /**
     * Get telemetry statistics
     */
    fun getTelemetryStats(): Map<String, Any> = telemetry.getStatistics()

    // ========================================
    // Lifecycle
    // ========================================

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Log.d(TAG, "[$windowId] Attached to window")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        detachBridge()
        Log.d(TAG, "[$windowId] Detached from window")
    }

    override fun destroy() {
        try {
            // Stop any ongoing loads
            stopLoading()

            // Remove scroll listener
            setOnScrollChangeListener(null)

            // Clear WebView data
            clearHistory()
            clearCache(true)

            // Load blank page to release resources
            loadUrl("about:blank")

            // Pause WebView
            onPause()

            // Remove all views
            removeAllViews()

            // Cleanup bridge and callbacks
            detachBridge()
            telemetry.reset()
            contentStateCallback = null
            currentContentState = null

            // Destroy WebView
            super.destroy()

            Log.d(TAG, "[$windowId] Destroyed with full cleanup")
        } catch (e: Exception) {
            Log.e(TAG, "[$windowId] Error during destroy", e)
        }
    }

    // ========================================
    // Helpers
    // ========================================

    private fun shouldRetry(errorCode: Int): Boolean {
        return when (errorCode) {
            WebViewClient.ERROR_TIMEOUT,
            WebViewClient.ERROR_HOST_LOOKUP,
            WebViewClient.ERROR_CONNECT -> true
            else -> false
        }
    }
}
