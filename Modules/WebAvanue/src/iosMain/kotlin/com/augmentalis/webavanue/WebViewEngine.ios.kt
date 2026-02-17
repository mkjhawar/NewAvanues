package com.augmentalis.webavanue

import kotlinx.cinterop.*
import platform.Foundation.*
import platform.WebKit.*

/**
 * iOS WebView implementation using WKWebView
 *
 * Provides platform-specific wrapper for WKWebView
 */
actual class WebViewEngine {
    private val configuration = WKWebViewConfiguration()
    private val webView: WKWebView

    init {
        // Configure web view
        configuration.preferences.javaScriptEnabled = true
        configuration.websiteDataStore = WKWebsiteDataStore.defaultDataStore()
        configuration.allowsInlineMediaPlayback = true

        // Create web view
        webView = WKWebView(
            frame = platform.CoreGraphics.CGRectZero.readValue(),
            configuration = configuration
        )

        webView.allowsBackForwardNavigationGestures = true
    }

    actual fun loadUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            val request = NSURLRequest.requestWithURL(nsUrl)
            webView.loadRequest(request)
        }
    }

    actual fun goBack() {
        if (canGoBack()) {
            webView.goBack()
        }
    }

    actual fun goForward() {
        if (canGoForward()) {
            webView.goForward()
        }
    }

    actual fun reload() {
        webView.reload()
    }

    actual fun stopLoading() {
        webView.stopLoading()
    }

    actual fun canGoBack(): Boolean {
        return webView.canGoBack
    }

    actual fun canGoForward(): Boolean {
        return webView.canGoForward
    }

    actual fun getCurrentUrl(): String? {
        return webView.URL?.absoluteString
    }

    actual fun getCurrentTitle(): String? {
        return webView.title
    }

    actual fun evaluateJavaScript(script: String, callback: ((String?) -> Unit)?) {
        webView.evaluateJavaScript(script) { result, error ->
            if (error == null) {
                callback?.invoke(result?.toString())
            } else {
                callback?.invoke(null)
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

    actual fun setUserAgent(userAgent: String) {
        webView.customUserAgent = userAgent
    }

    actual fun setJavaScriptEnabled(enabled: Boolean) {
        webView.configuration.preferences.javaScriptEnabled = enabled
    }

    actual fun destroy() {
        webView.stopLoading()
        webView.removeFromSuperview()
    }

    /**
     * Get the underlying WKWebView
     * For platform-specific operations
     */
    fun getWKWebView(): WKWebView = webView
}
