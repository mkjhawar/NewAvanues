package com.augmentalis.Avanues.web.universal.presentation.controller

import android.util.Log
import android.webkit.CookieManager
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabViewModel
import com.augmentalis.webavanue.platform.WebView
import kotlinx.coroutines.flow.firstOrNull

/**
 * Android implementation of WebViewController
 *
 * Extends CommonWebViewController to provide Android-specific WebView operations.
 * Most logic is shared via the base class; this class only implements platform-specific
 * methods for Android WebView, CookieManager, and resource loading.
 *
 * @param webViewProvider Function that returns the current active WebView instance
 * @param tabViewModel ViewModel for tab state persistence
 */
class AndroidWebViewController(
    private val webViewProvider: () -> WebView?,
    tabViewModel: TabViewModel
) : CommonWebViewController(tabViewModel) {

    private val webView: WebView?
        get() = webViewProvider()

    companion object {
        private const val TAG = "AndroidWebViewController"
    }

    // ========== Platform-Specific Implementations ==========

    override suspend fun evaluateJavaScript(script: String): String? {
        return webView?.evaluateJavaScript(script)
    }

    override suspend fun canGoBack(): Boolean {
        return webView?.canGoBack?.firstOrNull() ?: false
    }

    override suspend fun canGoForward(): Boolean {
        return webView?.canGoForward?.firstOrNull() ?: false
    }

    override fun navigateBack() {
        webView?.goBack()
    }

    override fun navigateForward() {
        webView?.goForward()
    }

    override fun reloadPage() {
        webView?.reload()
    }

    override fun zoomInWebView() {
        webView?.zoomIn()
    }

    override fun zoomOutWebView() {
        webView?.zoomOut()
    }

    override fun resetZoomWebView() {
        webView?.resetZoom()
    }

    override fun setDesktopModeInternal(enabled: Boolean): Boolean {
        return webView?.setDesktopMode(enabled) != null
    }

    override suspend fun clearCookiesInternal(): Boolean {
        return try {
            val cookieManager = CookieManager.getInstance()
            var success = true
            cookieManager.removeAllCookies { result ->
                if (!result) {
                    Log.w(TAG, "Failed to clear cookies")
                    success = false
                }
            }
            cookieManager.flush()
            success
        } catch (e: Exception) {
            logError(TAG, "Error clearing cookies", e)
            false
        }
    }

    override suspend fun ensureGesturesLibrary(): Boolean {
        val view = webView ?: return false

        // Check if library is already loaded
        val isLoaded = view.evaluateJavaScript("typeof window.AvanuesGestures !== 'undefined'")
        if (isLoaded == "true") return true

        // Load gestures.js from resources
        val gesturesJs = try {
            this::class.java.classLoader?.getResourceAsStream("gestures.js")?.bufferedReader()?.readText()
        } catch (e: Exception) {
            logError(TAG, "Failed to load gestures.js", e)
            return false
        }

        if (gesturesJs == null) {
            logError(TAG, "gestures.js not found in resources")
            return false
        }

        // Inject library
        view.evaluateJavaScript(gesturesJs)
        return true
    }

    override fun hasWebView(): Boolean {
        return webView != null
    }

    override fun log(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun logError(tag: String, message: String, error: Throwable?) {
        if (error != null) {
            Log.e(tag, message, error)
        } else {
            Log.e(tag, message)
        }
    }

    // ========== Page Info (Android-specific access) ==========

    override fun getCurrentUrl(): String {
        return webView?.currentUrl?.value ?: ""
    }

    override fun getCurrentTitle(): String {
        return webView?.pageTitle?.value ?: ""
    }
}
