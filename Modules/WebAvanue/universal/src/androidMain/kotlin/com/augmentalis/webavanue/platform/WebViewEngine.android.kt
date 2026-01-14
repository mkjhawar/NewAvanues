package com.augmentalis.webavanue.platform

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebSettings

/**
 * Android WebView implementation
 * Platform-specific wrapper for android.webkit.WebView
 */
actual class WebViewEngine(context: Context) {
    private val webView: WebView = WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
    }

    actual fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    actual fun goBack() {
        if (webView.canGoBack()) {
            webView.goBack()
        }
    }

    actual fun goForward() {
        if (webView.canGoForward()) {
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
        return webView.canGoBack()
    }

    actual fun canGoForward(): Boolean {
        return webView.canGoForward()
    }

    actual fun getCurrentUrl(): String? {
        return webView.url
    }

    actual fun getCurrentTitle(): String? {
        return webView.title
    }

    actual fun evaluateJavaScript(script: String, callback: ((String?) -> Unit)?) {
        webView.evaluateJavascript(script) { result ->
            callback?.invoke(result)
        }
    }

    actual fun clearCache() {
        webView.clearCache(true)
    }

    actual fun clearCookies() {
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
    }

    actual fun setUserAgent(userAgent: String) {
        webView.settings.userAgentString = userAgent
    }

    actual fun setJavaScriptEnabled(enabled: Boolean) {
        webView.settings.javaScriptEnabled = enabled
    }

    actual fun destroy() {
        webView.destroy()
    }

    /**
     * Get the underlying Android WebView
     * For platform-specific operations
     */
    fun getWebView(): WebView = webView
}
