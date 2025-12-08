/**
 * DebugOverlayManager.kt - Manager for LearnApp debug overlay (REWRITTEN)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 * Rewritten: 2025-12-08 (Complete rewrite with scrollable item list)
 *
 * Manages the debug overlay lifecycle, coordinates between:
 * - ExplorationEngine (source of items and events)
 * - ExplorationItemTracker (central item storage)
 * - DebugOverlayView (UI display)
 * - WindowManager (overlay window)
 *
 * ## Usage
 * ```kotlin
 * val manager = DebugOverlayManager(context, windowManager)
 *
 * // Start tracking
 * manager.show()
 *
 * // Register items from exploration
 * manager.onScreenExplored(elements, screenHash, activityName, packageName, parentHash)
 *
 * // Mark items clicked/blocked
 * manager.markItemClicked(stableId, screenHash, navigatedTo)
 * manager.markItemBlocked(stableId, screenHash, reason)
 *
 * // Get tracker for direct access
 * val tracker = manager.getTracker()
 *
 * // Hide when done
 * manager.hide()
 * ```
 */
package com.augmentalis.voiceoscore.learnapp.debugging

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.utils.MaterialThemeHelper

/**
 * Manager for debug overlay lifecycle and state
 *
 * @property context Application/Service context
 * @property windowManager WindowManager for overlay display
 */
class DebugOverlayManager(
    private val context: Context,
    private val windowManager: WindowManager
) {
    companion object {
        private const val TAG = "DebugOverlayManager"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    // Item tracker - persists across show/hide cycles
    private val tracker = ExplorationItemTracker()

    // Overlay view - only exists when visible
    private var overlayView: DebugOverlayView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    // State
    private var isVisible = false
    private var positionX = 50
    private var positionY = 200

    /**
     * Whether debug overlay is currently visible
     */
    fun isVisible(): Boolean = isVisible

    /**
     * Get the item tracker for direct access
     */
    fun getTracker(): ExplorationItemTracker = tracker

    /**
     * Show the debug overlay
     */
    fun show() {
        if (isVisible) {
            Log.d(TAG, "Debug overlay already visible")
            return
        }

        mainHandler.post {
            try {
                Log.i(TAG, "📊 Showing debug overlay")

                val themedContext = MaterialThemeHelper.getThemedContext(context)
                overlayView = DebugOverlayView(themedContext, tracker)

                overlayView?.onMoveRequested = { dx, dy ->
                    moveOverlay(dx.toInt(), dy.toInt())
                }

                overlayView?.onCloseRequested = {
                    hide()
                }

                layoutParams = createLayoutParams()
                windowManager.addView(overlayView, layoutParams)

                isVisible = true
                Log.i(TAG, "✅ Debug overlay shown")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to show debug overlay", e)
            }
        }
    }

    /**
     * Hide the debug overlay
     */
    fun hide() {
        mainHandler.post {
            overlayView?.let { view ->
                try {
                    Log.i(TAG, "📊 Hiding debug overlay")
                    view.dispose()
                    windowManager.removeView(view)
                    overlayView = null
                    isVisible = false
                    Log.i(TAG, "✅ Debug overlay hidden")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to hide debug overlay", e)
                }
            }
        }
    }

    /**
     * Toggle overlay visibility
     */
    fun toggle() {
        if (isVisible) hide() else show()
    }

    /**
     * Move overlay by delta
     */
    private fun moveOverlay(dx: Int, dy: Int) {
        layoutParams?.let { params ->
            params.x += dx
            params.y += dy
            positionX = params.x
            positionY = params.y

            try {
                windowManager.updateViewLayout(overlayView, params)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to move overlay", e)
            }
        }
    }

    // ========== Item Tracking Methods ==========

    /**
     * Called when a screen is explored and items are discovered
     *
     * @param elements List of discovered elements on current screen
     * @param screenHash Unique hash of the current screen state
     * @param activityName Current activity name
     * @param packageName Target app package
     * @param parentScreenHash Hash of the screen we navigated from (null if root)
     */
    fun onScreenExplored(
        elements: List<ElementInfo>,
        screenHash: String,
        activityName: String,
        packageName: String,
        parentScreenHash: String?
    ) {
        // Register screen
        tracker.registerScreen(screenHash, activityName, packageName, parentScreenHash)

        // Register all items
        tracker.registerItems(elements, screenHash, activityName)

        Log.d(TAG, "📊 Screen explored: ${activityName.substringAfterLast('.')} with ${elements.size} elements")
    }

    /**
     * Mark an item as clicked
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where item was clicked
     * @param navigatedTo Screen navigated to (if navigation occurred)
     */
    fun markItemClicked(stableId: String, screenHash: String, navigatedTo: String? = null) {
        tracker.markClicked(stableId, screenHash, navigatedTo)
    }

    /**
     * Mark an item as blocked
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where item was found
     * @param reason Blocking reason (e.g., "Call button (CRITICAL)")
     */
    fun markItemBlocked(stableId: String, screenHash: String, reason: String) {
        tracker.markBlocked(stableId, screenHash, reason)
    }

    /**
     * Mark multiple items as blocked
     */
    fun markItemsBlocked(items: List<Triple<String, String, String>>) {
        items.groupBy { it.second }.forEach { (screenHash, screenItems) ->
            val blockedPairs = screenItems.map { it.first to it.third }
            tracker.markBlockedBulk(blockedPairs, screenHash)
        }
    }

    /**
     * Mark an item as currently being explored
     */
    fun markItemExploring(stableId: String, screenHash: String) {
        tracker.markExploring(stableId, screenHash)
    }

    /**
     * Record navigation from element to destination screen
     *
     * @param stableId Element stable ID that was clicked
     * @param screenHash Source screen hash
     * @param destinationScreenHash Screen that element navigated to
     */
    fun recordNavigation(stableId: String, screenHash: String, destinationScreenHash: String) {
        tracker.markClicked(stableId, screenHash, destinationScreenHash)
        Log.d(TAG, "📍 Recorded navigation: $stableId → ${destinationScreenHash.take(8)}...")
    }

    /**
     * Get exploration summary
     */
    fun getSummary(): ExplorationSummary = tracker.getSummary()

    /**
     * Clear all tracking data (for new exploration session)
     */
    fun reset() {
        tracker.clear()
        overlayView?.refreshItems()
        Log.i(TAG, "🔄 Debug overlay state reset")
    }

    /**
     * Export current state to markdown
     */
    fun exportToMarkdown(): String = tracker.exportToMarkdown()

    /**
     * Clean up all resources
     */
    fun dispose() {
        hide()
        tracker.clear()
    }

    // ========== Verbosity (kept for API compatibility) ==========

    /**
     * Cycle through display modes
     */
    fun cycleVerbosity() {
        // No longer applicable - overlay has filter buttons
        Log.d(TAG, "cycleVerbosity called (no-op, use filter buttons)")
    }

    // ========== Private Helpers ==========

    /**
     * Create window layout params for overlay
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = positionX
            y = positionY
        }
    }
}

/**
 * Verbosity enum (kept for API compatibility)
 */
enum class DebugVerbosity {
    MINIMAL,
    STANDARD,
    VERBOSE
}
