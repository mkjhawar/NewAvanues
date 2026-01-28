/**
 * IElementClicker.kt - Element click operations interface
 *
 * Defines the contract for clicking UI elements during exploration.
 * Platform implementations handle the actual click mechanics.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.commandmanager

import com.augmentalis.commandmanager.ElementInfo

/**
 * Result of a click operation
 */
sealed class ClickResult {
    /** Click succeeded */
    data object Success : ClickResult()

    /** Click failed with reason */
    data class Failed(val reason: ClickFailure, val message: String? = null) : ClickResult()
}

/**
 * Interface for element click operations.
 *
 * Implementations provide platform-specific click mechanisms:
 * - Android: AccessibilityNodeInfo.performAction() + GestureDescription fallback
 * - iOS: XCUIElement tap
 * - Desktop: Platform-specific UI automation
 */
interface IElementClicker {

    /**
     * Click an element by its info.
     *
     * Strategy:
     * 1. Verify element is visible and enabled
     * 2. Attempt node-based click with retry
     * 3. Fall back to coordinate-based click if needed
     *
     * @param element Element to click
     * @return ClickResult indicating success or failure
     */
    suspend fun clickElement(element: ElementInfo): ClickResult

    /**
     * Click at specific screen coordinates.
     *
     * Used as fallback when node-based clicks fail.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return true if gesture dispatched successfully
     */
    fun clickAtCoordinates(x: Int, y: Int): Boolean

    /**
     * Press the back button/gesture.
     */
    suspend fun pressBack()

    /**
     * Dismiss soft keyboard if visible.
     */
    fun dismissKeyboard()

    /**
     * Refresh an element's native node reference.
     *
     * Accessibility nodes become stale quickly (~500ms).
     * This attempts to get a fresh reference.
     *
     * @param element Element to refresh
     * @return Updated ElementInfo with fresh node, or null if not found
     */
    fun refreshElement(element: ElementInfo): ElementInfo?

    /**
     * Get recorded click failures for telemetry.
     */
    fun getClickFailures(): List<ClickFailureReason>

    /**
     * Clear recorded click failures.
     */
    fun clearClickFailures()
}
