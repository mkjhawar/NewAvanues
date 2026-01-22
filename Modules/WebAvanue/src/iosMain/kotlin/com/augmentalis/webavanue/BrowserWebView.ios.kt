package com.augmentalis.webavanue

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import com.augmentalis.Avanues.web.data.domain.model.Tab
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.NSObject

/**
 * iOS WebView Implementation using WKWebView
 *
 * Integrates WKWebView via UIKitView for Compose Multiplatform
 */
@Composable
actual fun BrowserWebView(
    tab: Tab,
    desktopMode: Boolean,
    onUrlChanged: (String) -> Unit,
    onPageLoaded: (url: String, title: String?) -> Unit,
    modifier: Modifier
) {
    var webView: WKWebView? by remember { mutableStateOf(null) }

    UIKitView(
        factory = {
            val configuration = WKWebViewConfiguration()

            // Desktop mode user agent
            if (desktopMode) {
                configuration.applicationNameForUserAgent =
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
            }

            WKWebView(
                frame = platform.CoreGraphics.CGRectZero.readValue(),
                configuration = configuration
            ).apply {
                // Create navigation delegate
                val delegate = object : NSObject(), WKNavigationDelegateProtocol {
                    override fun webView(
                        webView: WKWebView,
                        didStartProvisionalNavigation: WKNavigation?
                    ) {
                        webView.URL?.absoluteString?.let { url ->
                            onUrlChanged(url)
                        }
                    }

                    override fun webView(
                        webView: WKWebView,
                        didFinishNavigation: WKNavigation?
                    ) {
                        val url = webView.URL?.absoluteString ?: ""
                        val title = webView.title
                        onPageLoaded(url, title)
                    }

                    override fun webView(
                        webView: WKWebView,
                        didFailNavigation: WKNavigation?,
                        withError: NSError
                    ) {
                        // Error handling
                        println("WKWebView navigation failed: ${withError.localizedDescription}")
                    }

                    override fun webView(
                        webView: WKWebView,
                        didFailProvisionalNavigation: WKNavigation?,
                        withError: NSError
                    ) {
                        // Provisional navigation error
                        println("WKWebView provisional navigation failed: ${withError.localizedDescription}")
                    }
                }

                navigationDelegate = delegate
                allowsBackForwardNavigationGestures = true

                // Load initial URL
                val url = NSURL.URLWithString(tab.url)
                if (url != null) {
                    val request = NSURLRequest.requestWithURL(url)
                    loadRequest(request)
                }

                webView = this
            }
        },
        update = { view ->
            // Update URL if changed
            val currentUrl = view.URL?.absoluteString
            if (currentUrl != tab.url) {
                val url = NSURL.URLWithString(tab.url)
                if (url != null) {
                    val request = NSURLRequest.requestWithURL(url)
                    view.loadRequest(request)
                }
            }

            // Update desktop mode if changed
            if (desktopMode && view.customUserAgent?.contains("Macintosh") != true) {
                view.customUserAgent =
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
                view.reload()
            } else if (!desktopMode && view.customUserAgent != null) {
                view.customUserAgent = null
                view.reload()
            }
        },
        modifier = modifier
    )
}
