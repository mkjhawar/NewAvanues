/**
 * BaseOverlay.kt - Base overlay infrastructure for VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2026-01-06
 *
 * KMP-compatible base class for all overlay implementations.
 * Provides common visibility management, lifecycle callbacks, and position handling.
 *
 * Ported from VoiceOSCore legacy implementation with KMP compatibility:
 * - Removed Android-specific WindowManager dependencies
 * - Added StateFlow for reactive visibility updates
 * - Platform-agnostic position management
 * - Coroutine scope for async operations
 *
 * Platform-specific subclasses should:
 * - Override lifecycle callbacks (onShow, onHide, onDispose)
 * - Implement update() for data rendering
 * - Use platform-native APIs for actual overlay display
 */
package com.augmentalis.voiceoscoreng.features

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Overlay type determining window behavior.
 *
 * Used by platform implementations to configure overlay appearance:
 * - Window flags (focusable, touchable, etc.)
 * - Layout parameters (size, gravity)
 * - Input handling behavior
 */
enum class OverlayType {
    /**
     * Full screen overlay covering entire display.
     * Used for modal dialogs, loading screens.
     */
    FULLSCREEN,

    /**
     * Small floating window that can be interacted with.
     * Used for status indicators, quick actions.
     */
    FLOATING,

    /**
     * Positioned overlay at specific coordinates.
     * Used for tooltips, number badges on elements.
     * Often non-interactive (pass-through touch).
     */
    POSITIONED
}

/**
 * Base class for all overlay implementations.
 *
 * Provides common visibility management and lifecycle callbacks.
 * Subclasses should implement platform-specific rendering.
 *
 * ## Usage
 *
 * ```kotlin
 * class AndroidStatusOverlay(
 *     context: Context
 * ) : BaseOverlay("status-overlay", OverlayType.FLOATING) {
 *
 *     private var windowManager: WindowManager? = null
 *     private var overlayView: View? = null
 *
 *     override fun onShow() {
 *         // Create and add view to WindowManager
 *     }
 *
 *     override fun onHide() {
 *         // Remove view from WindowManager
 *     }
 *
 *     override fun onDispose() {
 *         // Release all resources
 *     }
 *
 *     override fun update(data: OverlayData) {
 *         // Update view content
 *     }
 * }
 * ```
 *
 * ## Lifecycle
 *
 * ```
 * [Created] --> show() --> [Visible] --> hide() --> [Hidden]
 *                                  ^                    |
 *                                  |____________________v
 *                                          show()
 *
 * [Any State] --> dispose() --> [Disposed]
 * ```
 *
 * @param id Unique identifier for this overlay instance
 * @param overlayType Type of overlay determining window behavior
 */
abstract class BaseOverlay(
    final override val id: String,
    protected val overlayType: OverlayType = OverlayType.FLOATING
) : IOverlay {

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility State
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Internal mutable visibility state.
     */
    private val _isVisible = MutableStateFlow(false)

    /**
     * Current visibility state.
     * @return true if overlay is currently visible
     */
    override val isVisible: Boolean
        get() = _isVisible.value

    /**
     * Observable visibility state as StateFlow.
     *
     * Use this to reactively observe visibility changes:
     * ```kotlin
     * overlay.visibilityFlow.collect { visible ->
     *     println("Overlay is now ${if (visible) "visible" else "hidden"}")
     * }
     * ```
     */
    val visibilityFlow: StateFlow<Boolean> = _isVisible.asStateFlow()

    // ═══════════════════════════════════════════════════════════════════════
    // Coroutine Scope
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Coroutine scope for async operations within this overlay.
     *
     * Uses SupervisorJob so failures in one coroutine don't cancel siblings.
     * Scope is cancelled when dispose() is called.
     */
    protected val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ═══════════════════════════════════════════════════════════════════════
    // Position State (for POSITIONED type)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Current X position for POSITIONED overlay type.
     * Represents pixel offset from screen left edge.
     */
    protected var positionX: Float = 0f
        private set

    /**
     * Current Y position for POSITIONED overlay type.
     * Represents pixel offset from screen top edge.
     */
    protected var positionY: Float = 0f
        private set

    // ═══════════════════════════════════════════════════════════════════════
    // Visibility Control
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Show the overlay.
     *
     * If already visible, this is a no-op.
     * Calls [onShow] callback after setting visibility to true.
     */
    override fun show() {
        if (!_isVisible.value) {
            _isVisible.value = true
            onShow()
        }
    }

    /**
     * Hide the overlay.
     *
     * If already hidden, this is a no-op.
     * Calls [onHide] callback after setting visibility to false.
     */
    override fun hide() {
        if (_isVisible.value) {
            _isVisible.value = false
            onHide()
        }
    }

    /**
     * Toggle visibility.
     *
     * If hidden, shows the overlay.
     * If visible, hides the overlay.
     */
    override fun toggle() {
        if (_isVisible.value) {
            hide()
        } else {
            show()
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Position Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Update position for POSITIONED overlay type.
     *
     * Stores the new position and calls [onPositionChanged] if visible.
     * Can be called regardless of overlay type, but typically used with POSITIONED.
     *
     * @param x X coordinate in screen pixels (0 = left edge)
     * @param y Y coordinate in screen pixels (0 = top edge)
     */
    fun updatePosition(x: Float, y: Float) {
        positionX = x
        positionY = y
        if (_isVisible.value) {
            onPositionChanged(x, y)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Dispose of the overlay and release all resources.
     *
     * After calling this method:
     * - Overlay is hidden (if visible)
     * - Coroutine scope is cancelled
     * - [onDispose] callback is invoked
     *
     * The overlay should not be used after disposal.
     */
    override fun dispose() {
        hide()
        scope.cancel()
        onDispose()
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Lifecycle Callbacks
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Called when overlay becomes visible.
     *
     * Override in subclasses to:
     * - Create native views
     * - Add to window manager
     * - Start animations
     */
    protected open fun onShow() {
        // Override in subclasses
    }

    /**
     * Called when overlay becomes hidden.
     *
     * Override in subclasses to:
     * - Remove from window manager
     * - Stop animations
     * - Pause timers
     */
    protected open fun onHide() {
        // Override in subclasses
    }

    /**
     * Called when overlay is disposed.
     *
     * Override in subclasses to:
     * - Release native resources
     * - Destroy views
     * - Cancel pending operations
     */
    protected open fun onDispose() {
        // Override in subclasses
    }

    /**
     * Called when position changes while overlay is visible.
     *
     * Override in subclasses to:
     * - Update window layout parameters
     * - Animate to new position
     *
     * @param x New X coordinate
     * @param y New Y coordinate
     */
    protected open fun onPositionChanged(x: Float, y: Float) {
        // Override in subclasses
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Debug
    // ═══════════════════════════════════════════════════════════════════════

    override fun toString(): String {
        return "BaseOverlay(id='$id', type=$overlayType, visible=$isVisible, position=($positionX, $positionY))"
    }
}
