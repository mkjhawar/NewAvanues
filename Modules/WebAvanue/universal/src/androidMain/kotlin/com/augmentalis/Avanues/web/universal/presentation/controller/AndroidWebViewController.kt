package com.augmentalis.Avanues.web.universal.presentation.controller

import android.util.Log
import android.webkit.CookieManager
import com.augmentalis.Avanues.web.universal.commands.ActionResult
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

    // ========== Find in Page ==========

    override suspend fun findInPage(
        query: String,
        caseSensitive: Boolean,
        onResultsFound: (currentMatch: Int, totalMatches: Int) -> Unit
    ): ActionResult {
        val view = webView
        if (view == null) {
            return ActionResult.error("No active page")
        }

        if (query.isEmpty()) {
            return ActionResult.error("Empty search query")
        }

        try {
            // Android WebView findAllAsync doesn't support case sensitivity directly
            // We use JavaScript for better control
            val script = """
                (function() {
                    if (!window.find) return 0;

                    // Clear previous highlights
                    if (window.getSelection) {
                        window.getSelection().removeAllRanges();
                    }

                    // Find all matches
                    let count = 0;
                    while (window.find('$query', ${!caseSensitive}, false, true, false, false, false)) {
                        count++;
                        if (count > 1000) break; // Safety limit
                    }

                    // Return to first match
                    window.find('$query', ${!caseSensitive}, false, false, false, false, false);

                    return count;
                })();
            """.trimIndent()

            val result = view.evaluateJavaScript(script)
            val matchCount = result?.toIntOrNull() ?: 0

            onResultsFound(if (matchCount > 0) 1 else 0, matchCount)

            return if (matchCount > 0) {
                ActionResult.success("Found $matchCount matches")
            } else {
                ActionResult.success("No matches found")
            }
        } catch (e: Exception) {
            logError(TAG, "Find in page failed", e)
            return ActionResult.error("Search failed: ${e.message}")
        }
    }

    override suspend fun findNext(): ActionResult {
        val view = webView
        if (view == null) {
            return ActionResult.error("No active page")
        }

        try {
            // Use JavaScript window.find to navigate to next match
            val script = """
                (function() {
                    if (!window.find) return false;
                    return window.find(null, false, false, true, false, false, false);
                })();
            """.trimIndent()

            val result = view.evaluateJavaScript(script)
            val found = result == "true"

            return if (found) {
                ActionResult.success("Next match")
            } else {
                ActionResult.success("No more matches")
            }
        } catch (e: Exception) {
            logError(TAG, "Find next failed", e)
            return ActionResult.error("Failed: ${e.message}")
        }
    }

    override suspend fun findPrevious(): ActionResult {
        val view = webView
        if (view == null) {
            return ActionResult.error("No active page")
        }

        try {
            // Use JavaScript window.find to navigate to previous match (backwards=true)
            val script = """
                (function() {
                    if (!window.find) return false;
                    return window.find(null, false, true, true, false, false, false);
                })();
            """.trimIndent()

            val result = view.evaluateJavaScript(script)
            val found = result == "true"

            return if (found) {
                ActionResult.success("Previous match")
            } else {
                ActionResult.success("No more matches")
            }
        } catch (e: Exception) {
            logError(TAG, "Find previous failed", e)
            return ActionResult.error("Failed: ${e.message}")
        }
    }

    override suspend fun clearFindMatches(): ActionResult {
        val view = webView
        if (view == null) {
            return ActionResult.error("No active page")
        }

        try {
            // Clear selection and highlights
            val script = """
                (function() {
                    if (window.getSelection) {
                        window.getSelection().removeAllRanges();
                    }
                    return true;
                })();
            """.trimIndent()

            view.evaluateJavaScript(script)

            return ActionResult.success("Cleared find highlights")
        } catch (e: Exception) {
            logError(TAG, "Clear find matches failed", e)
            return ActionResult.error("Failed: ${e.message}")
        }
    }
}
