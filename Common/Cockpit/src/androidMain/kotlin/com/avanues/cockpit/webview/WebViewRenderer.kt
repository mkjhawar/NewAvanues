package com.avanues.cockpit.webview

import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.window.WindowState
import com.augmentalis.voiceoslogger.VoiceOsLogger

/**
 * WebView Renderer Compose Component
 *
 * Renders web content inside Cockpit windows with full state persistence.
 * Supports voice commands via JavaScript injection and VoiceOS integration.
 *
 * **Features:**
 * - Automatic scroll/zoom restoration from WindowState
 * - JavaScript injection for voice commands
 * - YouTube detection and optimization
 * - Loading states with progress indicator
 * - Error handling with fallback UI
 *
 * **Voice-First Integration:**
 * - JavaScript injection bridge for voice commands
 * - State persistence for "Resume where I left off"
 * - Spatial audio positioning based on window location
 *
 * @param window The AppWindow to render (must be WEB_APP type)
 * @param onStateChange Callback when window state changes (scroll, zoom, etc.)
 * @param onUrlChange Callback when URL changes (for browser history)
 * @param onTitleChange Callback when page title changes (for window title)
 * @param modifier Compose modifier
 */
@Composable
fun WebViewRenderer(
    window: AppWindow,
    onStateChange: (WindowState) -> Unit,
    onUrlChange: ((String) -> Unit)? = null,
    onTitleChange: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableStateOf(0) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Track WebView instance
    var webView: WebView? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Main WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // Apply configuration based on URL type
                    if (WebViewConfig.isYouTubeVideo(window.sourceId)) {
                        WebViewConfig.applyYouTubeConfig(this)
                        VoiceOsLogger.d("WebViewRenderer", "Applied YouTube config for: ${window.sourceId}")
                    } else {
                        WebViewConfig.applyStandardConfig(this)
                    }

                    // Set up WebViewClient (handles navigation, errors)
                    webViewClient = object : WebViewClient() {
                        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                            hasError = false

                            VoiceOsLogger.d("WebViewRenderer", "Page started: $url")

                            // Notify URL change
                            url?.let { onUrlChange?.invoke(it) }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false

                            VoiceOsLogger.d("WebViewRenderer", "Page finished: $url")

                            // Restore scroll position from WindowState
                            view?.post {
                                view.scrollTo(window.state.scrollX, window.state.scrollY)
                                VoiceOsLogger.d(
                                    "WebViewRenderer",
                                    "Restored scroll: ${window.state.scrollX}, ${window.state.scrollY}"
                                )
                            }

                            // Restore zoom level from WindowState
                            if (window.state.zoomLevel != 1.0f) {
                                view?.post {
                                    view.setInitialScale((window.state.zoomLevel * 100).toInt())
                                    VoiceOsLogger.d(
                                        "WebViewRenderer",
                                        "Restored zoom: ${window.state.zoomLevel}"
                                    )
                                }
                            }

                            // Update page title
                            view?.title?.let { title ->
                                onTitleChange?.invoke(title)
                            }
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: WebResourceRequest?,
                            error: WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)

                            if (request?.isForMainFrame == true) {
                                hasError = true
                                errorMessage = error?.description?.toString()
                                    ?: "Failed to load page"
                                isLoading = false

                                VoiceOsLogger.e(
                                    "WebViewRenderer",
                                    "Page error: $errorMessage for ${request.url}"
                                )
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            // Let WebView handle all URLs (no external browser)
                            return false
                        }
                    }

                    // Set up WebChromeClient (handles loading progress, title)
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            loadProgress = newProgress

                            VoiceOsLogger.d(
                                "WebViewRenderer",
                                "Loading progress: $newProgress%"
                            )
                        }

                        override fun onReceivedTitle(view: WebView?, title: String?) {
                            super.onReceivedTitle(view, title)
                            title?.let {
                                onTitleChange?.invoke(it)
                                VoiceOsLogger.d("WebViewRenderer", "Title changed: $it")
                            }
                        }
                    }

                    // Add JavaScript interface for voice commands
                    addJavascriptInterface(
                        WebViewJavaScriptInterface(
                            webView = this,
                            onStateChange = onStateChange
                        ),
                        "webview"
                    )

                    // Load URL
                    loadUrl(window.sourceId)

                    // Store reference
                    webView = this
                }
            },
            update = { view ->
                // Update WebView when window properties change
                if (view.url != window.sourceId) {
                    view.loadUrl(window.sourceId)
                }
            }
        )

        // Loading indicator
        if (isLoading && !hasError) {
            CircularProgressIndicator(
                progress = { loadProgress / 100f },
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Error UI
        if (hasError) {
            Text(
                text = "Failed to load page\n$errorMessage",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // Scroll tracking - Save state when user scrolls
    DisposableEffect(webView) {
        val view = webView
        if (view != null) {
            // Set up scroll change listener
            view.viewTreeObserver.addOnScrollChangedListener {
                val newState = window.state.withScroll(view.scrollX, view.scrollY)
                onStateChange(newState)
            }
        }

        onDispose {
            // Cleanup
            webView?.destroy()
        }
    }
}

/**
 * JavaScript interface for WebView-to-Kotlin communication
 *
 * Enables JavaScript code to call Kotlin functions.
 * Used for voice command click simulation and state updates.
 *
 * @property webView The WebView instance
 * @property onStateChange Callback to update WindowState
 */
private class WebViewJavaScriptInterface(
    private val webView: WebView,
    private val onStateChange: (WindowState) -> Unit
) {
    /**
     * Called from JavaScript to simulate mouse click at coordinates
     *
     * Ported from JsCommands.kt doMouseClick() function.
     * More reliable than JavaScript click events for some elements.
     *
     * JavaScript usage:
     * ```javascript
     * window.webview.doMouseClick(0.5, 0.5); // Click center of viewport
     * ```
     *
     * @param xRatio X coordinate (0.0 to 1.0, relative to viewport width)
     * @param yRatio Y coordinate (0.0 to 1.0, relative to viewport height)
     */
    @android.webkit.JavascriptInterface
    fun doMouseClick(xRatio: Float, yRatio: Float) {
        JavaScriptInjector.clickAtCoordinates(webView, xRatio, yRatio)
    }

    /**
     * Called from JavaScript to notify state changes
     *
     * Enables JavaScript to update WindowState when it detects changes.
     *
     * JavaScript usage:
     * ```javascript
     * window.webview.notifyScrollChange(window.scrollX, window.scrollY);
     * ```
     *
     * @param scrollX Horizontal scroll position
     * @param scrollY Vertical scroll position
     */
    @android.webkit.JavascriptInterface
    fun notifyScrollChange(scrollX: Int, scrollY: Int) {
        // Get current state and update scroll
        webView.post {
            // This would require passing current window state
            // For now, we track scroll via viewTreeObserver (see WebViewRenderer)
            VoiceOsLogger.d(
                "WebViewJavaScriptInterface",
                "Scroll changed: $scrollX, $scrollY"
            )
        }
    }

    /**
     * Called from JavaScript to notify zoom changes
     *
     * JavaScript usage:
     * ```javascript
     * window.webview.notifyZoomChange(1.5); // 150% zoom
     * ```
     *
     * @param zoomLevel New zoom level (1.0 = 100%)
     */
    @android.webkit.JavascriptInterface
    fun notifyZoomChange(zoomLevel: Float) {
        webView.post {
            VoiceOsLogger.d(
                "WebViewJavaScriptInterface",
                "Zoom changed: $zoomLevel"
            )
        }
    }
}

/**
 * Extension function to execute voice commands on this WebView
 *
 * Convenience method for VoiceOS integration.
 *
 * Voice flow:
 * 1. VoiceOS recognizes "Click sign in"
 * 2. Cockpit calls webView.executeVoiceCommand("SIGN IN", "github.com")
 * 3. JavaScriptInjector looks up command and executes JS
 * 4. Button is clicked
 *
 * @param command Voice command (e.g., "SIGN IN")
 * @param domain Current domain (e.g., "github.com")
 * @return True if command was executed
 */
fun WebView.executeVoiceCommand(command: String, domain: String): Boolean {
    return JavaScriptInjector.executeVoiceCommand(this, command, domain)
}

/**
 * Extension function to get current domain
 *
 * @return Display-friendly domain name (e.g., "github.com")
 */
fun WebView.getCurrentDomain(): String {
    return WebViewConfig.getDisplayDomain(this.url)
}
