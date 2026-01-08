/**
 * IActionExecutor.kt - Action execution interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Defines the contract for executing voice command actions.
 * Platform-specific implementations handle actual UI interactions.
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand

// ═══════════════════════════════════════════════════════════════════
// SEGREGATED INTERFACES (ISP compliance)
// ═══════════════════════════════════════════════════════════════════

/**
 * Element-level action execution.
 */
interface IElementActionExecutor {
    /**
     * Tap/click an element by VUID.
     *
     * @param vuid Voice Universal ID of target element
     * @return ActionResult indicating success or failure
     */
    suspend fun tap(vuid: String): ActionResult

    /**
     * Long press an element by VUID.
     *
     * @param vuid Voice Universal ID of target element
     * @param durationMs Duration of long press in milliseconds
     * @return ActionResult indicating success or failure
     */
    suspend fun longPress(vuid: String, durationMs: Long = 500L): ActionResult

    /**
     * Focus an element by VUID.
     *
     * @param vuid Voice Universal ID of target element
     * @return ActionResult indicating success or failure
     */
    suspend fun focus(vuid: String): ActionResult

    /**
     * Enter text into a focused element.
     *
     * @param text Text to enter
     * @param vuid Optional VUID to focus first
     * @return ActionResult indicating success or failure
     */
    suspend fun enterText(text: String, vuid: String? = null): ActionResult
}

/**
 * Scroll action execution.
 */
interface IScrollActionExecutor {
    /**
     * Scroll in specified direction.
     *
     * @param direction Scroll direction
     * @param amount Scroll amount (0.0 - 1.0 = percentage of screen)
     * @param vuid Optional scrollable container VUID
     * @return ActionResult indicating success or failure
     */
    suspend fun scroll(
        direction: ScrollDirection,
        amount: Float = 0.5f,
        vuid: String? = null
    ): ActionResult
}

/**
 * Navigation action execution.
 */
interface INavigationActionExecutor {
    /**
     * Navigate back.
     */
    suspend fun back(): ActionResult

    /**
     * Navigate to home screen.
     */
    suspend fun home(): ActionResult

    /**
     * Show recent apps.
     */
    suspend fun recentApps(): ActionResult

    /**
     * Open app drawer.
     */
    suspend fun appDrawer(): ActionResult
}

/**
 * System action execution.
 */
interface ISystemActionExecutor {
    /**
     * Open system settings.
     */
    suspend fun openSettings(): ActionResult

    /**
     * Show notifications panel.
     */
    suspend fun showNotifications(): ActionResult

    /**
     * Clear all notifications.
     */
    suspend fun clearNotifications(): ActionResult

    /**
     * Take screenshot.
     */
    suspend fun screenshot(): ActionResult

    /**
     * Control flashlight.
     *
     * @param on True to turn on, false to turn off
     */
    suspend fun flashlight(on: Boolean): ActionResult
}

/**
 * Media action execution.
 */
interface IMediaActionExecutor {
    /**
     * Play/pause media.
     */
    suspend fun mediaPlayPause(): ActionResult

    /**
     * Next media track.
     */
    suspend fun mediaNext(): ActionResult

    /**
     * Previous media track.
     */
    suspend fun mediaPrevious(): ActionResult

    /**
     * Adjust volume.
     *
     * @param direction Volume adjustment direction
     */
    suspend fun volume(direction: VolumeDirection): ActionResult
}

/**
 * App-level action execution.
 */
interface IAppActionExecutor {
    /**
     * Open an app by type.
     *
     * @param appType Type of app (browser, camera, etc.)
     */
    suspend fun openApp(appType: String): ActionResult

    /**
     * Open an app by package name.
     *
     * @param packageName App package identifier
     */
    suspend fun openAppByPackage(packageName: String): ActionResult

    /**
     * Close current app.
     */
    suspend fun closeApp(): ActionResult
}

/**
 * Element lookup operations.
 */
interface IElementLookupExecutor {
    /**
     * Check if an element exists.
     *
     * @param vuid Voice Universal ID to check
     * @return True if element exists and is visible
     */
    suspend fun elementExists(vuid: String): Boolean

    /**
     * Get element bounds.
     *
     * @param vuid Voice Universal ID
     * @return ElementBounds or null if not found
     */
    suspend fun getElementBounds(vuid: String): ElementBounds?
}

/**
 * Generic command execution.
 */
interface ICommandExecutor {
    /**
     * Execute a quantized command.
     *
     * Routes to appropriate action based on command's actionType.
     *
     * @param command Quantized command to execute
     * @return ActionResult indicating success or failure
     */
    suspend fun executeCommand(command: QuantizedCommand): ActionResult

    /**
     * Execute an action by type with optional parameters.
     *
     * @param actionType Type of action to execute
     * @param params Additional parameters
     * @return ActionResult indicating success or failure
     */
    suspend fun executeAction(
        actionType: CommandActionType,
        params: Map<String, Any> = emptyMap()
    ): ActionResult
}

// ═══════════════════════════════════════════════════════════════════
// COMPOSITE INTERFACE
// ═══════════════════════════════════════════════════════════════════

/**
 * Composite interface for full action execution capability.
 * Extends all segregated interfaces for backward compatibility.
 *
 * Each platform implements this to translate commands into
 * actual UI/system interactions:
 * - Android: AccessibilityService actions
 * - iOS: UIAccessibility actions
 * - Desktop: Platform-specific automation
 */
interface IActionExecutor :
    IElementActionExecutor,
    IScrollActionExecutor,
    INavigationActionExecutor,
    ISystemActionExecutor,
    IMediaActionExecutor,
    IAppActionExecutor,
    IElementLookupExecutor,
    ICommandExecutor

/**
 * Scroll direction
 */
enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

/**
 * Volume adjustment direction
 */
enum class VolumeDirection {
    UP,
    DOWN,
    MUTE,
    UNMUTE
}

/**
 * Element bounds on screen
 */
data class ElementBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
}
