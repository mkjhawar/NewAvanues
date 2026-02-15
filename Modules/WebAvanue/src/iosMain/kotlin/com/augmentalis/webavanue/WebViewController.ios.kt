package com.augmentalis.webavanue

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.WebKit.*

/**
 * iOS WebViewController implementation
 *
 * Controls WKWebView operations programmatically
 */
actual class WebViewController {
    private var webView: WKWebView? = null

    internal fun attachWebView(wkWebView: WKWebView) {
        this.webView = wkWebView
    }

    actual fun goBack() {
        webView?.let {
            if (it.canGoBack) {
                it.goBack()
            }
        }
    }

    actual fun goForward() {
        webView?.let {
            if (it.canGoForward) {
                it.goForward()
            }
        }
    }

    actual fun reload() {
        webView?.reload()
    }

    actual fun stopLoading() {
        webView?.stopLoading()
    }

    actual fun loadUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            val request = NSURLRequest.requestWithURL(nsUrl)
            webView?.loadRequest(request)
        }
    }

    actual fun evaluateJavaScript(script: String, callback: (String?) -> Unit) {
        webView?.evaluateJavaScript(script) { result, error ->
            if (error == null) {
                callback(result?.toString())
            } else {
                callback(null)
            }
        }
    }

    actual fun clearCache() {
        val dataTypes = setOf(
            WKWebsiteDataTypeDiskCache,
            WKWebsiteDataTypeMemoryCache,
            WKWebsiteDataTypeOfflineWebApplicationCache
        )

        val dataStore = WKWebsiteDataStore.defaultDataStore()
        val date = NSDate.dateWithTimeIntervalSince1970(0.0)

        dataStore.removeDataOfTypes(
            dataTypes,
            modifiedSince = date,
            completionHandler = {}
        )
    }

    actual fun clearCookies() {
        val dataTypes = setOf(WKWebsiteDataTypeCookies)
        val dataStore = WKWebsiteDataStore.defaultDataStore()
        val date = NSDate.dateWithTimeIntervalSince1970(0.0)

        dataStore.removeDataOfTypes(
            dataTypes,
            modifiedSince = date,
            completionHandler = {}
        )
    }

    actual fun clearHistory() {
        // Clear back/forward list by loading blank page
        webView?.loadHTMLString("", baseURL = null)
    }

    actual fun setUserAgent(userAgent: String) {
        webView?.customUserAgent = userAgent
    }

    actual fun setJavaScriptEnabled(enabled: Boolean) {
        webView?.configuration?.preferences?.javaScriptEnabled = enabled
    }

    actual fun setDesktopMode(enabled: Boolean) {
        val userAgent = if (enabled) {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Safari/605.1.15"
        } else {
            null
        }
        webView?.customUserAgent = userAgent
    }

    actual fun zoomIn() {
        webView?.let {
            val currentZoom = it.pageZoom
            it.pageZoom = (currentZoom * 1.1).coerceAtMost(3.0)
        }
    }

    actual fun zoomOut() {
        webView?.let {
            val currentZoom = it.pageZoom
            it.pageZoom = (currentZoom * 0.9).coerceAtLeast(0.5)
        }
    }

    actual fun resetZoom() {
        webView?.pageZoom = 1.0
    }

    actual fun applySettings(settings: BrowserSettings) {
        webView?.let { wv ->
            // Apply font size (via zoom)
            val scale = when (settings.fontSize) {
                BrowserSettings.FontSize.TINY -> 0.75
                BrowserSettings.FontSize.SMALL -> 0.875
                BrowserSettings.FontSize.MEDIUM -> 1.0
                BrowserSettings.FontSize.LARGE -> 1.125
                BrowserSettings.FontSize.HUGE -> 1.25
            }
            wv.pageZoom = scale

            // Apply JavaScript setting
            wv.configuration.preferences.javaScriptEnabled = settings.javaScriptEnabled

            // Apply media settings
            wv.configuration.mediaTypesRequiringUserActionForPlayback = when (settings.autoPlay) {
                AutoPlay.ALWAYS -> WKAudiovisualMediaTypes.None
                AutoPlay.NEVER -> WKAudiovisualMediaTypes.All
                else -> WKAudiovisualMediaTypes.Audio
            }
        }
    }
}
