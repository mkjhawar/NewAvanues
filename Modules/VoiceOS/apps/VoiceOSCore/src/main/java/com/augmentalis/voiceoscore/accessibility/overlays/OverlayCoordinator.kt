/**
 * OverlayCoordinator.kt - Overlay display management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-17
 *
 * Responsibility: Manages overlay show/hide/update operations and coordinates between multiple overlay types
 */
package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.Context
import android.graphics.Rect
import android.util.Log
import android.view.WindowManager

/**
 * Coordinates overlay display across different overlay types
 *
 * Manages:
 * - NumberedSelectionOverlay (numbered element selection)
 * - ContextMenuOverlay (context menus)
 * - CommandStatusOverlay (command status)
 * - ConfidenceOverlay (confidence indicators)
 */
class OverlayCoordinator(private val context: Context) {
    companion object {
        private const val TAG = "OverlayCoordinator"
    }

    private val windowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    private val numberedOverlay by lazy {
        NumberedSelectionOverlay(context, windowManager).also {
            Log.d(TAG, "NumberedSelectionOverlay initialized (lazy)")
        }
    }

    private val contextMenuOverlay by lazy {
        ContextMenuOverlay(context, windowManager).also {
            Log.d(TAG, "ContextMenuOverlay initialized (lazy)")
        }
    }

    private val commandStatusOverlay by lazy {
        CommandStatusOverlay(context, windowManager).also {
            Log.d(TAG, "CommandStatusOverlay initialized (lazy)")
        }
    }

    private val confidenceOverlay by lazy {
        ConfidenceOverlay(context, windowManager).also {
            Log.d(TAG, "ConfidenceOverlay initialized (lazy)")
        }
    }

    /**
     * Show numbered overlay for element selection
     */
    fun showNumberedOverlay(elements: List<SelectableItem>) {
        try {
            Log.d(TAG, "Showing numbered overlay with ${elements.size} elements")
            numberedOverlay.showItems(elements)
            Log.i(TAG, "Numbered overlay shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing numbered overlay", e)
        }
    }

    /**
     * Hide numbered overlay
     */
    fun hideNumberedOverlay() {
        try {
            Log.d(TAG, "Hiding numbered overlay")
            numberedOverlay.hide()
            Log.i(TAG, "Numbered overlay hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding numbered overlay", e)
        }
    }

    /**
     * Show context menu overlay
     */
    fun showContextMenu(x: Float, y: Float, options: List<String>) {
        try {
            Log.d(TAG, "Showing context menu at ($x, $y) with ${options.size} options")
            contextMenuOverlay.show(options, Rect(x.toInt(), y.toInt(), x.toInt(), y.toInt()))
            Log.i(TAG, "Context menu shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing context menu", e)
        }
    }

    /**
     * Hide context menu overlay
     */
    fun hideContextMenu() {
        try {
            Log.d(TAG, "Hiding context menu")
            contextMenuOverlay.hide()
            Log.i(TAG, "Context menu hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding context menu", e)
        }
    }

    /**
     * Show command status overlay
     */
    fun showCommandStatus(message: String, isSuccess: Boolean = true) {
        try {
            Log.d(TAG, "Showing command status: $message (success: $isSuccess)")
            commandStatusOverlay.show(message, isSuccess)
            Log.i(TAG, "Command status shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing command status", e)
        }
    }

    /**
     * Hide command status overlay
     */
    fun hideCommandStatus() {
        try {
            Log.d(TAG, "Hiding command status")
            commandStatusOverlay.hide()
            Log.i(TAG, "Command status hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding command status", e)
        }
    }

    /**
     * Show confidence overlay
     */
    fun showConfidenceOverlay(confidence: Float, bounds: Rect) {
        try {
            Log.d(TAG, "Showing confidence overlay: $confidence at $bounds")
            confidenceOverlay.show(confidence, bounds)
            Log.i(TAG, "Confidence overlay shown successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error showing confidence overlay", e)
        }
    }

    /**
     * Hide confidence overlay
     */
    fun hideConfidenceOverlay() {
        try {
            Log.d(TAG, "Hiding confidence overlay")
            confidenceOverlay.hide()
            Log.i(TAG, "Confidence overlay hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding confidence overlay", e)
        }
    }

    /**
     * Update overlay state
     */
    fun updateOverlayState(state: OverlayState) {
        try {
            Log.d(TAG, "Updating overlay state: $state")
            // Update all overlays based on state
            when (state) {
                OverlayState.HIDDEN -> hideAllOverlays()
                OverlayState.NUMBERED -> {} // Already shown via showNumberedOverlay
                OverlayState.CONTEXT_MENU -> {} // Already shown via showContextMenu
                OverlayState.COMMAND_STATUS -> {} // Already shown via showCommandStatus
                OverlayState.CONFIDENCE -> {} // Already shown via showConfidenceOverlay
            }
            Log.i(TAG, "Overlay state updated successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay state", e)
        }
    }

    /**
     * Hide all overlays
     */
    fun hideAllOverlays() {
        try {
            Log.d(TAG, "Hiding all overlays")
            hideNumberedOverlay()
            hideContextMenu()
            hideCommandStatus()
            hideConfidenceOverlay()
            Log.i(TAG, "All overlays hidden successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding all overlays", e)
        }
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        try {
            Log.d(TAG, "Cleaning up OverlayCoordinator...")
            // Dispose all initialized overlays
            if (::numberedOverlay.isInitialized) numberedOverlay.dispose()
            if (::contextMenuOverlay.isInitialized) contextMenuOverlay.dispose()
            if (::commandStatusOverlay.isInitialized) commandStatusOverlay.dispose()
            if (::confidenceOverlay.isInitialized) confidenceOverlay.dispose()
            Log.i(TAG, "OverlayCoordinator cleaned up successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up OverlayCoordinator", e)
        }
    }

    /**
     * Overlay state enum
     */
    enum class OverlayState {
        HIDDEN,
        NUMBERED,
        CONTEXT_MENU,
        COMMAND_STATUS,
        CONFIDENCE
    }
}
