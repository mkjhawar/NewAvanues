/**
 * IOverlay.kt - Overlay system interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Defines the contract for VoiceOSCoreNG overlay rendering.
 * Platform implementations provide native rendering for each overlay type.
 */
package com.augmentalis.voiceoscoreng.features

/**
 * Base interface for all VoiceOSCoreNG overlays.
 *
 * Overlays provide visual feedback for voice commands:
 * - Status indicators (listening, processing, success, error)
 * - Confidence displays
 * - Numbered item selectors
 * - Context menus
 *
 * Implementations provide platform-specific rendering:
 * - Android: Custom Views or Compose overlays
 * - iOS: UIKit or SwiftUI views
 * - Desktop: Platform-native windows/overlays
 *
 * @see OverlayData for data types that can be displayed
 * @see CommandState for command execution states
 */
interface IOverlay {

    // ═══════════════════════════════════════════════════════════════════════
    // Identity
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Unique identifier for this overlay instance.
     *
     * Used for:
     * - Overlay management and lookup
     * - State persistence
     * - Debugging and logging
     */
    val id: String

    // ═══════════════════════════════════════════════════════════════════════
    // State
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Current visibility state of the overlay.
     *
     * @return true if the overlay is currently visible to the user
     */
    val isVisible: Boolean

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Control
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show the overlay to the user.
     *
     * After calling this method, [isVisible] should return true.
     * Has no effect if overlay is already disposed.
     */
    fun show()

    /**
     * Hide the overlay from the user.
     *
     * After calling this method, [isVisible] should return false.
     * The overlay remains in memory and can be shown again.
     */
    fun hide()

    /**
     * Toggle the overlay visibility.
     *
     * - If hidden, calls [show]
     * - If visible, calls [hide]
     *
     * Default implementation provided. Override for custom behavior.
     */
    fun toggle() {
        if (isVisible) hide() else show()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Data Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update the overlay with new data.
     *
     * The overlay should re-render to reflect the new data.
     * Has no effect if overlay is disposed.
     *
     * @param data The new data to display
     * @see OverlayData for available data types
     */
    fun update(data: OverlayData)

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dispose of the overlay and release all resources.
     *
     * After calling this method:
     * - [isVisible] returns false
     * - [show] has no effect
     * - [update] has no effect
     *
     * The overlay cannot be reused after disposal.
     */
    fun dispose()
}

// ═══════════════════════════════════════════════════════════════════════════
// Overlay Data Types
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Sealed class representing data that can be displayed in an overlay.
 *
 * Each subclass represents a different type of overlay content:
 * - [Status] - Command execution status with state indicator
 * - [Confidence] - Speech recognition confidence display
 * - [NumberedItems] - Numbered selection list for disambiguation
 * - [ContextMenu] - Menu of available actions
 */
sealed class OverlayData {

    /**
     * Status message with command execution state.
     *
     * Used for showing the current command lifecycle phase.
     *
     * @property message Human-readable status message
     * @property state Current command execution state
     */
    data class Status(
        val message: String,
        val state: CommandState
    ) : OverlayData()

    /**
     * Speech recognition confidence display.
     *
     * Shows the confidence level of voice recognition.
     *
     * @property value Confidence value (0.0 to 1.0)
     * @property text The recognized text
     */
    data class Confidence(
        val value: Float,
        val text: String
    ) : OverlayData()

    /**
     * Numbered items for selection.
     *
     * Used when multiple elements match a voice command,
     * allowing the user to say a number to select.
     *
     * @property items List of numbered items to display
     */
    data class NumberedItems(
        val items: List<NumberedItem>
    ) : OverlayData()

    /**
     * Context menu with selectable actions.
     *
     * Shows a menu of available voice commands or actions.
     *
     * @property items List of menu items
     * @property title Optional menu title
     */
    data class ContextMenu(
        val items: List<MenuItem>,
        val title: String?
    ) : OverlayData()
}

// ═══════════════════════════════════════════════════════════════════════════
// Command State
// ═══════════════════════════════════════════════════════════════════════════

/**
 * State of command execution in the voice processing pipeline.
 *
 * Represents the lifecycle stages of a voice command:
 * ```
 * LISTENING -> PROCESSING -> EXECUTING -> SUCCESS/ERROR
 * ```
 */
enum class CommandState {
    /**
     * Actively listening for voice input.
     * Microphone is active and awaiting speech.
     */
    LISTENING,

    /**
     * Processing recognized speech.
     * Speech-to-text complete, matching to commands.
     */
    PROCESSING,

    /**
     * Executing the matched command.
     * Action is being performed on the UI.
     */
    EXECUTING,

    /**
     * Command executed successfully.
     * Action completed without errors.
     */
    SUCCESS,

    /**
     * Command execution failed.
     * An error occurred during processing or execution.
     */
    ERROR
}

// ═══════════════════════════════════════════════════════════════════════════
// Selection Types
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Numbered item for selection overlay.
 *
 * Represents an on-screen element that can be selected by saying its number.
 * Used for disambiguation when multiple elements match a voice command.
 *
 * @property number The number assigned to this item (spoken by user to select)
 * @property label Human-readable label for the item
 * @property bounds Screen position and size of the element
 * @property isEnabled Whether the item can be selected (grayed out if false)
 * @property hasName Whether the item has a meaningful name (vs just a number)
 */
data class NumberedItem(
    val number: Int,
    val label: String,
    val bounds: Rect,
    val isEnabled: Boolean = true,
    val hasName: Boolean = true
)

/**
 * Menu item for context menu overlay.
 *
 * Represents an action in a context menu that can be selected
 * by voice or touch.
 *
 * @property id Unique identifier for this action
 * @property label Human-readable label
 * @property icon Optional icon identifier (platform-specific)
 * @property isEnabled Whether the item can be selected
 * @property number Optional number for voice selection
 */
data class MenuItem(
    val id: String,
    val label: String,
    val icon: String? = null,
    val isEnabled: Boolean = true,
    val number: Int? = null
)

// ═══════════════════════════════════════════════════════════════════════════
// Geometry
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Rectangle bounds in screen coordinates.
 *
 * Uses absolute pixel coordinates with origin at top-left.
 *
 * @property left X coordinate of left edge
 * @property top Y coordinate of top edge
 * @property right X coordinate of right edge
 * @property bottom Y coordinate of bottom edge
 */
data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    /**
     * Width of the rectangle in pixels.
     */
    val width: Int get() = right - left

    /**
     * Height of the rectangle in pixels.
     */
    val height: Int get() = bottom - top

    /**
     * X coordinate of the center point.
     */
    val centerX: Int get() = left + width / 2

    /**
     * Y coordinate of the center point.
     */
    val centerY: Int get() = top + height / 2

    companion object {
        /**
         * Empty rectangle at origin with zero size.
         */
        val EMPTY = Rect(0, 0, 0, 0)
    }
}
