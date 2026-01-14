package com.augmentalis.webavanue.platform

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream

/**
 * Android implementation of WebView using android.webkit.WebView
 */
class AndroidWebView(
    private val webView: android.webkit.WebView,
    private val config: WebViewConfig
) : WebView {

    private val _currentUrl = MutableStateFlow(config.initialUrl)
    override val currentUrl: StateFlow<String> = _currentUrl.asStateFlow()

    private val _pageTitle = MutableStateFlow("")
    override val pageTitle: StateFlow<String> = _pageTitle.asStateFlow()

    private val _loadingProgress = MutableStateFlow(0)
    override val loadingProgress: StateFlow<Int> = _loadingProgress.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canGoBack = MutableStateFlow(false)
    override val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    private val _canGoForward = MutableStateFlow(false)
    override val canGoForward: StateFlow<Boolean> = _canGoForward.asStateFlow()

    init {
        setupWebView()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = config.javaScriptEnabled
            domStorageEnabled = config.domStorageEnabled
            allowFileAccess = config.allowFileAccess
            allowContentAccess = config.allowContentAccess
            blockNetworkImage = config.blockNetworkImage
            blockNetworkLoads = config.blockNetworkLoads
            cacheMode = when (config.cacheMode) {
                CacheMode.DEFAULT -> android.webkit.WebSettings.LOAD_DEFAULT
                CacheMode.NO_CACHE -> android.webkit.WebSettings.LOAD_NO_CACHE
                CacheMode.CACHE_ONLY -> android.webkit.WebSettings.LOAD_CACHE_ONLY
                CacheMode.CACHE_ELSE_NETWORK -> android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
            }
            mixedContentMode = when (config.mixedContentMode) {
                MixedContentMode.ALWAYS -> android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                MixedContentMode.NEVER -> android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                MixedContentMode.COMPATIBILITY -> android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            }
            safeBrowsingEnabled = config.safeBrowsingEnabled
            useWideViewPort = config.useWideViewPort
            loadWithOverviewMode = config.loadWithOverviewMode
            builtInZoomControls = config.builtInZoomControls
            displayZoomControls = config.displayZoomControls
            textZoom = config.textZoom
            config.userAgent?.let { userAgentString = it }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let {
                    _currentUrl.value = it
                    _isLoading.value = true
                }
            }

            override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                super.onPageFinished(view, url)
                _isLoading.value = false
                updateNavigationState()
            }

            override fun onReceivedError(
                view: android.webkit.WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                _isLoading.value = false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: android.webkit.WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                _loadingProgress.value = newProgress
            }

            override fun onReceivedTitle(view: android.webkit.WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { _pageTitle.value = it }
            }
        }
    }

    private fun updateNavigationState() {
        _canGoBack.value = webView.canGoBack()
        _canGoForward.value = webView.canGoForward()
    }

    override fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun reload() {
        webView.reload()
    }

    override fun stopLoading() {
        webView.stopLoading()
    }

    override fun goBack() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    override fun goForward() {
        if (webView.canGoForward()) {
            webView.goForward()
        }
    }

    override fun clearHistory() {
        webView.clearHistory()
    }

    override fun clearCache(includeDiskFiles: Boolean) {
        webView.clearCache(includeDiskFiles)
    }

    override suspend fun evaluateJavaScript(script: String): String? {
        var result: String? = null
        webView.evaluateJavascript(script) { value ->
            result = value
        }
        return result
    }

    override suspend fun captureScreenshot(): ByteArray? {
        return try {
            val bitmap = Bitmap.createBitmap(webView.width, webView.height, Bitmap.Config.ARGB_8888)
            webView.draw(android.graphics.Canvas(bitmap))
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    override fun findInPage(text: String) {
        webView.findAllAsync(text)
    }

    override fun clearFindInPage() {
        webView.clearMatches()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun setJavaScriptEnabled(enabled: Boolean) {
        webView.settings.javaScriptEnabled = enabled
    }

    override fun setUserAgent(userAgent: String) {
        webView.settings.userAgentString = userAgent
    }

    override fun setDesktopMode(enabled: Boolean) {
        val newUserAgent = if (enabled) {
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        } else {
            webView.settings.userAgentString
        }
        webView.settings.userAgentString = newUserAgent
        webView.settings.useWideViewPort = enabled
        webView.settings.loadWithOverviewMode = enabled
        webView.reload()
    }

    override fun zoomIn() {
        webView.zoomIn()
    }

    override fun zoomOut() {
        webView.zoomOut()
    }

    override fun resetZoom() {
        webView.settings.textZoom = 100
    }

    override fun dispose() {
        webView.destroy()
    }
}

/**
 * Android implementation of WebViewFactory
 */
actual class WebViewFactory {
    actual fun createWebView(config: WebViewConfig): WebView {
        throw NotImplementedError("Use WebViewComposable instead for Android")
    }
}

/**
 * Android Composable for WebView
 */
@Composable
actual fun WebViewComposable(
    url: String,
    modifier: Modifier,
    config: WebViewConfig,
    onEvent: (WebViewEvent) -> Unit
) {
    val context = LocalContext.current

    val webView = remember {
        android.webkit.WebView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    val androidWebView = remember { AndroidWebView(webView, config) }

    DisposableEffect(Unit) {
        androidWebView.loadUrl(url)
        onDispose {
            androidWebView.dispose()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier
    )
}