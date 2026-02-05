package com.augmentalis.browseravanue.webview

import android.os.Build
import android.webkit.PermissionRequest
import android.webkit.WebSettings
import android.webkit.WebView

/**
 * Enhanced WebView extensions for Phase 3 features
 *
 * Features:
 * - Find in Page (search within page)
 * - Dark Mode (force dark on websites)
 * - Popup handling (already in BrowserWebView)
 * - Permission handling (camera, mic, location)
 */

// ==========================================
// Find in Page
// ==========================================

/**
 * Find in page state
 */
data class FindInPageState(
    val query: String = "",
    val currentMatch: Int = 0,
    val totalMatches: Int = 0,
    val isActive: Boolean = false
)

/**
 * Find all occurrences of text in page
 *
 * @param query Search query
 * @param onResult Callback with (currentMatch, totalMatches)
 */
fun WebView.findInPage(query: String, onResult: (Int, Int) -> Unit) {
    if (query.isBlank()) {
        clearFindMatches()
        return
    }

    // Highlight all matches
    findAllAsync(query)

    // Set result callback
    setFindListener { activeMatchOrdinal, numberOfMatches, isDoneCounting ->
        if (isDoneCounting) {
            onResult(activeMatchOrdinal + 1, numberOfMatches) // +1 for 1-based index
        }
    }
}

/**
 * Find next match
 */
fun WebView.findNext() {
    findNext(true)
}

/**
 * Find previous match
 */
fun WebView.findPrevious() {
    findNext(false)
}

/**
 * Clear find in page highlighting
 */
fun WebView.clearFindMatches() {
    clearMatches()
    setFindListener(null)
}

// ==========================================
// Dark Mode
// ==========================================

/**
 * Enable/disable dark mode
 *
 * Forces dark mode on websites (inverts colors)
 * Requires API 29+ (Android 10)
 */
fun WebView.setDarkMode(enabled: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        settings.forceDark = if (enabled) {
            WebSettings.FORCE_DARK_ON
        } else {
            WebSettings.FORCE_DARK_OFF
        }
    }
    // Note: For API < 29, dark mode not available in WebView
}

/**
 * Check if dark mode is supported
 */
fun WebView.isDarkModeSupported(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}

/**
 * Get current dark mode state
 */
fun WebView.isDarkModeEnabled(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        settings.forceDark == WebSettings.FORCE_DARK_ON
    } else {
        false
    }
}

// ==========================================
// Permission Handling
// ==========================================

/**
 * Permission request data
 */
data class PermissionRequestInfo(
    val origin: String,
    val resources: Array<String>,
    val request: PermissionRequest
) {
    val permissions: List<String>
        get() = resources.map { resource ->
            when (resource) {
                PermissionRequest.RESOURCE_AUDIO_CAPTURE -> "Microphone"
                PermissionRequest.RESOURCE_VIDEO_CAPTURE -> "Camera"
                PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> "Protected Media ID"
                PermissionRequest.RESOURCE_MIDI_SYSEX -> "MIDI"
                else -> resource
            }
        }

    fun grant() {
        request.grant(resources)
    }

    fun deny() {
        request.deny()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PermissionRequestInfo

        if (origin != other.origin) return false
        if (!resources.contentEquals(other.resources)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = origin.hashCode()
        result = 31 * result + resources.contentHashCode()
        return result
    }
}

/**
 * Grant permission request
 */
fun PermissionRequest.grantPermission() {
    grant(resources)
}

/**
 * Deny permission request
 */
fun PermissionRequest.denyPermission() {
    deny()
}

// ==========================================
// Popup Handling (already in BrowserWebView)
// ==========================================

/**
 * Enable/disable popups
 */
fun WebView.setPopupsEnabled(enabled: Boolean) {
    settings.javaScriptCanOpenWindowsAutomatically = enabled
    settings.setSupportMultipleWindows(enabled)
}

// ==========================================
// Console Logging (DevTools)
// ==========================================

/**
 * Enable/disable remote debugging
 *
 * Allows connecting via chrome://inspect
 */
fun WebView.setRemoteDebuggingEnabled(enabled: Boolean) {
    WebView.setWebContentsDebuggingEnabled(enabled)
}

// ==========================================
// Performance
// ==========================================

/**
 * Enable/disable hardware acceleration
 */
fun WebView.setHardwareAccelerated(enabled: Boolean) {
    setLayerType(
        if (enabled) WebView.LAYER_TYPE_HARDWARE
        else WebView.LAYER_TYPE_SOFTWARE,
        null
    )
}
