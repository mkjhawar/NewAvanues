/**
 * FloatingProgressWidget.kt - Draggable floating progress widget for LearnApp
 *
 * Shows a compact, draggable progress indicator during app exploration.
 * Can be moved anywhere on screen to avoid blocking content.
 *
 * IMPORTANT: Uses ContextThemeWrapper to apply Material theme in AccessibilityService context.
 * This fixes the "Theme.MaterialComponents required" crash when inflating MaterialCardView.
 *
 * Author: VOS4 Development Team
 * Created: 2025-12-07
 * Fixed: 2025-12-08 - Material theme wrapper for AccessibilityService context
 */

package com.augmentalis.voiceoscore.learnapp.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import com.augmentalis.voiceoscore.R
import android.widget.ProgressBar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Floating, draggable progress widget for LearnApp exploration.
 *
 * Features:
 * - Draggable via drag handle
 * - Circular progress indicator with percentage
 * - Status text
 * - Pause/Resume button
 * - STOP button
 * - Semi-transparent background
 *
 * Usage:
 * ```kotlin
 * val widget = FloatingProgressWidget(service)
 * widget.show()
 * widget.updateProgress(45, "Learning Instagram...", "12 screens")
 * widget.dismiss()
 * ```
 */
class FloatingProgressWidget(
    private val context: Context,
    private val windowManager: WindowManager
) {
    companion object {
        private const val TAG = "FloatingProgressWidget"
    }

    // Coroutine scope for async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var showJob: Job? = null

    // View references
    private var widgetView: View? = null
    private var progressIndicator: ProgressBar? = null
    private var progressPercent: TextView? = null
    private var statusText: TextView? = null
    private var statsText: TextView? = null
    private var pauseButton: ImageButton? = null
    private var stopButton: ImageButton? = null
    private var dragHandle: View? = null

    // State
    private var isShowing = false
    private var isPaused = false

    // Drag state
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    // Callbacks
    var onPauseClicked: (() -> Unit)? = null
    var onResumeClicked: (() -> Unit)? = null
    var onStopClicked: (() -> Unit)? = null

    /**
     * Show the floating widget
     */
    fun show() {
        if (isShowing) return

        showJob = scope.launch {
            try {
                withContext(Dispatchers.Main) {
                    val view = createWidget()
                    if (view != null) {
                        val params = createLayoutParams()
                        windowManager.addView(view, params)
                        widgetView = view
                        isShowing = true
                        setupDragBehavior(view, params)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to show widget", e)
            }
        }
    }

    /**
     * Dismiss the floating widget
     */
    fun dismiss() {
        showJob?.cancel()
        if (!isShowing) return

        try {
            widgetView?.let { view ->
                windowManager.removeView(view)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to dismiss widget", e)
        } finally {
            widgetView = null
            progressIndicator = null
            progressPercent = null
            statusText = null
            statsText = null
            pauseButton = null
            stopButton = null
            dragHandle = null
            isShowing = false
        }
    }

    /**
     * Update progress
     *
     * @param percent Progress percentage (0-100)
     * @param status Status message
     * @param stats Statistics text (e.g., "12 screens, 145 elements")
     */
    fun updateProgress(percent: Int, status: String, stats: String? = null) {
        if (!isShowing) return

        scope.launch(Dispatchers.Main) {
            progressIndicator?.progress = percent.coerceIn(0, 100)
            progressPercent?.text = "$percent%"
            statusText?.text = status
            stats?.let { statsText?.text = it }
        }
    }

    /**
     * Update status text only
     */
    fun updateStatus(status: String) {
        if (!isShowing) return

        scope.launch(Dispatchers.Main) {
            statusText?.text = status
        }
    }

    /**
     * Set paused state (updates button icon)
     */
    fun setPaused(paused: Boolean) {
        isPaused = paused
        scope.launch(Dispatchers.Main) {
            pauseButton?.setImageResource(
                if (paused) R.drawable.ic_play else R.drawable.ic_pause
            )
            pauseButton?.contentDescription = if (paused) "Resume learning" else "Pause learning"
        }
    }

    /**
     * Check if widget is currently showing
     */
    fun isShowing(): Boolean = isShowing

    /**
     * Get debug overlay manager (stub for API compatibility)
     */
    fun getDebugOverlayManager(): DebugOverlayManager = debugOverlayManager

    // Internal debug overlay manager
    private val debugOverlayManager = DebugOverlayManager()

    // ========================================================================
    // Private Implementation
    // ========================================================================

    /**
     * Create the widget view
     *
     * IMPORTANT: Wraps context with ContextThemeWrapper to apply Material theme.
     * This is required because AccessibilityService context doesn't have Material theme.
     */
    private fun createWidget(): View? {
        return try {
            // CRITICAL FIX: Wrap context with Material theme for AccessibilityService
            // Without this, MaterialCardView throws:
            // "The style on this component requires your app theme to be Theme.MaterialComponents"
            val themedContext = ContextThemeWrapper(
                context,
                com.google.android.material.R.style.Theme_Material3_DayNight
            )

            val inflater = LayoutInflater.from(themedContext)
            val view = inflater.inflate(R.layout.floating_progress_widget, null)

            // Get view references
            progressIndicator = view.findViewById(R.id.floating_progress_indicator)
            progressPercent = view.findViewById(R.id.floating_progress_percent)
            statusText = view.findViewById(R.id.floating_status_text)
            statsText = view.findViewById(R.id.floating_stats_text)
            pauseButton = view.findViewById(R.id.floating_pause_button)
            stopButton = view.findViewById(R.id.floating_stop_button)
            dragHandle = view.findViewById(R.id.drag_handle)

            // Setup button listeners
            pauseButton?.setOnClickListener {
                if (isPaused) {
                    onResumeClicked?.invoke()
                } else {
                    onPauseClicked?.invoke()
                }
            }

            stopButton?.setOnClickListener {
                onStopClicked?.invoke()
            }

            view
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to create widget", e)
            null
        }
    }

    /**
     * Create WindowManager layout params for floating overlay
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 200
        }
    }

    /**
     * Setup drag behavior for the widget
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragBehavior(view: View, params: WindowManager.LayoutParams) {
        val dragTarget = dragHandle ?: view.findViewById(R.id.floating_widget_root) ?: view

        dragTarget.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Calculate new position (inverted X because gravity is END)
                    params.x = initialX - (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()

                    // Update view position
                    try {
                        windowManager.updateViewLayout(view, params)
                    } catch (e: Exception) {
                        android.util.Log.e(TAG, "Failed to update layout", e)
                    }
                    true
                }
                else -> false
            }
        }
    }
}
