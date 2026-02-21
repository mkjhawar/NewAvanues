package com.augmentalis.webavanue

import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import com.augmentalis.webavanue.util.JsStringEscaper
import kotlinx.serialization.json.Json

private const val TAG = "WebAvanueVoiceOS"

/**
 * Bridge between WebAvanue WebView and VoiceOS command system.
 *
 * Provides:
 * - DOM scraping for voice command generation
 * - Element interaction (click, focus, input)
 * - Real-time element tracking
 *
 * Usage:
 * ```kotlin
 * val bridge = WebAvanueVoiceOSBridge(webView)
 * bridge.attach()
 *
 * // Scrape DOM
 * val result = bridge.scrapeDom()
 * result?.elements?.forEach { element ->
 *     // Generate voice commands for element
 * }
 *
 * // Click element
 * bridge.clickElement("vos_42")
 * ```
 */
class WebAvanueVoiceOSBridge(private val webView: WebView) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private var isAttached = false

    /**
     * Listener for DOM changes.
     */
    interface DOMChangeListener {
        fun onDOMChanged(result: DOMScrapeResult)
        fun onPageLoaded(url: String, title: String)
    }

    private var domChangeListener: DOMChangeListener? = null

    /**
     * Attach the JavaScript interface to the WebView.
     * Must be called before any scraping.
     */
    fun attach() {
        if (isAttached) return

        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(VoiceOSInterface(), "VoiceOSBridge")
        isAttached = true
        Log.d(TAG, "VoiceOS bridge attached to WebView")
    }

    /**
     * Detach the JavaScript interface.
     */
    fun detach() {
        if (!isAttached) return

        webView.removeJavascriptInterface("VoiceOSBridge")
        isAttached = false
        Log.d(TAG, "VoiceOS bridge detached from WebView")
    }

    /**
     * Set listener for DOM changes.
     */
    fun setDOMChangeListener(listener: DOMChangeListener?) {
        domChangeListener = listener
    }

    /**
     * Scrape the current page DOM.
     *
     * @return DOMScrapeResult with all interactive elements, or null if failed
     */
    suspend fun scrapeDom(): DOMScrapeResult? = withContext(Dispatchers.Main) {
        if (!isAttached) {
            Log.w(TAG, "Bridge not attached, cannot scrape DOM")
            return@withContext null
        }

        val deferred = CompletableDeferred<String?>()

        webView.evaluateJavascript(DOMScraperBridge.SCRAPER_SCRIPT) { result ->
            deferred.complete(result)
        }

        val resultStr = withTimeoutOrNull(5000) { deferred.await() }

        if (resultStr.isNullOrEmpty() || resultStr == "null") {
            Log.w(TAG, "Empty result from DOM scraper")
            return@withContext null
        }

        try {
            // JavaScript returns JSON as a string, need to unescape
            val unescaped = resultStr
                .removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")

            val response = json.decodeFromString<ScraperResponse>(unescaped)
            Log.d(TAG, "Scraped ${response.scrape.elementCount} elements from ${response.scrape.url}")
            response.scrape
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse scraper result", e)
            null
        }
    }

    /**
     * Click an element by its VoiceOS ID.
     */
    suspend fun clickElement(vosId: String): Boolean = withContext(Dispatchers.Main) {
        executeAction(DOMScraperBridge.clickElementScript(vosId))
    }

    /**
     * Click an element by CSS selector.
     */
    suspend fun clickBySelector(selector: String): Boolean = withContext(Dispatchers.Main) {
        val safe = JsStringEscaper.escapeSelector(selector)
        val script = """
            (function() {
                const el = document.querySelector('$safe');
                if (el) {
                    el.click();
                    return JSON.stringify({ success: true });
                }
                return JSON.stringify({ success: false, error: 'Element not found' });
            })();
        """.trimIndent()
        executeAction(script)
    }

    /**
     * Focus an element by CSS selector.
     */
    suspend fun focusElement(selector: String): Boolean = withContext(Dispatchers.Main) {
        executeAction(DOMScraperBridge.focusElementScript(selector))
    }

    /**
     * Input text into an element.
     */
    suspend fun inputText(selector: String, text: String): Boolean = withContext(Dispatchers.Main) {
        executeAction(DOMScraperBridge.inputTextScript(selector, text))
    }

    /**
     * Scroll to an element.
     */
    suspend fun scrollToElement(selector: String): Boolean = withContext(Dispatchers.Main) {
        executeAction(DOMScraperBridge.scrollToElementScript(selector))
    }

    /**
     * Highlight an element (for debugging).
     */
    suspend fun highlightElement(selector: String): Boolean = withContext(Dispatchers.Main) {
        executeAction(DOMScraperBridge.highlightElementScript(selector))
    }

    /**
     * Execute a JavaScript action and return success status.
     */
    private suspend fun executeAction(script: String): Boolean {
        val deferred = CompletableDeferred<String?>()

        webView.evaluateJavascript(script) { result ->
            deferred.complete(result)
        }

        val resultStr = withTimeoutOrNull(3000) { deferred.await() } ?: return false

        return try {
            val unescaped = resultStr
                .removeSurrounding("\"")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")

            val response = json.decodeFromString<ActionResponse>(unescaped)
            response.success
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse action result", e)
            false
        }
    }

    /**
     * JavaScript interface exposed to the web page.
     *
     * Allows the page to notify Android of DOM changes.
     */
    inner class VoiceOSInterface {

        @JavascriptInterface
        fun onDOMReady() {
            Log.d(TAG, "DOM ready notification received")
            // Could trigger auto-scrape here
        }

        @JavascriptInterface
        fun onDOMChanged() {
            Log.d(TAG, "DOM changed notification received")
            // Could trigger re-scrape here
        }

        @JavascriptInterface
        fun onPageLoaded(url: String, title: String) {
            Log.d(TAG, "Page loaded: $title ($url)")
            domChangeListener?.onPageLoaded(url, title)
        }

        @JavascriptInterface
        fun sendScrapeResult(jsonResult: String) {
            try {
                val response = json.decodeFromString<ScraperResponse>(jsonResult)
                domChangeListener?.onDOMChanged(response.scrape)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse DOM change result", e)
            }
        }
    }

    companion object {
        /**
         * JavaScript to inject for automatic DOM change monitoring.
         *
         * Call this after page load to enable real-time tracking.
         */
        const val MUTATION_OBSERVER_SCRIPT = """
            (function() {
                if (window._voiceOSObserver) return;

                const observer = new MutationObserver((mutations) => {
                    // Debounce notifications
                    clearTimeout(window._voiceOSDebounce);
                    window._voiceOSDebounce = setTimeout(() => {
                        if (window.VoiceOSBridge) {
                            VoiceOSBridge.onDOMChanged();
                        }
                    }, 500);
                });

                observer.observe(document.body, {
                    childList: true,
                    subtree: true,
                    attributes: true,
                    attributeFilter: ['class', 'style', 'hidden', 'disabled', 'aria-hidden']
                });

                window._voiceOSObserver = observer;

                // Notify initial state
                if (window.VoiceOSBridge) {
                    VoiceOSBridge.onPageLoaded(window.location.href, document.title);
                }
            })();
        """
    }
}
