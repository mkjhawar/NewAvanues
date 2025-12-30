package com.augmentalis.Avanues.web.universal.presentation.controller

import com.augmentalis.Avanues.web.universal.commands.ActionResult

/**
 * WebViewController - Coordinates WebView operations for VoiceOS commands
 *
 * This controller acts as a bridge between VoiceOS CommandManager and the
 * platform-specific WebView implementation. It provides immediate WebView
 * control for voice commands while coordinating with TabViewModel for state
 * persistence.
 *
 * Architecture:
 * - VoiceOS CommandManager → WebAvanueActionMapper → WebViewController → WebView
 * - WebViewController → TabViewModel (for state persistence)
 *
 * Responsibilities:
 * - Execute immediate WebView operations (scroll, zoom, navigation)
 * - Coordinate with TabViewModel for state persistence
 * - Return ActionResult for voice feedback
 * - Manage active WebView instance lifecycle
 *
 * See: WebAvanueActionMapper, TabViewModel, AndroidWebView
 */
interface WebViewController {

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

    // ========== Web Action Extraction (AVA Integration) ==========

    /**
     * Extract actionable elements from current page
     *
     * Scans the page for buttons, links, inputs, and other interactive
     * elements and returns them as voice-friendly commands.
     *
     * @return JSON string containing extracted actions, or null on failure
     */
    suspend fun extractWebActions(): String?

    /**
     * Get simplified voice commands for current page
     *
     * @return JSON string containing voice commands, or null on failure
     */
    suspend fun getVoiceCommands(): String?

    /**
     * Click element by voice command
     *
     * @param command Voice command to match (e.g., "submit", "login")
     * @return ActionResult with success/failure and details
     */
    suspend fun clickByVoiceCommand(command: String): ActionResult

    /**
     * Type text into focused or specified input element
     *
     * @param text Text to type
     * @param selector Optional CSS selector for target element
     * @return ActionResult indicating success/failure
     */
    suspend fun typeText(text: String, selector: String? = null): ActionResult
}
