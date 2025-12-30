package com.augmentalis.Avanues.web.domain

/**
 * iOS WebView implementation (Phase 2)
 * Platform-specific wrapper for WKWebView
 *
 * TODO: Implement using WKWebView from WebKit framework
 */
actual class WebViewEngine {
    actual fun loadUrl(url: String) {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun goBack() {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun goForward() {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun reload() {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun stopLoading() {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun canGoBack(): Boolean {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun canGoForward(): Boolean {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun getCurrentUrl(): String? {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun getCurrentTitle(): String? {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun evaluateJavaScript(script: String, callback: ((String?) -> Unit)?) {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun clearCache() {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun clearCookies() {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun setUserAgent(userAgent: String) {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun setJavaScriptEnabled(enabled: Boolean) {
        TODO("iOS WKWebView implementation - Phase 2")
    }

    actual fun destroy() {
        TODO("iOS WKWebView implementation - Phase 2")
    }
}
