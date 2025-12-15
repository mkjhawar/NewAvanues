package com.augmentalis.cockpit.mvp.content.bridge

import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * CockpitJsBridge - JavaScript bridge interface for WebView-native communication
 *
 * Exposes Cockpit functionality to JavaScript via `window.cockpit` object.
 * All methods are annotated with @JavascriptInterface for WebView access.
 *
 * Security:
 * - Only expose safe methods (no file system access, no process execution)
 * - Validate all inputs from JavaScript
 * - Use HTTPS-only content to prevent XSS
 * - Certificate pinning for www.augmentalis.com
 *
 * Example JavaScript usage:
 * ```javascript
 * // Request window resize
 * window.cockpit.requestSize(800, 600, true);
 *
 * // Get device location
 * window.cockpit.getLocation().then(location => {
 *   console.log('Location:', location.lat, location.lng);
 * });
 *
 * // Open new window
 * window.cockpit.openWindow('WEB_APP', 'https://google.com', 'Google');
 * ```
 */
interface CockpitJsBridge {

    // ========================================
    // Window Management
    // ========================================

    /**
     * Request window size change
     *
     * @param width Requested width in dp
     * @param height Requested height in dp
     * @param isLarge Whether to maximize window (true) or use custom size (false)
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun requestSize(width: Int, height: Int, isLarge: Boolean): String

    /**
     * Minimize window (collapse to title bar)
     *
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun minimize(): String

    /**
     * Maximize window (full screen - 40dp)
     *
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun maximize(): String

    /**
     * Close window
     *
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun close(): String

    // ========================================
    // Device Features
    // ========================================

    /**
     * Get device location (requires location permission)
     *
     * @return JSON string: {"lat": 37.7749, "lng": -122.4194, "accuracy": 10.0}
     *         or {"error": "Location permission denied"}
     */
    @JavascriptInterface
    fun getLocation(): String

    /**
     * Launch voice search with pre-filled query
     *
     * Opens VoiceOS voice search interface with optional query text.
     *
     * @param query Pre-filled search query (empty string for blank search)
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun voiceSearch(query: String): String

    /**
     * Navigate to specific screen/route in Cockpit
     *
     * @param screenId Screen identifier (e.g., "settings", "workspaces", "home")
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun navigate(screenId: String): String

    /**
     * Share content via Android share sheet
     *
     * @param title Share dialog title
     * @param text Text content to share
     * @param url URL to share (optional, pass empty string if not needed)
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun shareContent(title: String, text: String, url: String): String

    // ========================================
    // Workspace Integration
    // ========================================

    /**
     * Open new window in workspace
     *
     * @param type Window type: "WEB_APP", "WIDGET", "REMOTE_DESKTOP", "ANDROID_APP"
     * @param url Content URL (for WEB_APP) or identifier (for other types)
     * @param title Window title
     * @return JSON string: {"success": true, "windowId": "..."} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun openWindow(type: String, url: String, title: String): String

    /**
     * Send message to another window
     *
     * Enables inter-window communication (e.g., dashboard → detail view).
     *
     * @param windowId Target window ID
     * @param message JSON string message payload
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun sendMessage(windowId: String, message: String): String

    // ========================================
    // System
    // ========================================

    /**
     * Log message to native console
     *
     * @param level Log level: "DEBUG", "INFO", "WARN", "ERROR"
     * @param message Log message
     * @return JSON string: {"success": true}
     */
    @JavascriptInterface
    fun log(level: String, message: String): String

    /**
     * Report error to telemetry system
     *
     * Sends error to Firebase Crashlytics with context.
     *
     * @param error Error message
     * @param context JSON string with error context (url, timestamp, etc.)
     * @return JSON string: {"success": true} or {"success": false, "error": "..."}
     */
    @JavascriptInterface
    fun reportError(error: String, context: String): String
}

/**
 * Helper extension to safely parse JSON responses from bridge methods
 */
fun String.toJsBridgeResult(): JsBridgeResult {
    return try {
        val json = JSONObject(this)
        JsBridgeResult(
            success = json.optBoolean("success", false),
            error = json.optString("error").takeIf { it.isNotEmpty() },
            data = json.optJSONObject("data")
        )
    } catch (e: Exception) {
        JsBridgeResult(success = false, error = "Invalid JSON response: ${e.message}")
    }
}

/**
 * Bridge result data class for type-safe parsing
 */
data class JsBridgeResult(
    val success: Boolean,
    val error: String? = null,
    val data: JSONObject? = null
)
