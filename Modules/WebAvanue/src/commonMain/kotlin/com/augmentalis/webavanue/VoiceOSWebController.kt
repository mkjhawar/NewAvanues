package com.augmentalis.webavanue

import com.augmentalis.webavanue.ActionResult

/**
 * VoiceOSWebController - Coordinates WebView operations for VoiceOS commands
 *
 * This controller acts as a bridge between VoiceOS CommandManager and the
 * platform-specific WebView implementation. It provides immediate WebView
 * control for voice commands while coordinating with TabViewModel for state
 * persistence.
 *
 * Note: This is different from WebViewController (expect class in WebViewContainer.kt)
 * which provides direct platform-specific WebView control. This interface is specifically
 * for VoiceOS integration with ActionResult feedback.
 *
 * Architecture:
 * - VoiceOS CommandManager → WebAvanueActionMapper → VoiceOSWebController → WebView
 * - VoiceOSWebController → TabViewModel (for state persistence)
 *
 * Responsibilities:
 * - Execute immediate WebView operations (scroll, zoom, navigation)
 * - Coordinate with TabViewModel for state persistence
 * - Return ActionResult for voice feedback
 * - Manage active WebView instance lifecycle
 *
 * See: WebAvanueActionMapper, TabViewModel, AndroidWebView, WebViewController
 */
interface VoiceOSWebController {

    // ========== Scrolling ==========

    /**
     * Scroll up by one viewport height
     * @return ActionResult indicating success/failure
     */
    suspend fun scrollUp(): ActionResult

    /**
     * Scroll down by one viewport height
     * @return ActionResult indicating success/failure
     */
    suspend fun scrollDown(): ActionResult

    /**
     * Scroll left by one viewport width
     * @return ActionResult indicating success/failure
     */
    suspend fun scrollLeft(): ActionResult

    /**
     * Scroll right by one viewport width
     * @return ActionResult indicating success/failure
     */
    suspend fun scrollRight(): ActionResult

    /**
     * Scroll to top of page
     * @return ActionResult indicating success/failure
     */
    suspend fun scrollTop(): ActionResult

    /**
     * Scroll to bottom of page
     * @return ActionResult indicating success/failure
     */
    suspend fun scrollBottom(): ActionResult

    // ========== Navigation ==========

    /**
     * Navigate back in history
     * @return ActionResult indicating success/failure
     */
    suspend fun goBack(): ActionResult

    /**
     * Navigate forward in history
     * @return ActionResult indicating success/failure
     */
    suspend fun goForward(): ActionResult

    /**
     * Reload current page
     * @return ActionResult indicating success/failure
     */
    suspend fun reload(): ActionResult

    // ========== Zoom ==========

    /**
     * Zoom in (increase text size)
     * @return ActionResult indicating success/failure
     */
    suspend fun zoomIn(): ActionResult

    /**
     * Zoom out (decrease text size)
     * @return ActionResult indicating success/failure
     */
    suspend fun zoomOut(): ActionResult

    /**
     * Reset zoom to 100%
     * @return ActionResult indicating success/failure
     */
    suspend fun resetZoom(): ActionResult

    /**
     * Set specific zoom level
     * @param level Zoom percentage (50-200)
     * @return ActionResult indicating success/failure
     */
    suspend fun setZoomLevel(level: Int): ActionResult

    // ========== Desktop Mode ==========

    /**
     * Enable or disable desktop mode (user agent)
     * @param enabled true for desktop mode, false for mobile
     * @return ActionResult indicating success/failure
     */
    suspend fun setDesktopMode(enabled: Boolean): ActionResult

    // ========== Page Control ==========

    /**
     * Toggle page freeze (stop JavaScript execution)
     * @return ActionResult indicating success/failure
     */
    suspend fun toggleFreeze(): ActionResult

    /**
     * Clear all cookies
     * @return ActionResult indicating success/failure
     */
    suspend fun clearCookies(): ActionResult

    // ========== Page Info ==========

    /**
     * Get current page URL
     * @return Current URL string
     */
    fun getCurrentUrl(): String

    /**
     * Get current page title
     * @return Current page title string
     */
    fun getCurrentTitle(): String

    // ========== Gestures ==========

    /**
     * Perform single click at center
     * @return ActionResult indicating success/failure
     */
    suspend fun performClick(): ActionResult

    /**
     * Perform double click at center
     * @return ActionResult indicating success/failure
     */
    suspend fun performDoubleClick(): ActionResult

    /**
     * Start drag operation
     * @return ActionResult indicating success/failure
     */
    suspend fun startDrag(): ActionResult

    /**
     * Stop drag operation
     * @return ActionResult indicating success/failure
     */
    suspend fun stopDrag(): ActionResult

    /**
     * Select text
     * @return ActionResult indicating success/failure
     */
    suspend fun select(): ActionResult

    // ========== Universal Gesture Support (IPC) ==========

    /**
     * Perform gesture with coordinates and modifiers
     *
     * Universal gesture handler for all 80+ VoiceOS gesture types.
     * Supports coordinate-based execution with fallback to viewport center.
     *
     * IPC Format: VCM:gestureId:GESTURE_TYPE:x:y:modifiers
     *
     * @param gestureType Gesture type (e.g., "GESTURE_CLICK", "GESTURE_DRAG_START")
     * @param x X coordinate (use -1 for automatic positioning)
     * @param y Y coordinate (use -1 for automatic positioning)
     * @param modifiers Keyboard modifiers as bitmask (shift/ctrl/alt/meta)
     * @return ActionResult indicating success/failure
     */
    suspend fun performGesture(
        gestureType: String,
        x: Float,
        y: Float,
        modifiers: Int = 0
    ): ActionResult

    // ========== Find in Page ==========

    /**
     * Find all occurrences of text in the page
     *
     * Searches the page content for the given text and highlights all matches.
     * Returns the total number of matches found.
     *
     * @param query Search query string
     * @param caseSensitive Whether to match case
     * @param onResultsFound Callback with (currentMatch, totalMatches)
     * @return ActionResult indicating success/failure
     */
    suspend fun findInPage(
        query: String,
        caseSensitive: Boolean,
        onResultsFound: (currentMatch: Int, totalMatches: Int) -> Unit
    ): ActionResult

    /**
     * Find next match in the page
     *
     * Navigates to the next occurrence of the current search query.
     * Wraps around from last to first match.
     *
     * @return ActionResult indicating success/failure
     */
    suspend fun findNext(): ActionResult

    /**
     * Find previous match in the page
     *
     * Navigates to the previous occurrence of the current search query.
     * Wraps around from first to last match.
     *
     * @return ActionResult indicating success/failure
     */
    suspend fun findPrevious(): ActionResult

    /**
     * Clear find in page highlights
     *
     * Removes all search highlights from the page.
     *
     * @return ActionResult indicating success/failure
     */
    suspend fun clearFindMatches(): ActionResult
}
