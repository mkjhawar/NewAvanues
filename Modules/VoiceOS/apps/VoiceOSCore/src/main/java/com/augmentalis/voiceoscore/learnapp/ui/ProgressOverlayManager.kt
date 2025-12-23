/**
 * ProgressOverlayManager.kt - Manages progress overlay lifecycle
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/ui/ProgressOverlayManager.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Manages progress overlay showing exploration progress
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.augmentalis.voiceoscore.learnapp.models.ExplorationProgress

/**
 * Progress Overlay Manager
 *
 * Manages lifecycle of progress overlay showing real-time exploration stats.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val manager = ProgressOverlayManager(context)
 *
 * // Show overlay
 * manager.showProgressOverlay(
 *     progress = explorationProgress,
 *     onPause = { pauseExploration() },
 *     onStop = { stopExploration() }
 * )
 *
 * // Update progress
 * manager.updateProgress(newProgress)
 *
 * // Hide overlay
 * manager.hideProgressOverlay()
 * ```
 *
 * @property context Application context
 *
 * @since 1.0.0
 */
class ProgressOverlayManager(
    private val context: Context
) {

    /**
     * Window manager
     */
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /**
     * Current overlay view
     */
    private var currentOverlayView: ComposeView? = null

    /**
     * Current progress state
     */
    private val progressState = mutableStateOf<ExplorationProgress?>(null)

    /**
     * Overlay visible state
     */
    private val isOverlayVisible = mutableStateOf(false)

    /**
     * Show progress overlay
     *
     * @param progress Initial progress
     * @param onPause Callback for pause button
     * @param onStop Callback for stop button
     */
    fun showProgressOverlay(
        progress: ExplorationProgress,
        onPause: () -> Unit,
        onStop: () -> Unit
    ) {
        // Hide existing overlay if any
        if (isOverlayVisible.value) {
            hideProgressOverlay()
        }

        progressState.value = progress

        // Create Compose view
        val composeView = ComposeView(context).apply {
            setContent {
                val currentProgress = progressState.value
                if (currentProgress != null) {
                    ProgressOverlay(
                        progress = currentProgress,
                        onPause = onPause,
                        onStop = onStop
                    )
                }
            }
        }

        // Create layout params
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )

        // Add view to window
        windowManager.addView(composeView, params)
        currentOverlayView = composeView
        isOverlayVisible.value = true
    }

    /**
     * Update progress
     *
     * @param progress New progress
     */
    fun updateProgress(progress: ExplorationProgress) {
        progressState.value = progress
    }

    /**
     * Hide progress overlay
     */
    fun hideProgressOverlay() {
        currentOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                // View already removed
            }
        }

        currentOverlayView = null
        progressState.value = null
        isOverlayVisible.value = false
    }

    /**
     * Check if overlay is visible
     *
     * @return true if visible
     */
    fun isOverlayShowing(): Boolean {
        return isOverlayVisible.value
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        hideProgressOverlay()
    }
}
