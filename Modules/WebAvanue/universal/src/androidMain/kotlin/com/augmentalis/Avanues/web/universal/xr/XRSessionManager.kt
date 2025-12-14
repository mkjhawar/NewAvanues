package com.augmentalis.webavanue.feature.xr

import android.os.Handler
import android.os.Looper
import android.webkit.WebView

/**
 * Android implementation of XR session management.
 *
 * Extends CommonSessionManager with Android-specific WebView
 * JavaScript execution and Handler-based delayed tasks.
 */
class XRSessionManager : CommonSessionManager() {

    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Set WebView instance for session management
     *
     * @param webView WebView instance
     */
    fun setWebView(webView: WebView) {
        this.webView = webView
    }

    override fun executeEndSessionScript() {
        webView?.evaluateJavascript(JS_END_SESSION, null)
    }

    override fun scheduleDelayedTask(delayMs: Long, task: () -> Unit) {
        mainHandler.postDelayed(task, delayMs)
    }

    override fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Query current XR session state from WebView
     *
     * @param callback Callback with session active state and mode
     */
    fun querySessionState(callback: (active: Boolean, mode: String?) -> Unit) {
        webView?.evaluateJavascript(JS_CHECK_SESSION) { result ->
            try {
                val active = result?.contains("\"active\":true") == true
                val mode = if (active) {
                    val modeMatch = Regex("\"mode\":\"([^\"]+)\"").find(result)
                    modeMatch?.groupValues?.get(1)
                } else {
                    null
                }
                callback(active, mode)
            } catch (e: Exception) {
                callback(false, null)
            }
        }
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        forceEndSession()
        webView = null
    }
}
