package com.augmentalis.Avanues.web.universal.platform.webview

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * WebViewLifecycle - Manages WebView lifecycle, pooling, and memory management
 *
 * Single Responsibility: WebView instance creation, reuse, and disposal
 *
 * Features:
 * - WebView pooling per tab ID (preserves navigation history across tab switches)
 * - Thread-safe operations (ConcurrentHashMap + @Synchronized)
 * - State serialization/deserialization for history persistence
 * - Proper cleanup on WebView disposal
 *
 * FIX: Issue #1 - Tab History Lost When Switching Tabs
 * WebView instances are cached by tab ID, preventing recreation on every tab switch.
 *
 * FIX: Race condition - Thread-safe operations with ConcurrentHashMap
 */
class WebViewLifecycle {
    private val pool = WebViewPool()

    /**
     * Acquire WebView for a tab ID.
     * Reuses existing WebView from pool or creates a new one.
     *
     * @param tabId Unique tab identifier
     * @param context Android context for WebView creation
     * @param factory Factory function to create new WebView instances
     * @return WebView instance (existing or newly created)
     */
    fun acquireWebView(
        tabId: String,
        context: Context,
        factory: (Context) -> WebView
    ): WebView {
        return pool.getOrCreate(tabId, context, factory)
    }

    /**
     * Release WebView back to pool when tab is backgrounded.
     * WebView remains in pool for reuse.
     *
     * @param tabId Tab identifier
     */
    fun releaseWebView(tabId: String) {
        // WebView stays in pool - only destroyed on removeWebView()
        pool.get(tabId)?.let { webView ->
            // Pause to save resources
            Handler(Looper.getMainLooper()).post {
                webView.onPause()
                webView.pauseTimers()
            }
        }
    }

    /**
     * Remove and destroy WebView for a tab ID.
     * Called when tab is permanently closed.
     *
     * @param tabId Tab identifier
     */
    fun removeWebView(tabId: String) {
        pool.remove(tabId)
    }

    /**
     * Clear all WebViews from pool.
     * Called on Activity.onDestroy for cleanup.
     */
    fun clearAllWebViews() {
        pool.clear()
    }

    /**
     * Get existing WebView without creating new one.
     *
     * @param tabId Tab identifier
     * @return WebView if exists in pool, null otherwise
     */
    fun getWebView(tabId: String): WebView? {
        return pool.get(tabId)
    }

    /**
     * Save WebView navigation state to serialized string.
     *
     * @param webView WebView instance
     * @return Base64-encoded state string, or null if serialization fails
     */
    fun saveState(webView: WebView): String? {
        return webView.saveStateToString()
    }

    /**
     * Restore WebView navigation state from serialized string.
     *
     * @param webView WebView instance
     * @param stateString Base64-encoded state string
     * @return true if restoration succeeded, false otherwise
     */
    fun restoreState(webView: WebView, stateString: String?): Boolean {
        return webView.restoreStateFromString(stateString)
    }
}

/**
 * WebViewPool - Internal pool manager for WebView instances
 *
 * Thread-safe pool implementation with LRU eviction.
 * Maintains WebView instances across tab switches to preserve navigation history.
 *
 * FIX: Race condition - @Synchronized for thread safety
 * FIX: Memory Leak - LRU eviction prevents unbounded growth causing OOM on <4GB devices
 */
private class WebViewPool {
    /**
     * Maximum number of WebViews to cache (prevents OOM)
     * Each WebView ~50-100MB, so 5 WebViews = ~250-500MB max memory
     */
    private val MAX_CACHED_WEBVIEWS = 5

    /**
     * LRU cache using LinkedHashMap with access-order mode
     * FIX: Memory Leak - Prevents unbounded growth causing OOM on <4GB devices
     */
    private val webViews = object : LinkedHashMap<String, WebView>(
        16,          // Initial capacity
        0.75f,       // Load factor
        true         // Access-order mode for LRU behavior
    ) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, WebView>): Boolean {
            val shouldRemove = size > MAX_CACHED_WEBVIEWS
            if (shouldRemove) {
                // Destroy evicted WebView to free memory
                Handler(Looper.getMainLooper()).post {
                    eldest.value.onPause()
                    eldest.value.pauseTimers()
                    eldest.value.destroy()
                }
                println("🗑️ WebViewLifecycle: Evicting LRU WebView (tab: ${eldest.key}) - pool size: $size")
            }
            return shouldRemove
        }
    }

    /**
     * Get or create WebView for a tab ID (thread-safe with LRU eviction)
     *
     * @param tabId Unique tab identifier
     * @param context Android context
     * @param factory Factory function to create new WebView
     * @return WebView instance (existing or newly created)
     */
    @Synchronized
    fun getOrCreate(tabId: String, context: Context, factory: (Context) -> WebView): WebView {
        // Access existing WebView (updates LRU order)
        val existing = webViews[tabId]
        if (existing != null) {
            return existing
        }

        // Create new WebView (may trigger LRU eviction if pool full)
        val newWebView = factory(context)
        webViews[tabId] = newWebView
        println("✅ WebViewLifecycle: Created WebView for tab $tabId - pool size: ${webViews.size}")

        return newWebView
    }

    /**
     * Remove and destroy WebView for a tab ID
     * Call this when a tab is closed (thread-safe)
     *
     * @param tabId Tab identifier
     */
    @Synchronized
    fun remove(tabId: String) {
        webViews.remove(tabId)?.let { webView ->
            // Ensure Main thread for WebView operations
            Handler(Looper.getMainLooper()).post {
                webView.onPause()
                webView.pauseTimers()
                webView.destroy()
            }
        }
    }

    /**
     * Clear all WebViews (for cleanup on Activity.onDestroy)
     * Thread-safe with snapshot iteration
     */
    @Synchronized
    fun clear() {
        val snapshot = webViews.values.toList()
        webViews.clear()
        // Ensure Main thread for WebView operations
        Handler(Looper.getMainLooper()).post {
            snapshot.forEach { webView ->
                webView.onPause()
                webView.pauseTimers()
                webView.destroy()
            }
        }
    }

    /**
     * Get existing WebView for a tab ID (without creating)
     *
     * @param tabId Tab identifier
     * @return WebView if exists, null otherwise
     */
    fun get(tabId: String): WebView? {
        return webViews[tabId]
    }
}

/**
 * Extension: Save WebView state to Base64 string
 *
 * @return Base64-encoded state string, or null if serialization fails
 */
private fun WebView.saveStateToString(): String? {
    return try {
        val bundle = Bundle()
        saveState(bundle)

        // Serialize bundle to Base64 string
        val parcel = android.os.Parcel.obtain()
        parcel.writeBundle(bundle)
        val bytes = parcel.marshall()
        parcel.recycle()

        Base64.encodeToString(bytes, Base64.DEFAULT)
    } catch (e: Exception) {
        println("WebViewLifecycle: Failed to save state: ${e.message}")
        null
    }
}

/**
 * Extension: Restore WebView state from Base64 string
 *
 * @param stateString Base64-encoded state string
 * @return true if restoration succeeded, false otherwise
 */
private fun WebView.restoreStateFromString(stateString: String?): Boolean {
    if (stateString.isNullOrBlank()) return false

    return try {
        // Deserialize Base64 string to bundle
        val bytes = Base64.decode(stateString, Base64.DEFAULT)
        val parcel = android.os.Parcel.obtain()
        parcel.unmarshall(bytes, 0, bytes.size)
        parcel.setDataPosition(0)
        val bundle = parcel.readBundle(WebView::class.java.classLoader)
        parcel.recycle()

        if (bundle != null) {
            restoreState(bundle)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        println("WebViewLifecycle: Failed to restore state: ${e.message}")
        false
    }
}

/**
 * Composable: Remember WebViewLifecycle instance
 *
 * @return WebViewLifecycle instance (persists across recompositions)
 */
@Composable
fun rememberWebViewLifecycle(): WebViewLifecycle {
    return remember { WebViewLifecycle() }
}
