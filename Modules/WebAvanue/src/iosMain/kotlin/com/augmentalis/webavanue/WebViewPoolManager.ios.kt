package com.augmentalis.webavanue

import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration

/**
 * iOS WebViewPoolManager implementation
 *
 * Manages a pool of WKWebView instances for performance
 */
actual object WebViewPoolManager {
    private val pool = mutableListOf<WKWebView>()
    private const val MAX_POOL_SIZE = 3

    actual fun preWarmWebView() {
        if (pool.size < MAX_POOL_SIZE) {
            val configuration = WKWebViewConfiguration()
            configuration.preferences.javaScriptEnabled = true

            val webView = WKWebView(
                frame = platform.CoreGraphics.CGRectZero.readValue(),
                configuration = configuration
            )
            pool.add(webView)
        }
    }

    actual fun getWebView(): Any? {
        return pool.removeFirstOrNull()
    }

    actual fun returnWebView(webView: Any) {
        if (webView is WKWebView && pool.size < MAX_POOL_SIZE) {
            // Clear webview state before returning to pool
            webView.stopLoading()
            webView.loadHTMLString("", baseURL = null)
            pool.add(webView)
        }
    }

    actual fun clearPool() {
        pool.forEach { it.removeFromSuperview() }
        pool.clear()
    }

    actual fun getPoolSize(): Int {
        return pool.size
    }
}
