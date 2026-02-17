package com.augmentalis.webavanue

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.*
import platform.Foundation.*
import platform.WebKit.*
import platform.darwin.NSObject

/**
 * iOS WebViewContainer implementation using WKWebView
 *
 * Wraps WKWebView in UIKitView for Compose integration
 */
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
    onOpenInNewTab: (String) -> Unit,
    sessionData: String?,
    onSessionDataChange: (String?) -> Unit,
    securityViewModel: SecurityViewModel?,
    onDownloadStart: ((WebViewDownloadInfo) -> Unit)?,
    initialScale: Float,
    settings: BrowserSettings?,
    isDesktopMode: Boolean,
    voiceOSCallback: VoiceOSWebCallback?,
    modifier: Modifier
) {
    var webView: WKWebView? by remember { mutableStateOf(null) }
    var currentUrl by remember { mutableStateOf(url) }

    // Create navigation delegate
    val navigationDelegate = remember {
        object : NSObject(), WKNavigationDelegateProtocol {
            override fun webView(
                webView: WKWebView,
                didStartProvisionalNavigation: WKNavigation?
            ) {
                onLoadingChange(true)
                webView.URL?.absoluteString?.let { newUrl ->
                    currentUrl = newUrl
                    onUrlChange(newUrl)
                }
            }

            override fun webView(
                webView: WKWebView,
                didFinishNavigation: WKNavigation?
            ) {
                onLoadingChange(false)
                webView.title?.let { title ->
                    onTitleChange(title)
                }
                canGoBack(webView.canGoBack)
                canGoForward(webView.canGoForward)
            }

            override fun webView(
                webView: WKWebView,
                didFailNavigation: WKNavigation?,
                withError: NSError
            ) {
                onLoadingChange(false)
                println("WKWebView navigation failed: ${withError.localizedDescription}")
            }

            override fun webView(
                webView: WKWebView,
                didFailProvisionalNavigation: WKNavigation?,
                withError: NSError
            ) {
                onLoadingChange(false)
                println("WKWebView provisional navigation failed: ${withError.localizedDescription}")
            }

            override fun webView(
                webView: WKWebView,
                decidePolicyForNavigationAction: WKNavigationAction,
                decisionHandler: (WKNavigationActionPolicy) -> Unit
            ) {
                // Handle target="_blank" links
                if (decidePolicyForNavigationAction.targetFrame == null) {
                    decidePolicyForNavigationAction.request.URL?.absoluteString?.let { newUrl ->
                        onOpenInNewTab(newUrl)
                    }
                    decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyCancel)
                } else {
                    decisionHandler(WKNavigationActionPolicy.WKNavigationActionPolicyAllow)
                }
            }
        }
    }

    UIKitView(
        factory = {
            val configuration = WKWebViewConfiguration()

            // Apply settings
            settings?.let { s ->
                configuration.preferences.javaScriptEnabled = s.javaScriptEnabled
                configuration.allowsInlineMediaPlayback = true
                configuration.mediaTypesRequiringUserActionForPlayback = when (s.autoPlay) {
                    AutoPlay.ALWAYS -> WKAudiovisualMediaTypes.None
                    AutoPlay.NEVER -> WKAudiovisualMediaTypes.All
                    else -> WKAudiovisualMediaTypes.Audio
                }
            } ?: run {
                configuration.preferences.javaScriptEnabled = true
                configuration.allowsInlineMediaPlayback = true
            }

            // Desktop mode user agent
            if (isDesktopMode) {
                configuration.applicationNameForUserAgent =
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
            }

            WKWebView(
                frame = platform.CoreGraphics.CGRectZero.readValue(),
                configuration = configuration
            ).apply {
                navigationDelegate = navigationDelegate
                allowsBackForwardNavigationGestures = true

                // Apply initial zoom scale
                pageZoom = initialScale.toDouble()

                // Apply font size from settings
                settings?.let { s ->
                    val scale = when (s.fontSize) {
                        BrowserSettings.FontSize.TINY -> 0.75
                        BrowserSettings.FontSize.SMALL -> 0.875
                        BrowserSettings.FontSize.MEDIUM -> 1.0
                        BrowserSettings.FontSize.LARGE -> 1.125
                        BrowserSettings.FontSize.HUGE -> 1.25
                    }
                    pageZoom = scale
                }

                // Add progress observer
                addObserver(
                    observer = object : NSObject() {
                        override fun observeValueForKeyPath(
                            keyPath: String?,
                            ofObject: Any?,
                            change: Map<Any?, *>?,
                            context: COpaquePointer?
                        ) {
                            when (keyPath) {
                                "estimatedProgress" -> {
                                    val progress = this@apply.estimatedProgress.toFloat()
                                    onProgressChange(progress)
                                }
                            }
                        }
                    },
                    forKeyPath = "estimatedProgress",
                    options = NSKeyValueObservingOptionNew,
                    context = null
                )

                // Load initial URL
                val nsUrl = NSURL.URLWithString(url)
                if (nsUrl != null) {
                    val request = NSURLRequest.requestWithURL(nsUrl)
                    loadRequest(request)
                }

                // Attach to controller if provided
                controller?.let { ctrl ->
                    if (ctrl is WebViewController) {
                        ctrl.attachWebView(this)
                    }
                }

                webView = this
            }
        },
        update = { view ->
            // Update URL if changed
            val viewUrl = view.URL?.absoluteString
            if (viewUrl != url && url.isNotEmpty()) {
                val nsUrl = NSURL.URLWithString(url)
                if (nsUrl != null) {
                    val request = NSURLRequest.requestWithURL(nsUrl)
                    view.loadRequest(request)
                    currentUrl = url
                }
            }

            // Update desktop mode if changed
            if (isDesktopMode && view.customUserAgent?.contains("Macintosh") != true) {
                view.customUserAgent =
                    "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15"
                view.reload()
            } else if (!isDesktopMode && view.customUserAgent != null) {
                view.customUserAgent = null
                view.reload()
            }

            // Apply settings updates
            settings?.let { s ->
                view.configuration.preferences.javaScriptEnabled = s.javaScriptEnabled

                val scale = when (s.fontSize) {
                    BrowserSettings.FontSize.TINY -> 0.75
                    BrowserSettings.FontSize.SMALL -> 0.875
                    BrowserSettings.FontSize.MEDIUM -> 1.0
                    BrowserSettings.FontSize.LARGE -> 1.125
                    BrowserSettings.FontSize.HUGE -> 1.25
                }
                if (view.pageZoom != scale) {
                    view.pageZoom = scale
                }
            }
        },
        modifier = modifier
    )
}
