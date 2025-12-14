package com.augmentalis.webavanue.ui.viewmodel

import com.augmentalis.webavanue.feature.commands.ActionResult
import com.augmentalis.webavanue.ui.viewmodel.TabViewModel

/**
 * CommonWebViewController - Platform-agnostic base implementation of WebViewController
 *
 * Provides shared logic for WebView operations that can be used across all platforms.
 * Platform-specific implementations (Android, iOS, Desktop) extend this class and
 * implement the abstract methods.
 *
 * Architecture:
 * - Uses GestureMapper for gesture type → JavaScript mapping
 * - Uses GestureCoordinateResolver for coordinate fallback logic
 * - Delegates scroll operations to TabViewModel
 * - Abstract methods for platform-specific WebView operations
 *
 * @param tabViewModel ViewModel for tab state persistence
 */
abstract class CommonWebViewController(
    protected val tabViewModel: TabViewModel
) : WebViewController {

    /**
     * Coordinate resolver for gesture positioning
     */
    protected val coordinateResolver = GestureCoordinateResolver()

    // ========== Scrolling (Common - delegates to TabViewModel) ==========

    override suspend fun scrollUp(): ActionResult {
        tabViewModel.scrollUp()
        return ActionResult.success("Scrolled up")
    }

    override suspend fun scrollDown(): ActionResult {
        tabViewModel.scrollDown()
        return ActionResult.success("Scrolled down")
    }

    override suspend fun scrollLeft(): ActionResult {
        tabViewModel.scrollLeft()
        return ActionResult.success("Scrolled left")
    }

    override suspend fun scrollRight(): ActionResult {
        tabViewModel.scrollRight()
        return ActionResult.success("Scrolled right")
    }

    override suspend fun scrollTop(): ActionResult {
        tabViewModel.scrollToTop()
        return ActionResult.success("Scrolled to top")
    }

    override suspend fun scrollBottom(): ActionResult {
        tabViewModel.scrollToBottom()
        return ActionResult.success("Scrolled to bottom")
    }

    // ========== Zoom (Common logic, WebView-dependent execution) ==========

    override suspend fun setZoomLevel(level: Int): ActionResult {
        // Validate level
        if (level !in 50..200) {
            return ActionResult.error("Invalid zoom level: must be 50-200")
        }

        // Convert percentage to level (1-5) for TabViewModel
        // 50% = 1, 75% = 2, 100% = 3, 125% = 4, 150% = 5
        val tabZoomLevel = when {
            level <= 50 -> 1
            level <= 75 -> 2
            level <= 100 -> 3
            level <= 125 -> 4
            else -> 5
        }

        // Set zoom level via ViewModel (which handles WebView)
        tabViewModel.setZoomLevel(tabZoomLevel)

        return ActionResult.success("Zoom set to $level%")
    }

    // ========== Desktop Mode (Common logic) ==========

    override suspend fun setDesktopMode(enabled: Boolean): ActionResult {
        // Platform-specific WebView operation
        val result = setDesktopModeInternal(enabled)
        if (!result) {
            return ActionResult.error("No active WebView")
        }

        // Persist to TabViewModel for restoration
        tabViewModel.setDesktopMode(enabled)

        val mode = if (enabled) "desktop" else "mobile"
        return ActionResult.success("Switched to $mode mode")
    }

    // ========== Page Control ==========

    override suspend fun toggleFreeze(): ActionResult {
        // Note: Page freeze not yet implemented
        return ActionResult.error("Page freeze not yet implemented")
    }

    // ========== Gestures (Common - uses GestureMapper) ==========

    override suspend fun performClick(): ActionResult {
        val coords = coordinateResolver.resolve(-1f, -1f)
        return executeGestureScript(
            "JSON.stringify(window.AvanuesGestures.click(${coords.first}, ${coords.second}))"
        )
    }

    override suspend fun performDoubleClick(): ActionResult {
        val coords = coordinateResolver.resolve(-1f, -1f)
        return executeGestureScript(
            "JSON.stringify(window.AvanuesGestures.doubleClick(${coords.first}, ${coords.second}))"
        )
    }

    override suspend fun startDrag(): ActionResult {
        val coords = coordinateResolver.resolve(-1f, -1f)
        return executeGestureScript(
            "JSON.stringify(window.AvanuesGestures.dragStart(${coords.first}, ${coords.second}))"
        )
    }

    override suspend fun stopDrag(): ActionResult {
        val coords = coordinateResolver.resolve(-1f, -1f)
        return executeGestureScript(
            "JSON.stringify(window.AvanuesGestures.dragEnd(${coords.first}, ${coords.second}))"
        )
    }

    override suspend fun select(): ActionResult {
        val coords = coordinateResolver.resolve(-1f, -1f)
        return executeGestureScript(
            "JSON.stringify(window.AvanuesGestures.selectWord(${coords.first}, ${coords.second}))"
        )
    }

    // ========== Universal Gesture Support (Common - uses GestureMapper) ==========

    override suspend fun performGesture(
        gestureType: String,
        x: Float,
        y: Float,
        modifiers: Int
    ): ActionResult {
        // Get final coordinates with fallback logic
        val coords = coordinateResolver.resolve(x, y)

        // Map gesture type to JavaScript
        val script = GestureMapper.mapToScript(gestureType, coords.first, coords.second, modifiers)
            ?: return ActionResult.error("Unknown gesture type: $gestureType")

        return executeGestureScript("JSON.stringify($script)")
    }

    // ========== Abstract Methods (Platform-specific) ==========

    /**
     * Execute JavaScript in the WebView and return result
     * @param script JavaScript to execute
     * @return JavaScript result string, or null on failure
     */
    protected abstract suspend fun evaluateJavaScript(script: String): String?

    /**
     * Check if WebView can go back in history
     * @return true if back navigation is possible
     */
    protected abstract suspend fun canGoBack(): Boolean

    /**
     * Check if WebView can go forward in history
     * @return true if forward navigation is possible
     */
    protected abstract suspend fun canGoForward(): Boolean

    /**
     * Execute back navigation in WebView
     */
    protected abstract fun navigateBack()

    /**
     * Execute forward navigation in WebView
     */
    protected abstract fun navigateForward()

    /**
     * Reload current page in WebView
     */
    protected abstract fun reloadPage()

    /**
     * Execute zoom in on WebView
     */
    protected abstract fun zoomInWebView()

    /**
     * Execute zoom out on WebView
     */
    protected abstract fun zoomOutWebView()

    /**
     * Reset zoom to default on WebView
     */
    protected abstract fun resetZoomWebView()

    /**
     * Set desktop mode on WebView
     * @param enabled true for desktop mode
     * @return true if successful, false if no WebView
     */
    protected abstract fun setDesktopModeInternal(enabled: Boolean): Boolean

    /**
     * Clear cookies (platform-specific)
     * @return true if successful
     */
    protected abstract suspend fun clearCookiesInternal(): Boolean

    /**
     * Load gestures.js library into WebView
     * @return true if library is loaded
     */
    protected abstract suspend fun ensureGesturesLibrary(): Boolean

    /**
     * Check if WebView is available
     * @return true if WebView instance exists
     */
    protected abstract fun hasWebView(): Boolean

    /**
     * Log message (platform-specific)
     */
    protected abstract fun log(tag: String, message: String)

    /**
     * Log error (platform-specific)
     */
    protected abstract fun logError(tag: String, message: String, error: Throwable? = null)

    // ========== Common Helper Methods ==========

    /**
     * Execute a gesture script with library injection
     */
    protected suspend fun executeGestureScript(script: String): ActionResult {
        if (!ensureGesturesLibrary()) {
            return ActionResult.error("Failed to load gestures library")
        }

        if (!hasWebView()) {
            return ActionResult.error("No active WebView")
        }

        return try {
            val result = evaluateJavaScript(script)
            if (result?.contains("success\":true") == true) {
                ActionResult.success("Gesture executed")
            } else {
                ActionResult.error("Gesture failed: $result")
            }
        } catch (e: Exception) {
            ActionResult.error("Gesture error: ${e.message}")
        }
    }

    // ========== Navigation Implementation ==========

    override suspend fun goBack(): ActionResult {
        if (!hasWebView()) {
            return ActionResult.error("No active WebView")
        }

        if (!canGoBack()) {
            return ActionResult.error("Already at first page")
        }

        navigateBack()
        return ActionResult.success("Navigated back")
    }

    override suspend fun goForward(): ActionResult {
        if (!hasWebView()) {
            return ActionResult.error("No active WebView")
        }

        if (!canGoForward()) {
            return ActionResult.error("Already at last page")
        }

        navigateForward()
        return ActionResult.success("Navigated forward")
    }

    override suspend fun reload(): ActionResult {
        if (!hasWebView()) {
            return ActionResult.error("No active WebView")
        }
        reloadPage()
        return ActionResult.success("Page reloaded")
    }

    // ========== Zoom Implementation ==========

    override suspend fun zoomIn(): ActionResult {
        if (!hasWebView()) {
            return ActionResult.error("No active WebView")
        }

        zoomInWebView()
        tabViewModel.zoomIn()
        return ActionResult.success("Zoomed in")
    }

    override suspend fun zoomOut(): ActionResult {
        if (!hasWebView()) {
            return ActionResult.error("No active WebView")
        }

        zoomOutWebView()
        tabViewModel.zoomOut()
        return ActionResult.success("Zoomed out")
    }

    override suspend fun resetZoom(): ActionResult {
        if (!hasWebView()) {
            return ActionResult.error("No active WebView")
        }

        resetZoomWebView()
        tabViewModel.setZoomLevel(3) // 100%
        return ActionResult.success("Zoom reset to 100%")
    }

    // ========== Cookie Implementation ==========

    override suspend fun clearCookies(): ActionResult {
        return if (clearCookiesInternal()) {
            ActionResult.success("Cookies cleared")
        } else {
            ActionResult.error("Failed to clear cookies")
        }
    }
}
