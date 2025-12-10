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

package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.graphics.PixelFormat
import com.augmentalis.voiceoscore.R
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine
import com.augmentalis.voiceoscore.learnapp.ui.widgets.ProgressOverlay
import com.augmentalis.voiceoscore.utils.MaterialThemeHelper
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Progress Overlay Manager
 *
 * Manages lifecycle of bottom command bar showing exploration progress and controls.
 *
 * ## Phase 3 Refactor: Command Bar Implementation
 *
 * Previous version used full-screen overlay blocking user interaction.
 * Now uses bottom command bar (48dp height) that allows interaction with underlying app.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val manager = ProgressOverlayManager(context, explorationEngine)
 *
 * // Show command bar
 * manager.showCommandBar("com.instagram.android", 45)
 *
 * // Update progress
 * manager.updateProgress(75, "Learning... 75%")
 *
 * // Update pause state
 * manager.updatePauseState(true)
 *
 * // Dismiss (with animation)
 * manager.dismissCommandBar()
 *
 * // Show again
 * manager.showCommandBar()
 * ```
 *
 * ## Thread Safety
 *
 * All methods are thread-safe and can be called from any thread.
 * UI operations are automatically executed on main thread.
 *
 * @property context Application or Service context
 * @property explorationEngine Exploration engine for pause/resume control
 *
 * @since 1.0.0 (Phase 3: Command bar refactor)
 */
class ProgressOverlayManager(
    private val context: AccessibilityService,
    internal var explorationEngine: ExplorationEngine? = null
) {

    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Window manager
     */
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Command bar view (Phase 3: Replaces full-screen overlay)
     */
    private var commandBarView: View? = null

    /**
     * Command bar UI elements
     */
    private var progressIndicator: CircularProgressIndicator? = null
    private var statusText: MaterialTextView? = null
    private var pauseButton: MaterialButton? = null
    private var closeButton: MaterialButton? = null

    /**
     * Current package name being learned
     */
    private var currentPackageName: String? = null

    /**
     * Current progress percentage (0-100)
     */
    private var currentProgress: Int = 0

    /**
     * Paused state
     */
    private var isPaused: Boolean = false

    /**
     * Command bar visible state
     */
    private var isCommandBarVisible = false

    /**
     * Command bar height in pixels (48dp)
     */
    private val commandBarHeight: Int by lazy {
        (48 * context.resources.displayMetrics.density).toInt()
    }

    /**
     * Legacy overlay for backward compatibility (will be removed)
     */
    @Deprecated("Use command bar instead", ReplaceWith("showCommandBar()"))
    private var progressOverlay: ProgressOverlay? = null

    /**
     * Overlay visible state (legacy)
     */
    @Deprecated("Use isCommandBarVisible instead")
    private var isOverlayVisible = false

    // ========== Phase 3: Command Bar Methods ==========

    /**
     * Create command bar view
     *
     * Phase 3: Creates bottom command bar from layout with proper window parameters.
     *
     * @return Configured command bar view
     */
    private fun createCommandBar(): View {
        // Use MaterialThemeHelper to inflate Material3 views in AccessibilityService context
        val view = MaterialThemeHelper.inflateOverlay(context, R.layout.command_bar_layout)

        // Get references to UI elements
        progressIndicator = view.findViewById(R.id.progress_indicator)
        statusText = view.findViewById(R.id.status_text)
        pauseButton = view.findViewById(R.id.pause_button)
        closeButton = view.findViewById(R.id.close_button)

        // Set up button click handlers
        pauseButton?.setOnClickListener {
            handlePauseButtonClick()
        }

        closeButton?.setOnClickListener {
            handleCloseButtonClick()
        }

        // Set up swipe-to-dismiss gesture
        view.setOnTouchListener(SwipeToDismissListener())

        return view
    }

    /**
     * Show command bar with package name and progress
     *
     * Phase 3: Shows bottom command bar at bottom of screen.
     *
     * @param packageName Package name being learned
     * @param progress Progress percentage (0-100)
     */
    fun showCommandBar(packageName: String, progress: Int) {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                try {
                    // Store current state
                    currentPackageName = packageName
                    currentProgress = progress

                    // Create view if needed
                    if (commandBarView == null) {
                        commandBarView = createCommandBar()
                    }

                    // Update UI
                    updateProgress(progress, "Learning $packageName... $progress%")

                    // Add to window manager if not already showing
                    if (!isCommandBarVisible) {
                        val params = WindowManager.LayoutParams(
                            WindowManager.LayoutParams.MATCH_PARENT,
                            WindowManager.LayoutParams.WRAP_CONTENT,  // CRITICAL: Must be WRAP_CONTENT!
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                            PixelFormat.TRANSLUCENT
                        )
                        params.gravity = Gravity.BOTTOM

                        windowManager.addView(commandBarView, params)
                        isCommandBarVisible = true

                        // Animate in
                        animateIn()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProgressOverlayManager", "Failed to show command bar", e)
                }
            }
        }
    }

    /**
     * Show command bar (re-show after dismiss)
     *
     * Phase 3: Re-shows previously dismissed command bar.
     */
    fun showCommandBar() {
        currentPackageName?.let { packageName ->
            showCommandBar(packageName, currentProgress)
        }
    }

    /**
     * Update progress indicator and message
     *
     * Phase 3: Updates progress percentage and status text.
     *
     * @param progress Progress percentage (0-100)
     * @param message Status message
     */
    fun updateProgress(progress: Int, message: String) {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                try {
                    currentProgress = progress
                    progressIndicator?.setProgressCompat(progress, true)
                    statusText?.text = message
                } catch (e: Exception) {
                    android.util.Log.e("ProgressOverlayManager", "Failed to update progress", e)
                }
            }
        }
    }

    /**
     * Update pause state
     *
     * Phase 3: Updates pause button icon and state.
     *
     * @param isPaused True if paused, false if running
     */
    fun updatePauseState(isPaused: Boolean) {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                try {
                    this@ProgressOverlayManager.isPaused = isPaused
                    pauseButton?.setIconResource(
                        if (isPaused) R.drawable.ic_play else R.drawable.ic_pause
                    )
                    pauseButton?.contentDescription = if (isPaused) "Resume learning" else "Pause learning"
                } catch (e: Exception) {
                    android.util.Log.e("ProgressOverlayManager", "Failed to update pause state", e)
                }
            }
        }
    }

    /**
     * Check if command bar is showing
     *
     * Phase 3: Returns current visibility state of command bar.
     *
     * @return True if command bar is currently visible
     */
    fun isCommandBarShowing(): Boolean {
        return isCommandBarVisible
    }

    /**
     * Dismiss command bar with animation
     *
     * Phase 3: Slides command bar down and removes from window.
     */
    fun dismissCommandBar() {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                try {
                    if (isCommandBarVisible) {
                        animateOut {
                            windowManager.removeView(commandBarView)
                            isCommandBarVisible = false

                            // TODO: Show background notification when implemented
                            // notificationManager.showBackgroundNotification(currentPackageName, currentProgress)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ProgressOverlayManager", "Failed to dismiss command bar", e)
                }
            }
        }
    }

    /**
     * Handle pause button click
     *
     * Phase 3: Toggles pause/resume state via ExplorationEngine.
     */
    private fun handlePauseButtonClick() {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                try {
                    if (isPaused) {
                        explorationEngine?.resumeExploration()
                    } else {
                        explorationEngine?.pauseExploration()
                    }
                    // updatePauseState will be called by state flow listener
                } catch (e: Exception) {
                    android.util.Log.e("ProgressOverlayManager", "Failed to toggle pause", e)
                }
            }
        }
    }

    /**
     * Handle close button click
     *
     * Phase 3: Dismisses command bar (shows background notification).
     */
    private fun handleCloseButtonClick() {
        dismissCommandBar()
    }

    /**
     * Animate command bar in (slide up)
     *
     * Phase 3: Slides command bar up from bottom.
     */
    private fun animateIn() {
        commandBarView?.let { view ->
            view.translationY = commandBarHeight.toFloat()
            view.animate()
                .translationY(0f)
                .setDuration(200)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    /**
     * Animate command bar out (slide down)
     *
     * Phase 3: Slides command bar down to bottom.
     *
     * @param onComplete Callback when animation completes
     */
    private fun animateOut(onComplete: () -> Unit) {
        commandBarView?.let { view ->
            view.animate()
                .translationY(commandBarHeight.toFloat())
                .setDuration(200)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction(onComplete)
                .start()
        } ?: onComplete()
    }

    /**
     * Swipe-to-dismiss gesture listener
     *
     * Phase 3: Detects swipe down gesture to dismiss command bar.
     */
    private inner class SwipeToDismissListener : View.OnTouchListener {
        private var initialY = 0f
        private var initialTouchY = 0f

        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = v.translationY
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialTouchY
                    if (deltaY > 0) { // Only allow swiping down
                        v.translationY = initialY + deltaY
                    }
                    return true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val deltaY = event.rawY - initialTouchY
                    if (deltaY > commandBarHeight / 2) {
                        // Swiped more than half way, dismiss
                        dismissCommandBar()
                    } else {
                        // Snap back
                        v.animate()
                            .translationY(0f)
                            .setDuration(200)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                    return true
                }
            }
            return false
        }
    }

    // ========== Legacy Methods (Backward Compatibility) ==========

    /**
     * Show progress overlay
     *
     * Displays progress overlay with message. If already showing, updates
     * the message instead.
     *
     * FIX (2025-12-04): Added null-safety checks for progressOverlay
     * Root cause: progressOverlay can now be null after cleanup
     * Solution: Recreate overlay if null, use safe call operator
     *
     * @param message Message to display (e.g., "Learning Instagram...")
     */
    fun showProgressOverlay(message: String = "Loading...") {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                // Ensure overlay exists (recreate if cleaned up)
                if (progressOverlay == null) {
                    progressOverlay = ProgressOverlay(context)
                }

                if (!isOverlayVisible) {
                    progressOverlay?.show(windowManager, message)
                    isOverlayVisible = true
                } else {
                    // Already showing, just update message
                    progressOverlay?.updateMessage(message)
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
     * FIX (2025-12-04): Added null-safety check
     *
     * @param message New message text
     */
    fun updateMessage(message: String) {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                if (isOverlayVisible) {
                    progressOverlay?.updateMessage(message)
                }
            }
        }

    }

    /**
     * Hide progress overlay
     *
     * Dismisses the progress overlay. Safe to call even if not showing.
     *
     * FIX (2025-12-04): CRITICAL MEMORY LEAK FIX - Clear progressOverlay reference
     * Root cause: progressOverlay reference persisted after dismiss(), preventing GC
     * Leak chain: LearnAppIntegration → ProgressOverlayManager → progressOverlay → rootView → FrameLayout (168.4 KB retained)
     * Solution: Always set progressOverlay = null in finally block to break leak chain
     * Result: Allows GC to collect dismissed overlay and all its views
     */
    fun hideProgressOverlay() {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                if (isOverlayVisible) {
                    try {
                        progressOverlay?.dismiss(windowManager)
                    } catch (e: Exception) {
                        android.util.Log.e("ProgressOverlayManager", "Failed to dismiss overlay: ${e.message}", e)
                    } finally {
                        // ✅ FIX: Always clear reference to allow GC, even if dismiss() throws
                        progressOverlay = null
                        isOverlayVisible = false
                    }
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
     * Releases resources. Call when manager is no longer needed.
     *
     * Phase 3: Updated to clean up both command bar and legacy overlay.
     * FIX (2025-12-04): Enhanced cleanup to ensure all references are cleared
     * Root cause: Memory leak - cleanup() called progressOverlay.cleanup() but didn't clear reference
     * Solution: Hide overlay, call cleanup(), then clear reference
     */
    fun cleanup() {
        mainScope.launch {
            withContext(Dispatchers.Main) {
                try {
                    // Phase 3: Dismiss command bar
                    if (isCommandBarVisible) {
                        windowManager.removeView(commandBarView)
                        isCommandBarVisible = false
                    }
                    commandBarView = null
                    progressIndicator = null
                    statusText = null
                    pauseButton = null
                    closeButton = null

                    // Legacy: Hide overlay first (this will clear progressOverlay reference)
                    hideProgressOverlay()
                    // Note: progressOverlay is already null after hideProgressOverlay()
                    // but we call cleanup() defensively in case hide wasn't called
                    progressOverlay?.cleanup()
                } catch (e: Exception) {
                    android.util.Log.e("ProgressOverlayManager", "Error during cleanup: ${e.message}", e)
                } finally {
                    // Ensure references are cleared even if exceptions occur
                    progressOverlay = null
                    isOverlayVisible = false
                    commandBarView = null
                    isCommandBarVisible = false
                    explorationEngine = null
                }
            }
        }
    }
}
