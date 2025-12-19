/**
 * ProgressOverlayManager.kt - Manages progress overlay lifecycle
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/ui/ProgressOverlayManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Updated: 2025-10-24 (Migrated from Compose to widgets)
 *
 * Manages progress overlay showing exploration progress.
 * Migrated from Compose to widget-based implementation.
 */

package com.augmentalis.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.WindowManager
import com.augmentalis.learnapp.ui.widgets.ProgressOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Progress Overlay Manager
 *
 * Manages lifecycle of progress overlay showing real-time operation status.
 *
 * ## Migration from Compose
 *
 * Previous version used Jetpack Compose with ComposeView and lifecycle owners.
 * This caused ViewTreeLifecycleOwner exceptions in AccessibilityService context.
 * Now uses widget-based ProgressOverlay for reliable display.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val manager = ProgressOverlayManager(context)
 *
 * // Show overlay
 * manager.showProgressOverlay("Learning Instagram...")
 *
 * // Update message
 * manager.updateMessage("Mapping elements...")
 *
 * // Hide overlay
 * manager.hideProgressOverlay()
 * ```
 *
 * ## Thread Safety
 *
 * All methods are thread-safe and can be called from any thread.
 * UI operations are automatically executed on main thread.
 *
 * @property context Application or Service context
 *
 * @since 1.0.0
 */
class ProgressOverlayManager(
    private val context: AccessibilityService
) {

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Window manager
     */
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Current progress overlay (widget-based)
     */
    private val progressOverlay = ProgressOverlay(context)

    /**
     * Overlay visible state
     */
    private var isOverlayVisible = false

    /**
     * Show progress overlay
     *
     * Displays progress overlay with message. If already showing, updates
     * the message instead.
     *
     * @param message Message to display (e.g., "Learning Instagram...")
     */
    fun showProgressOverlay(message: String = "Loading...") {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                if (!isOverlayVisible) {
                    progressOverlay.show(windowManager, message)
                    isOverlayVisible = true
                } else {
                    // Already showing, just update message
                    progressOverlay.updateMessage(message)
                }
            }
        }

    }

    /**
     * Update message
     *
     * Updates the progress message while overlay is showing.
     * Has no effect if overlay is not visible.
     *
     * @param message New message text
     */
    fun updateMessage(message: String) {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                if (isOverlayVisible) {
                    progressOverlay.updateMessage(message)
                }
            }
        }

    }

    /**
     * Hide progress overlay
     *
     * Dismisses the progress overlay. Safe to call even if not showing.
     */
    fun hideProgressOverlay() {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                if (isOverlayVisible) {
                    progressOverlay.dismiss(windowManager)
                    isOverlayVisible = false
                }
            }
        }
    }

    /**
     * Check if overlay is visible
     *
     * @return true if overlay is currently showing
     */
    fun isOverlayShowing(): Boolean {
        return isOverlayVisible
    }

    /**
     * Cleanup
     *
     * Releases resources and cancels all coroutines.
     * Call when manager is no longer needed.
     */
    fun cleanup() {
        // First perform cleanup on main thread, then cancel scope
        mainScope.launch {
            withContext(Dispatchers.Main) {
                hideProgressOverlay()
                progressOverlay.cleanup()
            }
        }.invokeOnCompletion {
            mainScope.cancel()
        }
    }
}
