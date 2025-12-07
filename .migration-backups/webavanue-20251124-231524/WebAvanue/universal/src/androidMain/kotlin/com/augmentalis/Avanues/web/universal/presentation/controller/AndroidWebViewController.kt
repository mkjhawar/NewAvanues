package com.augmentalis.Avanues.web.universal.presentation.controller

import android.webkit.CookieManager
import com.augmentalis.Avanues.web.universal.commands.ActionResult
import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabViewModel
import com.augmentalis.webavanue.platform.WebView
import kotlinx.coroutines.flow.firstOrNull

/**
 * Android implementation of WebViewController
 *
 * Coordinates between VoiceOS commands, WebView operations, and TabViewModel
 * state persistence. Provides immediate WebView control with dual-call pattern:
 * 1. Immediate WebView operation (for instant UI feedback)
 * 2. TabViewModel persistence (for state restoration)
 *
 * @param webViewProvider Function that returns the current active WebView instance
 * @param tabViewModel ViewModel for tab state persistence
 */
class AndroidWebViewController(
    private val webViewProvider: () -> WebView?,
    private val tabViewModel: TabViewModel
) : WebViewController {

    private val webView: WebView?
        get() = webViewProvider()

    // ========== Scrolling ==========
    // Note: Scroll operations delegate to TabViewModel which executes JavaScript
    // TabViewModel handles both immediate scrolling and state persistence

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

    // ========== Navigation ==========

    override suspend fun goBack(): ActionResult {
        val view = webView ?: return ActionResult.error("No active WebView")

        // Check if can go back
        val canGoBack = view.canGoBack.firstOrNull() ?: false
        if (!canGoBack) {
            return ActionResult.error("Already at first page")
        }

        view.goBack()
        return ActionResult.success("Navigated back")
    }

    override suspend fun goForward(): ActionResult {
        val view = webView ?: return ActionResult.error("No active WebView")

        // Check if can go forward
        val canGoForward = view.canGoForward.firstOrNull() ?: false
        if (!canGoForward) {
            return ActionResult.error("Already at last page")
        }

        view.goForward()
        return ActionResult.success("Navigated forward")
    }

    override suspend fun reload(): ActionResult {
        webView?.reload() ?: return ActionResult.error("No active WebView")
        return ActionResult.success("Page reloaded")
    }

    // ========== Zoom ==========

    override suspend fun zoomIn(): ActionResult {
        // Immediate WebView operation
        webView?.zoomIn() ?: return ActionResult.error("No active WebView")

        // Persist to TabViewModel for restoration
        tabViewModel.zoomIn()

        return ActionResult.success("Zoomed in")
    }

    override suspend fun zoomOut(): ActionResult {
        // Immediate WebView operation
        webView?.zoomOut() ?: return ActionResult.error("No active WebView")

        // Persist to TabViewModel for restoration
        tabViewModel.zoomOut()

        return ActionResult.success("Zoomed out")
    }

    override suspend fun resetZoom(): ActionResult {
        // Reset zoom via WebView
        webView?.resetZoom() ?: return ActionResult.error("No active WebView")

        // Persist zoom level 3 (100%) to TabViewModel
        tabViewModel.setZoomLevel(3)

        return ActionResult.success("Zoom reset to 100%")
    }

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

    // ========== Desktop Mode ==========

    override suspend fun setDesktopMode(enabled: Boolean): ActionResult {
        // Immediate WebView operation
        webView?.setDesktopMode(enabled) ?: return ActionResult.error("No active WebView")

        // Persist to TabViewModel for restoration
        tabViewModel.setDesktopMode(enabled)

        val mode = if (enabled) "desktop" else "mobile"
        return ActionResult.success("Switched to $mode mode")
    }

    // ========== Page Control ==========

    override suspend fun toggleFreeze(): ActionResult {
        // Note: Page freeze not yet implemented in WebView
        // This would pause/resume JavaScript execution
        return ActionResult.error("Page freeze not yet implemented")
    }

    override suspend fun clearCookies(): ActionResult {
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeAllCookies { success ->
            if (!success) {
                android.util.Log.w("WebViewController", "Failed to clear cookies")
            }
        }
        cookieManager.flush()
        return ActionResult.success("Cookies cleared")
    }

    // ========== Page Info ==========

    override fun getCurrentUrl(): String {
        return webView?.currentUrl?.value ?: ""
    }

    override fun getCurrentTitle(): String {
        return webView?.pageTitle?.value ?: ""
    }

    // ========== Gestures ==========

    /**
     * Inject gestures.js library into WebView if not already loaded
     */
    private suspend fun ensureGesturesLibrary(): Boolean {
        val view = webView ?: return false

        // Check if library is already loaded
        val isLoaded = view.evaluateJavaScript("typeof window.AvanuesGestures !== 'undefined'")
        if (isLoaded == "true") return true

        // Load gestures.js from resources
        val gesturesJs = try {
            this::class.java.classLoader?.getResourceAsStream("gestures.js")?.bufferedReader()?.readText()
        } catch (e: Exception) {
            android.util.Log.e("WebViewController", "Failed to load gestures.js", e)
            return false
        }

        if (gesturesJs == null) {
            android.util.Log.e("WebViewController", "gestures.js not found in resources")
            return false
        }

        // Inject library
        view.evaluateJavaScript(gesturesJs)
        return true
    }

    /**
     * Execute gesture via JavaScript injection
     */
    private suspend fun executeGesture(script: String): ActionResult {
        if (!ensureGesturesLibrary()) {
            return ActionResult.error("Failed to load gestures library")
        }

        val view = webView ?: return ActionResult.error("No active WebView")

        try {
            val result = view.evaluateJavaScript(script)
            return if (result?.contains("success\":true") == true) {
                ActionResult.success("Gesture executed")
            } else {
                ActionResult.error("Gesture failed: $result")
            }
        } catch (e: Exception) {
            return ActionResult.error("Gesture error: ${e.message}")
        }
    }

    override suspend fun performClick(): ActionResult {
        val coords = getGestureCoordinates(-1f, -1f) // Use center if no coords
        return executeGesture("JSON.stringify(window.AvanuesGestures.click(${coords.first}, ${coords.second}))")
    }

    override suspend fun performDoubleClick(): ActionResult {
        val coords = getGestureCoordinates(-1f, -1f)
        return executeGesture("JSON.stringify(window.AvanuesGestures.doubleClick(${coords.first}, ${coords.second}))")
    }

    override suspend fun startDrag(): ActionResult {
        val coords = getGestureCoordinates(-1f, -1f)
        return executeGesture("JSON.stringify(window.AvanuesGestures.dragStart(${coords.first}, ${coords.second}))")
    }

    override suspend fun stopDrag(): ActionResult {
        val coords = getGestureCoordinates(-1f, -1f)
        return executeGesture("JSON.stringify(window.AvanuesGestures.dragEnd(${coords.first}, ${coords.second}))")
    }

    override suspend fun select(): ActionResult {
        val coords = getGestureCoordinates(-1f, -1f)
        return executeGesture("JSON.stringify(window.AvanuesGestures.selectWord(${coords.first}, ${coords.second}))")
    }

    // ========== Universal Gesture Support (IPC) ==========

    /**
     * Perform gesture with coordinates and modifiers
     *
     * Coordinate Priority:
     * 1. Provided coordinates (if x >= 0 && y >= 0)
     * 2. Last touch point (if available)
     * 3. Viewport center (fallback)
     *
     * @param gestureType Gesture type from VoiceOS (e.g., "GESTURE_CLICK")
     * @param x X coordinate (-1 for automatic)
     * @param y Y coordinate (-1 for automatic)
     * @param modifiers Keyboard modifier bitmask
     * @return ActionResult indicating success/failure
     */
    override suspend fun performGesture(
        gestureType: String,
        x: Float,
        y: Float,
        modifiers: Int
    ): ActionResult {
        // Get final coordinates with fallback logic
        val coords = getGestureCoordinates(x, y)

        val script = when (gestureType) {
            // Basic pointer gestures
            "GESTURE_CLICK" -> "window.AvanuesGestures.click(${coords.first}, ${coords.second})"
            "GESTURE_DOUBLE_CLICK" -> "window.AvanuesGestures.doubleClick(${coords.first}, ${coords.second})"
            "GESTURE_LONG_PRESS" -> "window.AvanuesGestures.longPress(${coords.first}, ${coords.second})"
            "GESTURE_TAP" -> "window.AvanuesGestures.tap(${coords.first}, ${coords.second})"

            // Drag gestures
            "GESTURE_DRAG_START" -> "window.AvanuesGestures.dragStart(${coords.first}, ${coords.second})"
            "GESTURE_DRAG_MOVE" -> "window.AvanuesGestures.dragMove(${coords.first}, ${coords.second})"
            "GESTURE_DRAG_END" -> "window.AvanuesGestures.dragEnd(${coords.first}, ${coords.second})"

            // Swipe gestures
            "GESTURE_SWIPE_LEFT" -> "window.AvanuesGestures.swipeLeft(${coords.first}, ${coords.second})"
            "GESTURE_SWIPE_RIGHT" -> "window.AvanuesGestures.swipeRight(${coords.first}, ${coords.second})"
            "GESTURE_SWIPE_UP" -> "window.AvanuesGestures.swipeUp(${coords.first}, ${coords.second})"
            "GESTURE_SWIPE_DOWN" -> "window.AvanuesGestures.swipeDown(${coords.first}, ${coords.second})"

            // Selection gestures
            "GESTURE_SELECT_START" -> "window.AvanuesGestures.selectStart(${coords.first}, ${coords.second})"
            "GESTURE_SELECT_EXTEND" -> "window.AvanuesGestures.selectExtend(${coords.first}, ${coords.second})"
            "GESTURE_SELECT_WORD" -> "window.AvanuesGestures.selectWord(${coords.first}, ${coords.second})"
            "GESTURE_SELECT_ALL" -> "window.AvanuesGestures.selectAll()"
            "GESTURE_CLEAR_SELECTION" -> "window.AvanuesGestures.clearSelection()"

            // Clipboard gestures
            "GESTURE_COPY" -> "await window.AvanuesGestures.copy()"
            "GESTURE_CUT" -> "await window.AvanuesGestures.cut()"
            "GESTURE_PASTE" -> "await window.AvanuesGestures.paste(${coords.first}, ${coords.second})"

            // 3D transform gestures
            "GESTURE_ROTATE_X" -> "window.AvanuesGestures.rotateX(${coords.first}, ${coords.second}, $modifiers)"
            "GESTURE_ROTATE_Y" -> "window.AvanuesGestures.rotateY(${coords.first}, ${coords.second}, $modifiers)"
            "GESTURE_ROTATE_Z" -> "window.AvanuesGestures.rotateZ(${coords.first}, ${coords.second}, $modifiers)"
            "GESTURE_PAN" -> "window.AvanuesGestures.pan(${coords.first}, ${coords.second})"
            "GESTURE_TILT" -> "window.AvanuesGestures.tilt(${coords.first}, ${coords.second}, $modifiers)"
            "GESTURE_ORBIT" -> "window.AvanuesGestures.orbit(${coords.first}, ${coords.second}, $modifiers, 0)"

            // Zoom/Scale gestures
            "GESTURE_ZOOM_IN" -> "window.AvanuesGestures.zoomIn(${coords.first}, ${coords.second})"
            "GESTURE_ZOOM_OUT" -> "window.AvanuesGestures.zoomOut(${coords.first}, ${coords.second})"
            "GESTURE_RESET_ZOOM" -> "window.AvanuesGestures.resetZoom()"
            "GESTURE_SCALE" -> "window.AvanuesGestures.scale(${coords.first}, ${coords.second}, ${modifiers / 100.0})"

            // Scrolling gestures
            "GESTURE_SCROLL_UP" -> "window.AvanuesGestures.scrollBy(0, -100)"
            "GESTURE_SCROLL_DOWN" -> "window.AvanuesGestures.scrollBy(0, 100)"
            "GESTURE_SCROLL_LEFT" -> "window.AvanuesGestures.scrollBy(-100, 0)"
            "GESTURE_SCROLL_RIGHT" -> "window.AvanuesGestures.scrollBy(100, 0)"
            "GESTURE_SCROLL_TO_TOP" -> "window.AvanuesGestures.scrollToTop()"
            "GESTURE_SCROLL_TO_BOTTOM" -> "window.AvanuesGestures.scrollToBottom()"
            "GESTURE_PAGE_UP" -> "window.AvanuesGestures.pageUp()"
            "GESTURE_PAGE_DOWN" -> "window.AvanuesGestures.pageDown()"
            "GESTURE_FLING" -> "window.AvanuesGestures.fling($modifiers, 'down')"

            // Grab gestures
            "GESTURE_GRAB" -> "window.AvanuesGestures.grab(${coords.first}, ${coords.second})"
            "GESTURE_RELEASE" -> "window.AvanuesGestures.release()"
            "GESTURE_THROW" -> "window.AvanuesGestures.throwElement(${coords.first}, ${coords.second})"

            // Drawing gestures
            "GESTURE_STROKE_START" -> "window.AvanuesGestures.strokeStart(${coords.first}, ${coords.second})"
            "GESTURE_STROKE_MOVE" -> "window.AvanuesGestures.strokeMove(${coords.first}, ${coords.second})"
            "GESTURE_STROKE_END" -> "window.AvanuesGestures.strokeEnd()"
            "GESTURE_ERASE" -> "window.AvanuesGestures.erase(${coords.first}, ${coords.second})"

            // Focus & Input gestures
            "GESTURE_FOCUS" -> "window.AvanuesGestures.focus(${coords.first}, ${coords.second})"
            "GESTURE_HOVER" -> "window.AvanuesGestures.hover(${coords.first}, ${coords.second})"
            "GESTURE_HOVER_OUT" -> "window.AvanuesGestures.hoverOut(${coords.first}, ${coords.second})"

            else -> return ActionResult.error("Unknown gesture type: $gestureType")
        }

        return executeGesture("JSON.stringify($script)")
    }

    /**
     * Get gesture coordinates with fallback logic
     *
     * Priority:
     * 1. Provided coordinates (if both x >= 0 && y >= 0)
     * 2. Last touch point (future: tracked by WebView)
     * 3. Viewport center (fallback)
     *
     * @param x Provided X coordinate (-1 for automatic)
     * @param y Provided Y coordinate (-1 for automatic)
     * @return Pair of (x, y) coordinates in viewport pixels
     */
    private fun getGestureCoordinates(x: Float, y: Float): Pair<Float, Float> {
        // Priority 1: Use provided coordinates if valid
        if (x >= 0f && y >= 0f) {
            return Pair(x, y)
        }

        // Priority 2: Use last touch point (future enhancement)
        // TODO: Track last touch point in WebView for voice-first interaction
        // This would require exposing touch tracking in WebView interface
        // val lastTouch = webView?.getLastTouchPoint()
        // if (lastTouch != null) {
        //     return Pair(lastTouch.x, lastTouch.y)
        // }

        // Priority 3: Fallback to viewport center
        // Note: WebView interface doesn't expose dimensions yet
        // TODO: Add viewport dimension tracking to WebView interface
        // For now, use standard mobile viewport center as fallback
        val centerX = 180f  // Approximate center X for typical mobile screen
        val centerY = 320f  // Approximate center Y for typical mobile screen

        return Pair(centerX, centerY)
    }
}
