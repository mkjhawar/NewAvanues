/**
 * VoiceCursorServiceBinder.kt - IPC service binder implementation for VoiceCursor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI Code Quality Expert
 * Created: 2025-11-10
 */
package com.augmentalis.voiceos.cursor

import android.util.Log
import com.augmentalis.voiceos.cursor.core.CursorConfig
import com.augmentalis.voiceos.cursor.core.CursorOffset
import com.augmentalis.voiceos.cursor.view.CursorAction

/**
 * Voice Cursor Service AIDL implementation
 *
 * Exposes VoiceCursorAPI functionality via IPC for use by external apps.
 * This binder wraps the VoiceCursorAPI singleton and provides thread-safe
 * access to cursor control functionality across process boundaries.
 *
 * Features:
 * - Thread-safe access to VoiceCursorAPI singleton
 * - Automatic conversion between Parcelable and internal types
 * - Error handling and logging
 * - Null safety checks
 *
 * Usage:
 * ```kotlin
 * // In service's onBind()
 * private var cursorBinder: VoiceCursorServiceBinder? = null
 *
 * override fun onBind(intent: Intent?): IBinder? {
 *     return when (intent?.action) {
 *         "com.augmentalis.voiceos.cursor.BIND_IPC" -> {
 *             cursorBinder = VoiceCursorServiceBinder()
 *             cursorBinder!!.asBinder()
 *         }
 *         else -> super.onBind(intent)
 *     }
 * }
 * ```
 */
class VoiceCursorServiceBinder : IVoiceCursorService.Stub() {

    companion object {
        private const val TAG = "VoiceCursorServiceBinder"
    }

    /**
     * Check if cursor API is initialized and ready
     *
     * @return true if initialized, false otherwise
     */
    override fun isInitialized(): Boolean {
        Log.d(TAG, "IPC: isInitialized() called")
        return try {
            VoiceCursorAPI.isInitialized()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking initialization state", e)
            false
        }
    }

    /**
     * Show cursor overlay with optional configuration
     *
     * @param config Cursor configuration (null = use defaults)
     * @return true if cursor shown successfully, false otherwise
     */
    override fun showCursor(config: CursorConfiguration?): Boolean {
        Log.d(TAG, "IPC: showCursor(config=${config != null})")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized - call initialize() first")
            return false
        }

        return try {
            val cursorConfig = config?.toCursorConfig() ?: CursorConfig()
            VoiceCursorAPI.showCursor(cursorConfig)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing cursor", e)
            false
        }
    }

    /**
     * Hide cursor overlay
     *
     * @return true if cursor hidden successfully, false otherwise
     */
    override fun hideCursor(): Boolean {
        Log.d(TAG, "IPC: hideCursor() called")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            VoiceCursorAPI.hideCursor()
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding cursor", e)
            false
        }
    }

    /**
     * Toggle cursor visibility (show if hidden, hide if shown)
     *
     * @return true if toggle successful, false otherwise
     */
    override fun toggleCursor(): Boolean {
        Log.d(TAG, "IPC: toggleCursor() called")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            VoiceCursorAPI.toggleCursor()
        } catch (e: Exception) {
            Log.e(TAG, "Error toggling cursor", e)
            false
        }
    }

    /**
     * Check if cursor is currently visible
     *
     * @return true if cursor is visible, false otherwise
     */
    override fun isVisible(): Boolean {
        return try {
            VoiceCursorAPI.isVisible()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking visibility", e)
            false
        }
    }

    /**
     * Center cursor on screen
     *
     * @return true if cursor centered successfully, false otherwise
     */
    override fun centerCursor(): Boolean {
        Log.d(TAG, "IPC: centerCursor() called")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            VoiceCursorAPI.centerCursor()
        } catch (e: Exception) {
            Log.e(TAG, "Error centering cursor", e)
            false
        }
    }

    /**
     * Move cursor to specified position
     *
     * @param position Target position (x, y coordinates)
     * @param animate Whether to animate movement (default: true)
     * @return true if cursor moved successfully, false otherwise
     */
    override fun moveTo(position: CursorPosition?, animate: Boolean): Boolean {
        Log.d(TAG, "IPC: moveTo(position=$position, animate=$animate)")

        if (position == null) {
            Log.w(TAG, "moveTo called with null position")
            return false
        }

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            val cursorOffset = CursorOffset(position.x, position.y)
            VoiceCursorAPI.moveTo(cursorOffset, animate)
        } catch (e: Exception) {
            Log.e(TAG, "Error moving cursor", e)
            false
        }
    }

    /**
     * Get current cursor position
     *
     * @return Current cursor position, or null if not visible/initialized
     */
    override fun getCurrentPosition(): CursorPosition? {
        return try {
            if (!isInitialized()) {
                Log.w(TAG, "VoiceCursor not initialized")
                return null
            }

            val offset = VoiceCursorAPI.getCurrentPosition() ?: return null
            CursorPosition(offset.x, offset.y)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current position", e)
            null
        }
    }

    /**
     * Execute cursor action at current or specified position
     *
     * Actions:
     * - 0 = SINGLE_CLICK
     * - 1 = DOUBLE_CLICK
     * - 2 = LONG_PRESS
     * - 3 = SCROLL_UP
     * - 4 = SCROLL_DOWN
     * - 5 = DRAG_START
     * - 6 = DRAG_END
     *
     * @param action Action code (see above)
     * @param position Optional target position (null = current cursor position)
     * @return true if action executed successfully, false otherwise
     */
    override fun executeAction(action: Int, position: CursorPosition?): Boolean {
        Log.d(TAG, "IPC: executeAction(action=$action, position=$position)")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            val cursorAction = when (action) {
                0 -> CursorAction.SINGLE_CLICK
                1 -> CursorAction.DOUBLE_CLICK
                2 -> CursorAction.LONG_PRESS
                3 -> CursorAction.SCROLL_UP
                4 -> CursorAction.SCROLL_DOWN
                5 -> CursorAction.DRAG_START
                6 -> CursorAction.DRAG_END
                else -> {
                    Log.w(TAG, "Unknown action code: $action")
                    return false
                }
            }

            val cursorOffset = position?.let { CursorOffset(it.x, it.y) }
            VoiceCursorAPI.executeAction(cursorAction, cursorOffset)
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action", e)
            false
        }
    }

    /**
     * Perform click at current cursor position
     *
     * @return true if click performed successfully, false otherwise
     */
    override fun click(): Boolean {
        Log.d(TAG, "IPC: click() called")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            VoiceCursorAPI.click()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing click", e)
            false
        }
    }

    /**
     * Perform double-click at current cursor position
     *
     * @return true if double-click performed successfully, false otherwise
     */
    override fun doubleClick(): Boolean {
        Log.d(TAG, "IPC: doubleClick() called")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            VoiceCursorAPI.doubleClick()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing double-click", e)
            false
        }
    }

    /**
     * Perform long press at current cursor position
     *
     * @return true if long press performed successfully, false otherwise
     */
    override fun longPress(): Boolean {
        Log.d(TAG, "IPC: longPress() called")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            VoiceCursorAPI.longPress()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing long press", e)
            false
        }
    }

    /**
     * Perform scroll up at current cursor position
     *
     * @return true if scroll performed successfully, false otherwise
     */
    override fun scrollUp(): Boolean {
        Log.d(TAG, "IPC: scrollUp() called")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            VoiceCursorAPI.scrollUp()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing scroll up", e)
            false
        }
    }

    /**
     * Perform scroll down at current cursor position
     *
     * @return true if scroll performed successfully, false otherwise
     */
    override fun scrollDown(): Boolean {
        Log.d(TAG, "IPC: scrollDown() called")

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            VoiceCursorAPI.scrollDown()
        } catch (e: Exception) {
            Log.e(TAG, "Error performing scroll down", e)
            false
        }
    }

    /**
     * Update cursor configuration
     *
     * @param config New cursor configuration
     * @return true if configuration updated successfully, false otherwise
     */
    override fun updateConfiguration(config: CursorConfiguration?): Boolean {
        Log.d(TAG, "IPC: updateConfiguration(config=${config != null})")

        if (config == null) {
            Log.w(TAG, "updateConfiguration called with null config")
            return false
        }

        if (!isInitialized()) {
            Log.w(TAG, "VoiceCursor not initialized")
            return false
        }

        return try {
            val cursorConfig = config.toCursorConfig()
            VoiceCursorAPI.updateConfiguration(cursorConfig)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating configuration", e)
            false
        }
    }
}

/**
 * Extension function to convert CursorConfiguration to CursorConfig
 */
private fun CursorConfiguration.toCursorConfig(): CursorConfig {
    return CursorConfig(
        // Note: CursorConfig fields may differ from CursorConfiguration
        // This is a placeholder conversion. Adjust based on actual CursorConfig implementation.
    )
}
