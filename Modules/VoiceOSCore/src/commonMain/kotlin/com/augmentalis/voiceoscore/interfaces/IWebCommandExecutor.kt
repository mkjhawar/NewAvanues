/**
 * IWebCommandExecutor.kt - Interface for web command execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-02-10
 *
 * KMP-safe interface that decouples VoiceOSCore from WebView.
 * The WebAvanue module provides the platform-specific implementation
 * that evaluates JavaScript in the browser.
 */
package com.augmentalis.voiceoscore

/**
 * Executes web actions in the browser context.
 *
 * VoiceOSCore cannot import Android WebView (KMP constraint).
 * This interface allows [WebCommandHandler] to delegate execution
 * to the WebAvanue module's platform-specific implementation.
 */
interface IWebCommandExecutor {
    /**
     * Execute a web action (click, focus, scroll, gesture, etc.).
     *
     * @param action The action to perform with target selector and parameters
     * @return Result indicating success/failure with optional data
     */
    suspend fun executeWebAction(action: WebAction): WebActionResult

    /**
     * Check if the web context is currently active (browser in foreground with a loaded page).
     */
    fun isWebContextActive(): Boolean
}

/**
 * Describes an action to perform on a web page element.
 *
 * @property actionType The type of web action (CLICK, FOCUS, SCROLL_PAGE_UP, etc.)
 * @property selector CSS selector for the target element (empty for page-level actions)
 * @property xpath XPath for the target element (fallback if selector fails)
 * @property text Text value for INPUT actions
 * @property params Additional parameters (angle, direction, distance for gestures)
 */
data class WebAction(
    val actionType: WebActionType,
    val selector: String = "",
    val xpath: String = "",
    val text: String = "",
    val params: Map<String, String> = emptyMap()
)

/**
 * Types of actions that can be performed on web page elements.
 */
enum class WebActionType {
    // ═══════════════════════════════════════════════════════════════════
    // Element Actions
    // ═══════════════════════════════════════════════════════════════════
    CLICK,
    FOCUS,
    INPUT,
    SCROLL_TO,
    TOGGLE,
    SELECT,
    LONG_PRESS,
    DOUBLE_CLICK,
    HOVER,

    // ═══════════════════════════════════════════════════════════════════
    // Page Navigation
    // ═══════════════════════════════════════════════════════════════════
    SCROLL_PAGE_UP,
    SCROLL_PAGE_DOWN,
    SCROLL_TO_TOP,
    SCROLL_TO_BOTTOM,
    PAGE_BACK,
    PAGE_FORWARD,
    PAGE_REFRESH,

    // ═══════════════════════════════════════════════════════════════════
    // Form Navigation
    // ═══════════════════════════════════════════════════════════════════
    TAB_NEXT,
    TAB_PREV,
    SUBMIT_FORM,

    // ═══════════════════════════════════════════════════════════════════
    // Gesture Actions
    // ═══════════════════════════════════════════════════════════════════
    SWIPE_LEFT,
    SWIPE_RIGHT,
    SWIPE_UP,
    SWIPE_DOWN,
    GRAB,
    RELEASE,
    ROTATE,
    DRAG,
    ZOOM_IN,
    ZOOM_OUT,

    // ═══════════════════════════════════════════════════════════════════
    // Text/Clipboard Actions
    // ═══════════════════════════════════════════════════════════════════
    COPY,
    CUT,
    PASTE,
    SELECT_ALL
}

/**
 * Result of a web action execution.
 *
 * @property success Whether the action completed successfully
 * @property message Human-readable result message
 * @property data Additional result data (element state, etc.)
 */
data class WebActionResult(
    val success: Boolean,
    val message: String = "",
    val data: Map<String, String> = emptyMap()
)
