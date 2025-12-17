/**
 * WidgetOverlayHelper.kt - Centralized WindowManager overlay operations
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/widgets/WidgetOverlayHelper.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-24
 *
 * Utility class for managing WindowManager overlay operations with thread safety.
 * Addresses ViewTreeLifecycleOwner issues by using direct WindowManager APIs.
 */

package com.augmentalis.learnapp.ui.widgets

import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

/**
 * Widget Overlay Helper
 *
 * Centralized utility for WindowManager overlay operations with thread safety.
 *
 * ## Purpose
 *
 * Provides thread-safe WindowManager operations for displaying overlays
 * in AccessibilityService context without Compose lifecycle dependencies.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val helper = WidgetOverlayHelper(context, windowManager)
 * val params = helper.createOverlayParams()
 *
 * // Show overlay (automatically on main thread)
 * helper.showOverlay(view, params)
 *
 * // Dismiss overlay (automatically on main thread)
 * helper.dismissOverlay(view)
 * ```
 *
 * ## Thread Safety
 *
 * All public methods ensure execution on main thread via Handler.post().
 * Safe to call from any thread.
 *
 * @property windowManager WindowManager instance
 *
 * @since 1.0.0
 */
class WidgetOverlayHelper(
    private val windowManager: WindowManager
) {

    /**
     * Main thread handler for UI operations
     */
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Show overlay
     *
     * Adds view to WindowManager on main thread. Checks if view is already
     * attached to prevent IllegalStateException.
     *
     * @param view View to display as overlay
     * @param params WindowManager layout parameters
     */
    fun showOverlay(view: View, params: WindowManager.LayoutParams) {
        ensureMainThread {
            // Only add if not already attached
            if (view.parent == null) {
                try {
                    windowManager.addView(view, params)
                } catch (e: Exception) {
                    // Log error but don't crash
                    android.util.Log.e(
                        "WidgetOverlayHelper",
                        "Failed to add overlay view",
                        e
                    )
                }
            }
        }
    }

    /**
     * Dismiss overlay
     *
     * Removes view from WindowManager on main thread. Checks if view is
     * attached to prevent IllegalArgumentException.
     *
     * @param view View to remove from overlay
     */
    fun dismissOverlay(view: View) {
        ensureMainThread {
            // Only remove if currently attached
            if (view.parent != null) {
                try {
                    windowManager.removeView(view)
                } catch (e: Exception) {
                    // Log error but don't crash
                    android.util.Log.e(
                        "WidgetOverlayHelper",
                        "Failed to remove overlay view",
                        e
                    )
                }
            }
        }
    }

    /**
     * Create overlay parameters
     *
     * Creates WindowManager.LayoutParams configured for accessibility overlay.
     * Uses TYPE_ACCESSIBILITY_OVERLAY for AccessibilityService context.
     *
     * @param type Window type (default: TYPE_ACCESSIBILITY_OVERLAY)
     * @return Configured layout parameters
     */
    fun createOverlayParams(
        type: Int = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
    }

    /**
     * Ensure main thread
     *
     * Executes block on main thread. If already on main thread, executes
     * immediately. Otherwise, posts to main thread handler.
     *
     * @param block Code to execute on main thread
     */
    fun ensureMainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread, execute immediately
            block()
        } else {
            // Post to main thread
            mainHandler.post(block)
        }
    }
}
