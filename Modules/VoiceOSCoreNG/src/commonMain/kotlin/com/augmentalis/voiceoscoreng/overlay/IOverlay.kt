/**
 * IOverlay.kt - Overlay system interface for VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Defines the contract for VoiceOSCoreNG overlay system.
 * Platform implementations provide native rendering capabilities.
 *
 * This interface focuses on overlay positioning, sizing, and theming.
 * For data-driven overlay content, see features.IOverlay.
 */
package com.augmentalis.voiceoscoreng.overlay

import com.augmentalis.voiceoscoreng.features.OverlayTheme

/**
 * Base interface for VoiceOSCoreNG overlay system.
 *
 * Provides the contract for overlays that can be:
 * - Shown and hidden (visibility control)
 * - Positioned on screen (x, y coordinates)
 * - Sized (width, height dimensions)
 * - Themed (visual styling via OverlayTheme)
 * - Disposed (resource cleanup)
 *
 * Each overlay has:
 * - A unique [id] for identification and management
 * - A [zIndex] for layering control (higher = front)
 * - An [isVisible] state indicating current visibility
 *
 * ## Lifecycle
 *
 * ```
 * Created -> (show/hide)* -> disposed
 * ```
 *
 * After [dispose] is called:
 * - [show], [updatePosition], [updateSize], [setTheme] have no effect
 * - [isVisible] returns false
 * - The overlay cannot be reused
 *
 * ## Usage Example
 *
 * ```kotlin
 * val overlay: IOverlay = createOverlay("my-overlay", zIndex = 10)
 *
 * // Configure
 * overlay.setTheme(OverlayTheme.DARK)
 * overlay.updatePosition(100, 200)
 * overlay.updateSize(300, 150)
 *
 * // Show
 * overlay.show()
 *
 * // Later...
 * overlay.hide()
 *
 * // Cleanup
 * overlay.dispose()
 * ```
 *
 * ## Platform Implementations
 *
 * - Android: WindowManager-based overlays or Compose overlays
 * - iOS: UIWindow or SwiftUI-based views
 * - Desktop: Platform-native floating windows
 *
 * @see OverlayTheme for theming options
 */
interface IOverlay {

    // ═══════════════════════════════════════════════════════════════════════
    // Identity Properties
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Unique identifier for this overlay instance.
     *
     * Used for:
     * - Overlay management and lookup
     * - State persistence
     * - Debugging and logging
     *
     * This value is stable and does not change over the overlay's lifetime.
     */
    val id: String

    /**
     * Current visibility state of the overlay.
     *
     * @return true if the overlay is currently visible to the user
     */
    val isVisible: Boolean

    /**
     * Z-index for overlay layering.
     *
     * Higher values render in front of lower values.
     * Used to control overlay stacking order when multiple overlays
     * are visible simultaneously.
     *
     * Common z-index conventions:
     * - 0-9: Background overlays
     * - 10-49: Standard overlays
     * - 50-99: Priority overlays (e.g., status indicators)
     * - 100+: Critical overlays (e.g., error dialogs)
     *
     * This value is stable and does not change over the overlay's lifetime.
     */
    val zIndex: Int

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Control
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show the overlay to the user.
     *
     * After calling this method, [isVisible] should return true.
     *
     * This method is idempotent - calling it multiple times when already
     * visible has no additional effect.
     *
     * Has no effect if overlay is already disposed.
     */
    fun show()

    /**
     * Hide the overlay from the user.
     *
     * After calling this method, [isVisible] should return false.
     *
     * The overlay remains in memory and can be shown again by calling [show].
     *
     * This method is idempotent - calling it multiple times when already
     * hidden has no additional effect.
     */
    fun hide()

    // ═══════════════════════════════════════════════════════════════════════
    // Positioning
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update the screen position of the overlay.
     *
     * Coordinates are in screen pixels with origin at top-left of the display.
     * Negative values are allowed and may position the overlay partially
     * off-screen.
     *
     * Has no effect if overlay is already disposed.
     *
     * @param x Horizontal position in pixels from left edge of screen
     * @param y Vertical position in pixels from top edge of screen
     */
    fun updatePosition(x: Int, y: Int)

    // ═══════════════════════════════════════════════════════════════════════
    // Sizing
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update the dimensions of the overlay.
     *
     * Dimensions are in screen pixels. Zero values are allowed and may
     * result in the overlay being invisible even when [isVisible] is true.
     *
     * Has no effect if overlay is already disposed.
     *
     * @param width Width in pixels
     * @param height Height in pixels
     */
    fun updateSize(width: Int, height: Int)

    // ═══════════════════════════════════════════════════════════════════════
    // Theming
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Apply a theme to the overlay.
     *
     * The overlay should re-render to reflect the new theme styling.
     * Common themes include:
     * - [OverlayTheme.DEFAULT] - Standard light theme
     * - [OverlayTheme.DARK] - Dark mode theme
     * - [OverlayTheme.HIGH_CONTRAST] - Accessibility high contrast
     *
     * Has no effect if overlay is already disposed.
     *
     * @param theme The theme configuration to apply
     * @see OverlayTheme for available theme options and customization
     */
    fun setTheme(theme: OverlayTheme)

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dispose of the overlay and release all resources.
     *
     * After calling this method:
     * - [isVisible] returns false
     * - [show] has no effect
     * - [updatePosition] has no effect
     * - [updateSize] has no effect
     * - [setTheme] has no effect
     *
     * This method is idempotent - calling it multiple times is safe.
     *
     * The overlay cannot be reused after disposal. Create a new instance
     * if needed.
     */
    fun dispose()
}
