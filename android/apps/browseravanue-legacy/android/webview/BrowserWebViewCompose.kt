package com.augmentalis.browseravanue.webview

import android.net.http.SslError
import android.webkit.HttpAuthHandler
import android.webkit.SslErrorHandler
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.augmentalis.browseravanue.domain.model.Tab

/**
 * Compose wrapper for BrowserWebView
 *
 * Architecture:
 * - Clean Compose integration via AndroidView
 * - Exposes all WebView callbacks as Compose lambdas
 * - Remembers WebView instance across recompositions
 * - Lifecycle-aware (disposes WebView properly)
 *
 * Usage:
 * ```
 * BrowserWebViewCompose(
 *     tab = currentTab,
 *     onPageStarted = { url -> viewModel.onPageStarted(url) },
 *     onPageFinished = { url, title -> viewModel.onPageFinished(url, title) }
 * )
 * ```
 */
@Composable
fun BrowserWebViewCompose(
    tab: Tab,
    modifier: Modifier = Modifier,
    onPageStarted: (String) -> Unit = {},
    onPageFinished: (String, String?) -> Unit = { _, _ -> },
    onProgressChanged: (Int) -> Unit = {},
    onReceivedTitle: (String) -> Unit = {},
    onAuthenticationRequired: (String, String?, HttpAuthHandler) -> Unit = { _, _, handler -> handler.cancel() },
    onSslError: (SslError, SslErrorHandler) -> Unit = { _, handler -> handler.cancel() },
    onNewTab: (String) -> Unit = {},
    onDownloadStart: (String, String, String, String, Long) -> Unit = { _, _, _, _, _ -> }
) {
    AndroidView(
        factory = { context ->
            BrowserWebView(context).apply {
                // Set all callbacks
                setOnPageStarted(onPageStarted)
                setOnPageFinished(onPageFinished)
                setOnProgressChanged(onProgressChanged)
                setOnReceivedTitle(onReceivedTitle)
                setOnAuthenticationRequired(onAuthenticationRequired)
                setOnSslError(onSslError)
                setOnNewTab(onNewTab)
                setOnDownloadStart(onDownloadStart)

                // Load initial tab
                setCurrentTab(tab)
            }
        },
        update = { webView ->
            // Update tab when it changes
            webView.setCurrentTab(tab)

            // Update desktop mode if changed
            if (webView.settings.userAgentString?.contains("Windows") == true != tab.isDesktopMode) {
                webView.setDesktopMode(tab.isDesktopMode)
            }
        },
        modifier = modifier
    )
}

/**
 * Composable function to execute WebView actions
 *
 * Usage:
 * ```
 * val webViewActions = rememberWebViewActions()
 * Button(onClick = { webViewActions.scrollUp() }) { Text("Scroll Up") }
 * ```
 */
@Composable
fun rememberWebViewActions(): WebViewActions {
    val webViewRef = remember { mutableStateOf<BrowserWebView?>(null) }

    return remember {
        WebViewActions(webViewRef)
    }
}

/**
 * WebView action interface for Compose
 *
 * Provides type-safe actions that can be triggered from Compose UI
 */
class WebViewActions(private val webViewRef: MutableState<BrowserWebView?>) {

    // Navigation
    fun goBack() = webViewRef.value?.goBack()
    fun goForward() = webViewRef.value?.goForward()
    fun reload() = webViewRef.value?.reload()
    fun stopLoading() = webViewRef.value?.stopLoading()

    // Scroll
    fun scrollUp(amount: Int = 200) = webViewRef.value?.scrollUp(amount)
    fun scrollDown(amount: Int = 200) = webViewRef.value?.scrollDown(amount)
    fun scrollLeft(amount: Int = 200) = webViewRef.value?.scrollLeft(amount)
    fun scrollRight(amount: Int = 200) = webViewRef.value?.scrollRight(amount)
    fun scrollToTop() = webViewRef.value?.scrollToTop()
    fun scrollToBottom() = webViewRef.value?.scrollToBottom()

    // Zoom
    fun setZoomLevel(level: Int) = webViewRef.value?.setZoomLevel(level)
    fun zoomIn() = webViewRef.value?.zoomIn()
    fun zoomOut() = webViewRef.value?.zoomOut()

    // Desktop mode
    fun setDesktopMode(enabled: Boolean) = webViewRef.value?.setDesktopMode(enabled)

    // Clear data
    fun clearCache() = webViewRef.value?.clearBrowserCache()
    fun clearHistory() = webViewRef.value?.clearBrowserHistory()
    fun clearCookies() = webViewRef.value?.clearCookies()
    fun clearAllData() = webViewRef.value?.clearAllData()

    // Cookie management
    fun setAcceptCookies(accept: Boolean) = webViewRef.value?.setAcceptCookies(accept)
    fun setAcceptThirdPartyCookies(accept: Boolean) = webViewRef.value?.setAcceptThirdPartyCookies(accept)
}
