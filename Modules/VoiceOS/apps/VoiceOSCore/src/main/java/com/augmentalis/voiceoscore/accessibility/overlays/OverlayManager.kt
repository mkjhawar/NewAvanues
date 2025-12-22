/**
 * OverlayManager.kt - Singleton manager for overlay coordination
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v12.1)
 * Created: 2025-12-22
 *
 * P2-8c: Completes overlay management extraction with singleton pattern.
 * Provides centralized access point for all overlay operations in VoiceOSService.
 */

package com.augmentalis.voiceoscore.accessibility.overlays

import android.content.Context
import android.graphics.Rect
import android.util.Log

/**
 * Overlay Manager (Singleton)
 *
 * Centralized manager for all overlay operations:
 * - Numbered element selection overlays
 * - Context menu overlays
 * - Command status overlays
 * - Confidence indicator overlays
 *
 * Provides singleton access to OverlayCoordinator functionality.
 *
 * @see OverlayCoordinator
 */
class OverlayManager private constructor(context: Context) {

    companion object {
        private const val TAG = "OverlayManager"

        @Volatile
        private var instance: OverlayManager? = null

        /**
         * Get singleton instance
         *
         * Thread-safe double-checked locking
         *
         * @param context Application or service context
         * @return Singleton OverlayManager instance
         */
        fun getInstance(context: Context): OverlayManager {
            return instance ?: synchronized(this) {
                instance ?: OverlayManager(context.applicationContext).also {
                    instance = it
                    Log.d(TAG, "OverlayManager singleton created")
                }
            }
        }
    }

    private val coordinator = OverlayCoordinator(context.applicationContext)

    // ============================================================
    // Numbered Selection Overlay
    // ============================================================

    /**
     * Show numbered overlay for element selection
     *
     * @param elements List of selectable items to display with numbers
     */
    fun showNumberedOverlay(elements: List<SelectableItem>) {
        coordinator.showNumberedOverlay(elements)
    }

    /**
     * Hide numbered overlay
     */
    fun hideNumberedOverlay() {
        coordinator.hideNumberedOverlay()
    }

    // ============================================================
    // Context Menu Overlay
    // ============================================================

    /**
     * Show context menu overlay at specified position
     *
     * @param x X-coordinate for menu position
     * @param y Y-coordinate for menu position
     * @param options List of menu option strings
     */
    fun showContextMenu(x: Float, y: Float, options: List<String>) {
        coordinator.showContextMenu(x, y, options)
    }

    /**
     * Hide context menu overlay
     */
    fun hideContextMenu() {
        coordinator.hideContextMenu()
    }

    // ============================================================
    // Command Status Overlay
    // ============================================================

    /**
     * Show command status overlay with message
     *
     * @param message Status message to display
     * @param isSuccess Whether command succeeded (affects styling)
     */
    fun showCommandStatus(message: String, isSuccess: Boolean = true) {
        coordinator.showCommandStatus(message, isSuccess)
    }

    /**
     * Hide command status overlay
     */
    fun hideCommandStatus() {
        coordinator.hideCommandStatus()
    }

    // ============================================================
    // Confidence Overlay
    // ============================================================

    /**
     * Show confidence indicator overlay
     *
     * @param confidence Confidence level (0.0 to 1.0)
     * @param bounds Screen bounds for overlay placement
     */
    fun showConfidenceOverlay(confidence: Float, bounds: Rect) {
        coordinator.showConfidenceOverlay(confidence, bounds)
    }

    /**
     * Hide confidence overlay
     */
    fun hideConfidenceOverlay() {
        coordinator.hideConfidenceOverlay()
    }

    // ============================================================
    // Overlay State Management
    // ============================================================

    /**
     * Update overlay state
     *
     * @param state Target overlay state
     */
    fun updateOverlayState(state: OverlayCoordinator.OverlayState) {
        coordinator.updateOverlayState(state)
    }

    /**
     * Hide all overlays
     */
    fun hideAllOverlays() {
        coordinator.hideAllOverlays()
    }

    // ============================================================
    // Lifecycle Management
    // ============================================================

    /**
     * Dispose and cleanup all overlay resources
     *
     * Call from VoiceOSService.onDestroy()
     */
    fun dispose() {
        try {
            Log.d(TAG, "Disposing OverlayManager...")
            coordinator.cleanup()
            Log.i(TAG, "OverlayManager disposed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing OverlayManager", e)
        }
    }
}
