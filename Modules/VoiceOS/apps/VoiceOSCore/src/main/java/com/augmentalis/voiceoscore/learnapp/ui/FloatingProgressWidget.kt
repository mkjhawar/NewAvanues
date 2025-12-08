/**
 * FloatingProgressWidget.kt - Draggable Floating Progress Widget
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/ui/FloatingProgressWidget.kt
 *
 * Author: VOS4 Development Team
 * Created: 2025-12-07
 *
 * Floating, draggable, semi-transparent progress widget for LearnApp exploration.
 * Can be moved anywhere on screen to avoid blocking content.
 *
 * Features:
 * - Draggable via touch (entire widget or drag handle)
 * - Semi-transparent background
 * - Progress percentage display
 * - Pause/Resume button
 * - STOP button (interrupts exploration)
 * - Remembers last position
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.augmentalis.voiceoscore.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Floating Progress Widget
 *
 * Displays exploration progress in a draggable, semi-transparent overlay widget.
 * User can move the widget anywhere on screen to avoid blocking content.
 *
 * ## Usage
 * ```kotlin
 * val widget = FloatingProgressWidget(
 *     context = accessibilityService,
 *     onPauseClick = { explorationEngine.pauseExploration() },
 *     onResumeClick = { explorationEngine.resumeExploration() },
 *     onStopClick = { explorationEngine.stopExploration() }
 * )
 *
 * widget.show()
 * widget.updateProgress(45, "Learning Instagram...", "12 screens, 145 elements")
 * widget.dismiss()
 * ```
 *
 * @param context AccessibilityService context (required for TYPE_ACCESSIBILITY_OVERLAY)
 * @param onPauseClick Callback when pause button clicked
 * @param onResumeClick Callback when resume button clicked (widget is paused)
 * @param onStopClick Callback when stop button clicked (interrupts exploration)
 */
class FloatingProgressWidget(
    private val context: AccessibilityService,
    private val onPauseClick: () -> Unit,
    private val onResumeClick: () -> Unit,
    private val onStopClick: () -> Unit
) {
    companion object {
        private const val TAG = "FloatingProgressWidget"

        // Default position (top-right, slightly below status bar)
        private const val DEFAULT_X_OFFSET = 20  // dp from right edge
        private const val DEFAULT_Y_OFFSET = 100 // dp from top
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var widgetView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isShowing = false
    private var isPaused = false

    // UI Elements
    private var progressIndicator: CircularProgressIndicator? = null
    private var progressPercent: MaterialTextView? = null
    private var statusText: MaterialTextView? = null
    private var statsText: MaterialTextView? = null
    private var pauseButton: MaterialButton? = null
    private var stopButton: MaterialButton? = null
    private var dragHandle: View? = null

    // Drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    /**
     * Show the floating widget
     *
     * Displays the widget at the last known position, or default position if first show.
     */
    fun show() {
        scope.launch {
            withContext(Dispatchers.Main) {
                try {
                    if (isShowing) {
                        Log.d(TAG, "Widget already showing")
                        return@withContext
                    }

                    // Create view if needed
                    if (widgetView == null) {
                        createWidget()
                    }

                    // Create layout params
                    layoutParams = createLayoutParams()

                    // Add to window
                    windowManager.addView(widgetView, layoutParams)
                    isShowing = true

                    Log.i(TAG, "Floating progress widget shown")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to show widget", e)
                }
            }
        }
    }

    /**
     * Dismiss the floating widget
     */
    fun dismiss() {
        scope.launch {
            withContext(Dispatchers.Main) {
                try {
                    if (!isShowing) return@withContext

                    widgetView?.let { view ->
                        windowManager.removeView(view)
                    }
                    isShowing = false

                    Log.i(TAG, "Floating progress widget dismissed")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to dismiss widget", e)
                }
            }
        }
    }

    /**
     * Update progress display
     *
     * @param progress Progress percentage (0-100)
     * @param status Status message (e.g., "Learning Instagram...")
     * @param stats Stats text (e.g., "12 screens, 145 elements")
     */
    fun updateProgress(progress: Int, status: String, stats: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                try {
                    progressIndicator?.setProgressCompat(progress, true)
                    progressPercent?.text = "$progress%"
                    statusText?.text = status
                    statsText?.text = stats
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update progress", e)
                }
            }
        }
    }

    /**
     * Update pause state
     *
     * @param paused True if exploration is paused
     */
    fun updatePauseState(paused: Boolean) {
        scope.launch {
            withContext(Dispatchers.Main) {
                try {
                    isPaused = paused
                    pauseButton?.setIconResource(
                        if (paused) R.drawable.ic_play else R.drawable.ic_pause
                    )
                    pauseButton?.contentDescription = if (paused) "Resume learning" else "Pause learning"

                    // Change progress color when paused
                    val color = if (paused) 0xFFFFAB00.toInt() else 0xFF00C853.toInt()
                    progressIndicator?.setIndicatorColor(color)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update pause state", e)
                }
            }
        }
    }

    /**
     * Check if widget is currently showing
     */
    fun isShowing(): Boolean = isShowing

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.launch {
            withContext(Dispatchers.Main) {
                try {
                    dismiss()
                    widgetView = null
                    layoutParams = null
                    progressIndicator = null
                    progressPercent = null
                    statusText = null
                    statsText = null
                    pauseButton = null
                    stopButton = null
                    dragHandle = null
                } catch (e: Exception) {
                    Log.e(TAG, "Error during cleanup", e)
                }
            }
        }
    }

    // ========== Private Methods ==========

    @SuppressLint("ClickableViewAccessibility")
    private fun createWidget() {
        widgetView = LayoutInflater.from(context).inflate(
            R.layout.floating_progress_widget,
            null
        )

        widgetView?.let { view ->
            // Get UI element references
            progressIndicator = view.findViewById(R.id.floating_progress_indicator)
            progressPercent = view.findViewById(R.id.floating_progress_percent)
            statusText = view.findViewById(R.id.floating_status_text)
            statsText = view.findViewById(R.id.floating_stats_text)
            pauseButton = view.findViewById(R.id.floating_pause_button)
            stopButton = view.findViewById(R.id.floating_stop_button)
            dragHandle = view.findViewById(R.id.drag_handle)

            // Set up button click handlers
            pauseButton?.setOnClickListener {
                if (isPaused) {
                    onResumeClick()
                } else {
                    onPauseClick()
                }
            }

            stopButton?.setOnClickListener {
                onStopClick()
            }

            // Set up drag on entire widget
            view.setOnTouchListener(DragTouchListener())

            // Also allow drag via drag handle specifically
            dragHandle?.setOnTouchListener(DragTouchListener())
        }
    }

    private fun createLayoutParams(): WindowManager.LayoutParams {
        val density = context.resources.displayMetrics.density
        val screenWidth = context.resources.displayMetrics.widthPixels

        // Calculate default position (top-right corner)
        val widgetWidth = (180 * density).toInt()
        val xPos = screenWidth - widgetWidth - (DEFAULT_X_OFFSET * density).toInt()
        val yPos = (DEFAULT_Y_OFFSET * density).toInt()

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = xPos
            y = yPos
        }
    }

    /**
     * Touch listener for dragging the widget
     */
    private inner class DragTouchListener : View.OnTouchListener {
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Store initial positions
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    // Calculate new position
                    val deltaX = (event.rawX - initialTouchX).toInt()
                    val deltaY = (event.rawY - initialTouchY).toInt()

                    layoutParams?.let { params ->
                        params.x = initialX + deltaX
                        params.y = initialY + deltaY

                        // Update view position
                        try {
                            windowManager.updateViewLayout(widgetView, params)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to update widget position", e)
                        }
                    }
                    return true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Check if this was a click (minimal movement)
                    val deltaX = kotlin.math.abs(event.rawX - initialTouchX)
                    val deltaY = kotlin.math.abs(event.rawY - initialTouchY)

                    if (deltaX < 10 && deltaY < 10) {
                        // This was a click, not a drag - perform click
                        v.performClick()
                    }
                    return true
                }
            }
            return false
        }
    }
}
