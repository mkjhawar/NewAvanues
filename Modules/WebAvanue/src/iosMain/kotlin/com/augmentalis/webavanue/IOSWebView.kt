package com.augmentalis.webavanue

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.*
import platform.UIKit.*
import platform.WebKit.*
import platform.CoreGraphics.*

/**
 * iOS implementation of WebView using WKWebView
 */
class IOSWebView(
    private val config: WebViewConfig
) : WebView {

    private val webView: WKWebView
    private val webViewConfiguration = WKWebViewConfiguration()
    private val navigationDelegate: WKNavigationDelegate
    private val uiDelegate: WKUIDelegate

    internal val _currentUrl = MutableStateFlow(config.initialUrl)
    override val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    internal val _pageTitle = MutableStateFlow("")
    override val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    internal val _loadingProgress = MutableStateFlow(0)
    override val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    internal val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    internal val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    internal val _canGoForward = MutableStateFlow(false)
    override val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    init {
        // Configure WKWebView
        setupWebViewConfiguration()

        // Create WKWebView
        webView = WKWebView(
            frame = CGRectMake(0.0, 0.0, 0.0, 0.0),
            configuration = webViewConfiguration
        )

        // Create and set delegates
        navigationDelegate = WebViewNavigationDelegate(this)
        uiDelegate = WebViewUIDelegate()

        webView.navigationDelegate = navigationDelegate
        webView.UIDelegate = uiDelegate

        // Setup KVO observers for properties
        setupObservers()

        // Apply initial configuration
        applyConfiguration()
    }

    private fun setupWebViewConfiguration() {
        webViewConfiguration.apply {
            // Enable JavaScript if configured
            preferences.javaScriptEnabled = config.javaScriptEnabled

            // Enable local storage
            websiteDataStore = if (config.domStorageEnabled) {
                WKWebsiteDataStore.defaultDataStore()
            } else {
                WKWebsiteDataStore.nonPersistentDataStore()
            }

            // Configure media playback
            allowsInlineMediaPlayback = true
            mediaTypesRequiringUserActionForPlayback = when (config.autoPlay) {
                AutoPlay.ALWAYS -> WKAudiovisualMediaTypes.None
                AutoPlay.NEVER -> WKAudiovisualMediaTypes.All
                else -> WKAudiovisualMediaTypes.Audio
            }

            // Security settings
            if (config.safeBrowsingEnabled) {
                preferences.isFraudulentWebsiteWarningEnabled = true
            }
        }
    }

    private fun applyConfiguration() {
        // Set user agent if provided
        config.userAgent?.let {
            webView.customUserAgent = it
        }

        // Set zoom scale
        val scale = when (config.fontSize) {
            BrowserSettings.FontSize.TINY -> 0.75
            BrowserSettings.FontSize.SMALL -> 0.875
            BrowserSettings.FontSize.MEDIUM -> 1.0
            BrowserSettings.FontSize.LARGE -> 1.125
            BrowserSettings.FontSize.HUGE -> 1.25
        }
        webView.pageZoom = scale
    }

    private fun setupObservers() {
        // Observe loading progress
        webView.addObserver(object : NSObject() {
            override fun observeValueForKeyPath(
                keyPath: String?,
                ofObject: Any?,
                change: Map<Any?, *>?,
                context: COpaquePointer?
            ) {
                when (keyPath) {
                    "estimatedProgress" -> {
                        val progress = (webView.estimatedProgress * 100).toInt()
                        _loadingProgress.value = progress
                    }
                    "loading" -> {
                        _isLoading.value = webView.loading
                    }
                    "title" -> {
                        _pageTitle.value = webView.title ?: ""
                    }
                    "URL" -> {
                        _currentUrl.value = webView.URL?.absoluteString ?: ""
                    }
                    "canGoBack" -> {
                        _canGoBack.value = webView.canGoBack
                    }
                    "canGoForward" -> {
                        _canGoForward.value = webView.canGoForward
                    }
                }
            }
        }, forKeyPath = "estimatedProgress", options = NSKeyValueObservingOptions.NSKeyValueObservingOptionNew, context = null)
    }

    override fun loadUrl(url: String) {
        val nsUrl = NSURL(string = url) ?: return
        val request = NSURLRequest(URL = nsUrl)
        webView.loadRequest(request)
    }

    override fun reload() {
        webView.reload()
    }

    override fun stopLoading() {
        webView.stopLoading()
    }

    override fun goBack() {
        if (webView.canGoBack) {
            webView.goBack()
        }
    }

    override fun goForward() {
        if (webView.canGoForward) {
            webView.goForward()
        }
    }

    override fun clearHistory() {
        // Clear back/forward list by loading blank page
        webView.loadHTMLString("", baseURL = null)
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        val dataTypes = setOf(
            WKWebsiteDataTypeDiskCache,
            WKWebsiteDataTypeMemoryCache,
            WKWebsiteDataTypeOfflineWebApplicationCache
        )

        val dataStore = WKWebsiteDataStore.defaultDataStore()
        val date = NSDate.dateWithTimeIntervalSince1970(0.0)

        dataStore.removeDataOfTypes(
            types = dataTypes,
            modifiedSince = date,
            completionHandler = {}
        )
    }

    override suspend fun evaluateJavaScript(script: String): String? {
        var result: String? = null
        webView.evaluateJavaScript(script) { value, error ->
            if (error == null && value != null) {
                result = value.toString()
            }
        }
        return result
    }

    override suspend fun captureScreenshot(): ByteArray? {
        // Take snapshot of WKWebView
        val configuration = WKSnapshotConfiguration()
        var imageData: ByteArray? = null

        webView.takeSnapshotWithConfiguration(configuration) { image, error ->
            if (error == null && image != null) {
                val pngData = UIImagePNGRepresentation(image)
                pngData?.let {
                    imageData = it.bytes?.readBytes(it.length.toInt())
                }
            }
        }

        return imageData
    }

    override fun findInPage(text: String) {
        // iOS doesn't have built-in find in page for WKWebView
        // Would need to implement with JavaScript
        val script = """
            window.find('$text', false, false, true, false, true, false);
        """
        webView.evaluateJavaScript(script, null)
    }

    override fun clearFindInPage() {
        // Clear selection with JavaScript
        val script = "window.getSelection().removeAllRanges();"
        webView.evaluateJavaScript(script, null)
    }

    override fun setUserAgent(userAgent: String) {
        webView.customUserAgent = userAgent
    }

    override fun setJavaScriptEnabled(enabled: Boolean) {
        webView.configuration.preferences.javaScriptEnabled = enabled
    }

    override fun setDesktopMode(enabled: Boolean) {
        val userAgent = if (enabled) {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15"
        } else {
            // Default iOS user agent
            null
        }
        webView.customUserAgent = userAgent
    }

    override fun zoomIn() {
        val currentZoom = webView.pageZoom
        webView.pageZoom = (currentZoom * 1.1).coerceAtMost(3.0)
    }

    override fun zoomOut() {
        val currentZoom = webView.pageZoom
        webView.pageZoom = (currentZoom * 0.9).coerceAtLeast(0.5)
    }

    override fun resetZoom() {
        webView.pageZoom = 1.0
    }

    override fun dispose() {
        webView.stopLoading()
        webView.removeFromSuperview()
    }

    fun getUIView(): UIView = webView
}

/**
 * Navigation delegate for handling page navigation events
 */
private class WebViewNavigationDelegate(
    private val webView: IOSWebView
) : NSObject(), WKNavigationDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        didStartProvisionalNavigation: WKNavigation?
    ) {
        // Page started loading
        webView.URL?.absoluteString?.let {
            this.webView._currentUrl.value = it
        }
        this.webView._isLoading.value = true
    }

    override fun webView(
        webView: WKWebView,
        didFinishNavigation: WKNavigation?
    ) {
        // Page finished loading
        this.webView._isLoading.value = false
        webView.title?.let {
            this.webView._pageTitle.value = it
        }
        this.webView.updateNavigationState()
    }

    override fun webView(
        webView: WKWebView,
        didFailProvisionalNavigation: WKNavigation?,
        withError: NSError
    ) {
        // Page failed to load
        this.webView._isLoading.value = false
    }

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit
    ) {
        // Allow all navigation by default
        decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
    }
}

/**
 * UI delegate for handling JavaScript alerts, confirms, etc.
 */
private class WebViewUIDelegate : NSObject(), WKUIDelegateProtocol {

    override fun webView(
        webView: WKWebView,
        runJavaScriptAlertPanelWithMessage: String,
        initiatedByFrame: WKFrameInfo,
        completionHandler: () -> Unit
    ) {
        // Handle JavaScript alert
        val alertController = UIAlertController.alertControllerWithTitle(
            title = "Alert",
            message = runJavaScriptAlertPanelWithMessage,
            preferredStyle = UIAlertControllerStyle.UIAlertControllerStyleAlert
        )

        alertController.addAction(
            UIAlertAction.actionWithTitle(
                title = "OK",
                style = UIAlertActionStyle.UIAlertActionStyleDefault,
                handler = { completionHandler() }
            )
        )

        // Present alert (would need view controller reference)
        completionHandler()
    }

    override fun webView(
        webView: WKWebView,
        runJavaScriptConfirmPanelWithMessage: String,
        initiatedByFrame: WKFrameInfo,
        completionHandler: (Boolean) -> Unit
    ) {
        // Handle JavaScript confirm
        completionHandler(true)
    }

    override fun webView(
        webView: WKWebView,
        runJavaScriptTextInputPanelWithPrompt: String,
        defaultText: String?,
        initiatedByFrame: WKFrameInfo,
        completionHandler: (String?) -> Unit
    ) {
        // Handle JavaScript prompt
        completionHandler(defaultText)
    }
}

/**
 * Factory for creating iOS WebView
 */
actual class WebViewFactory {
    actual fun createWebView(config: WebViewConfig): WebView {
        return IOSWebView(config)
    }
}

/**
 * iOS Composable for WebView
 */
@Composable
actual fun WebViewComposable(
    url: String,
    modifier: Modifier,
    config: WebViewConfig,
    onEvent: (WebViewEvent) -> Unit
) {
    val iosWebView = remember { IOSWebView(config) }

    DisposableEffect(url) {
        iosWebView.loadUrl(url)
        onDispose {
            iosWebView.dispose()
        }
    }

    UIKitView(
        factory = { iosWebView.getUIView() },
        modifier = modifier,
        update = { view ->
            // Update view if needed
        }
    )
}

// Extension functions for IOSWebView
private fun IOSWebView.updateNavigationState() {
    _canGoBack.value = webView.canGoBack
    _canGoForward.value = webView.canGoForward
}