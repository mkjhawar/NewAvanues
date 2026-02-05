/**
 * VoiceCursorAPI.kt
 *
 * Created: 2025-09-26 16:20:54 IST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Public API for external modules to use VoiceCursor functionality
 * Provides access to cursor features without requiring VoiceCursor services
 * Module: VoiceCursor System
 */

package com.augmentalis.voiceos.cursor

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.cursor.core.CursorConfig
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.manager.CursorOverlayManager
import com.augmentalis.voiceos.cursor.view.CursorAction

/**
 * Public API for external modules to use VoiceCursor functionality
 * Provides access to cursor features without requiring VoiceCursor services
 */
object VoiceCursorAPI {

    private const val TAG = "VoiceCursorAPI"
    private var overlayManager: CursorOverlayManager? = null

    /**
     * Initialize cursor system with accessibility service and context
     * This must be called before using any other cursor functionality
     *
     * @param context The context to use for cursor operations
     * @param accessibilityService The accessibility service that will handle gestures
     * @return true if initialization was successful, false otherwise
     */
    fun initialize(context: Context, accessibilityService: AccessibilityService): Boolean {
        return try {
            dispose() // Cleanup any existing instance

            overlayManager = CursorOverlayManager(context).apply {
                initialize(accessibilityService)
            }

            Log.d(TAG, "VoiceCursor API initialized successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VoiceCursor API", e)
            false
        }
    }

    /**
     * Show cursor overlay with optional configuration
     *
     * @param config The cursor configuration to use. If not provided, uses saved preferences
     * @return true if cursor was shown successfully, false otherwise
     */
    fun showCursor(config: CursorConfig = CursorConfig()): Boolean {
        return overlayManager?.showCursor(config) ?: run {
            Log.w(TAG, "VoiceCursor API not initialized - call initialize() first")
            false
        }
    }

    /**
     * Hide cursor overlay
     *
     * @return true if cursor was hidden successfully, false otherwise
     */
    fun hideCursor(): Boolean {
        return overlayManager?.hideCursor() ?: run {
            Log.w(TAG, "VoiceCursor API not initialized - call initialize() first")
            false
        }
    }

    /**
     * Center cursor on screen
     *
     * @return true if cursor was centered successfully, false otherwise
     */
    fun centerCursor(): Boolean {
        return overlayManager?.centerCursor() ?: run {
            Log.w(TAG, "VoiceCursor API not initialized - call initialize() first")
            false
        }
    }

    /**
     * Move cursor to specified position
     *
     * @param position The target position to move the cursor to
     * @param animate Whether to animate the movement (default: true)
     * @return true if cursor was moved successfully, false otherwise
     */
    fun moveTo(position: CursorOffset, animate: Boolean = true): Boolean {
        return overlayManager?.moveTo(position, animate) ?: run {
            Log.w(TAG, "VoiceCursor API not initialized - call initialize() first")
            false
        }
    }

    /**
     * Execute cursor action at specified position
     *
     * @param action The cursor action to execute
     * @param position The position to execute the action at. If null, uses current cursor position
     * @return true if action was executed successfully, false otherwise
     */
    fun executeAction(action: CursorAction, position: CursorOffset? = null): Boolean {
        return overlayManager?.executeAction(action, position) ?: run {
            Log.w(TAG, "VoiceCursor API not initialized - call initialize() first")
            false
        }
    }

    /**
     * Update cursor configuration
     *
     * @param config The new cursor configuration to apply
     * @return true if configuration was updated successfully, false otherwise
     */
    fun updateConfiguration(config: CursorConfig): Boolean {
        return overlayManager?.updateConfiguration(config) ?: run {
            Log.w(TAG, "VoiceCursor API not initialized - call initialize() first")
            false
        }
    }

    /**
     * Get current cursor position
     *
     * @return The current cursor position, or null if not initialized or cursor not visible
     */
    fun getCurrentPosition(): CursorOffset? {
        return overlayManager?.getCurrentPosition() ?: run {
            Log.w(TAG, "VoiceCursor API not initialized - call initialize() first")
            null
        }
    }

    /**
     * Check if cursor is currently visible
     *
     * @return true if cursor is visible, false otherwise
     */
    fun isVisible(): Boolean {
        return overlayManager?.isVisible() ?: false
    }

    /**
     * Check if API is initialized and ready to use
     *
     * @return true if initialized, false otherwise
     */
    fun isInitialized(): Boolean {
        return overlayManager != null
    }

    /**
     * Dispose cursor system and cleanup resources
     * Call this when the accessibility service is being destroyed
     */
    fun dispose() {
        overlayManager?.dispose()
        overlayManager = null
        Log.d(TAG, "VoiceCursor API disposed")
    }

    // Convenience methods for common cursor actions

    /**
     * Perform a single click at current cursor position
     *
     * @return true if click was performed successfully, false otherwise
     */
    fun click(): Boolean {
        return executeAction(CursorAction.SINGLE_CLICK)
    }

    /**
     * Perform a double click at current cursor position
     *
     * @return true if double click was performed successfully, false otherwise
     */
    fun doubleClick(): Boolean {
        return executeAction(CursorAction.DOUBLE_CLICK)
    }

    /**
     * Perform a long press at current cursor position
     *
     * @return true if long press was performed successfully, false otherwise
     */
    fun longPress(): Boolean {
        return executeAction(CursorAction.LONG_PRESS)
    }

    /**
     * Perform scroll up at current cursor position
     *
     * @return true if scroll was performed successfully, false otherwise
     */
    fun scrollUp(): Boolean {
        return executeAction(CursorAction.SCROLL_UP)
    }

    /**
     * Perform scroll down at current cursor position
     *
     * @return true if scroll was performed successfully, false otherwise
     */
    fun scrollDown(): Boolean {
        return executeAction(CursorAction.SCROLL_DOWN)
    }

    /**
     * Start drag operation at current cursor position
     *
     * @return true if drag start was performed successfully, false otherwise
     */
    fun startDrag(): Boolean {
        return executeAction(CursorAction.DRAG_START)
    }

    /**
     * End drag operation at current cursor position
     *
     * @return true if drag end was performed successfully, false otherwise
     */
    fun endDrag(): Boolean {
        return executeAction(CursorAction.DRAG_END)
    }

    /**
     * Toggle cursor visibility
     *
     * @return true if toggle was successful, false otherwise
     */
    fun toggleCursor(): Boolean {
        return if (isVisible()) {
            hideCursor()
        } else {
            showCursor()
        }
    }
}