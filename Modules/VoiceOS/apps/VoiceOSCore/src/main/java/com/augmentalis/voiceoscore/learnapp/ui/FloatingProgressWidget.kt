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
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.augmentalis.voiceoscore.R
import com.augmentalis.voiceoscore.learnapp.debugging.DebugOverlayManager
import com.augmentalis.voiceoscore.learnapp.debugging.DebugVerbosity
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
    private var debugButton: MaterialButton? = null
    private var verbosityButton: MaterialButton? = null
    private var dragHandle: View? = null
    private var sizeDecreaseButton: MaterialButton? = null
    private var sizeIncreaseButton: MaterialButton? = null
    private var sizeLabel: MaterialTextView? = null

    // Debug overlay manager
    private var debugOverlayManager: DebugOverlayManager? = null
    private var isDebugOverlayEnabled = true  // Default ON during exploration

    // Size state: S=0.7, M=1.0, L=1.3, XL=1.6
    private val sizeScales = floatArrayOf(0.7f, 1.0f, 1.3f, 1.6f)
    private val sizeNames = arrayOf("S", "M", "L", "XL")
    private var currentSizeIndex = 1 // Default: M (1.0x)

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
     * FIX (2025-12-07): Added recovery mechanism for detached widget
     * If the widget gets detached from WindowManager (e.g., during screen navigation),
     * this will detect the issue and re-add the view.
     *
     * @param progress Progress percentage (0-100)
     * @param status Status message (e.g., "Learning Instagram...")
     * @param stats Stats text (e.g., "12 screens, 145 elements")
     */
    fun updateProgress(progress: Int, status: String, stats: String) {
        scope.launch {
            withContext(Dispatchers.Main) {
                try {
                    // FIX (2025-12-08): Removed faulty recovery mechanism that caused duplicate widgets
                    // The windowToken check was unreliable and caused addView to be called
                    // while the view was still attached, creating duplicates.
                    // If the widget is not showing, simply skip the update.
                    if (!isShowing) {
                        Log.d(TAG, "Widget not showing, skipping progress update")
                        return@withContext
                    }

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
                    // FIX (2025-12-08): Removed faulty recovery mechanism that caused duplicate widgets
                    if (!isShowing) {
                        Log.d(TAG, "Widget not showing, skipping pause state update")
                        return@withContext
                    }

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
     *
     * FIX (2025-12-08): Simplified - just return isShowing flag
     * The windowToken check was unreliable and caused false negatives
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
                    debugOverlayManager?.dispose()
                    debugOverlayManager = null
                    widgetView = null
                    layoutParams = null
                    progressIndicator = null
                    progressPercent = null
                    statusText = null
                    statsText = null
                    pauseButton = null
                    stopButton = null
                    debugButton = null
                    verbosityButton = null
                    dragHandle = null
                    sizeDecreaseButton = null
                    sizeIncreaseButton = null
                    sizeLabel = null
                } catch (e: Exception) {
                    Log.e(TAG, "Error during cleanup", e)
                }
            }
        }
    }

    // ========== Debug Overlay Methods ==========

    /**
     * Get or create the debug overlay manager
     */
    fun getDebugOverlayManager(): DebugOverlayManager {
        if (debugOverlayManager == null) {
            debugOverlayManager = DebugOverlayManager(context, windowManager)
        }
        return debugOverlayManager!!
    }

    /**
     * Toggle debug overlay visibility
     */
    private fun toggleDebugOverlay() {
        isDebugOverlayEnabled = !isDebugOverlayEnabled

        val manager = getDebugOverlayManager()
        if (isDebugOverlayEnabled) {
            manager.show()
            debugButton?.setIconResource(R.drawable.ic_visibility)
            debugButton?.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0x4D2196F3)
            )
            Log.i(TAG, "Debug overlay enabled")
        } else {
            manager.hide()
            debugButton?.setIconResource(R.drawable.ic_visibility_off)
            debugButton?.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0x4D757575)
            )
            Log.i(TAG, "Debug overlay disabled")
        }
    }

    /**
     * Cycle through verbosity levels (now toggles overlay visibility)
     *
     * NOTE (2025-12-08): Verbosity no longer used - new overlay has filter buttons.
     * This method now toggles the debug overlay visibility.
     */
    private fun cycleVerbosity() {
        // Toggle visibility instead of cycling verbosity
        toggleDebugOverlay()
        Log.i(TAG, "Debug overlay toggled via verbosity button")
    }

    /**
     * Check if debug overlay is enabled
     */
    fun isDebugOverlayEnabled(): Boolean = isDebugOverlayEnabled

    /**
     * Enable debug overlay and show it
     */
    fun enableDebugOverlay() {
        if (!isDebugOverlayEnabled) {
            toggleDebugOverlay()
        } else {
            // Already enabled flag, but ensure overlay is actually shown
            val manager = getDebugOverlayManager()
            if (!manager.isVisible()) {
                manager.show()
                Log.i(TAG, "Debug overlay shown (was enabled but not visible)")
            }
        }
    }

    /**
     * Disable debug overlay and hide it
     */
    fun disableDebugOverlay() {
        if (isDebugOverlayEnabled) {
            toggleDebugOverlay()
        }
    }

    // ========== Private Methods ==========

    @SuppressLint("ClickableViewAccessibility")
    private fun createWidget() {
        // Use MaterialThemeHelper to inflate Material3 views in AccessibilityService context
        widgetView = MaterialThemeHelper.inflateOverlay(context, R.layout.floating_progress_widget)

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

            // Debug overlay toggle button
            debugButton = view.findViewById(R.id.floating_debug_button)
            debugButton?.setOnClickListener {
                toggleDebugOverlay()
            }

            // Verbosity toggle button
            verbosityButton = view.findViewById(R.id.floating_verbosity_button)
            verbosityButton?.setOnClickListener {
                cycleVerbosity()
            }

            // Size control buttons
            sizeDecreaseButton = view.findViewById(R.id.floating_size_decrease_button)
            sizeIncreaseButton = view.findViewById(R.id.floating_size_increase_button)
            sizeLabel = view.findViewById(R.id.floating_size_label)

            sizeDecreaseButton?.setOnClickListener {
                decreaseSize()
            }
            sizeIncreaseButton?.setOnClickListener {
                increaseSize()
            }

            // Update initial size label
            updateSizeLabel()

            // Set up drag on entire widget
            view.setOnTouchListener(DragTouchListener())

            // Also allow drag via drag handle specifically
            dragHandle?.setOnTouchListener(DragTouchListener())
        }
    }

    /**
     * Increase widget size (up to XL)
     */
    private fun increaseSize() {
        if (currentSizeIndex < sizeScales.size - 1) {
            currentSizeIndex++
            applyScale()
            updateSizeLabel()
            Log.i(TAG, "Widget size increased to ${sizeNames[currentSizeIndex]}")
        }
    }

    /**
     * Decrease widget size (down to S)
     */
    private fun decreaseSize() {
        if (currentSizeIndex > 0) {
            currentSizeIndex--
            applyScale()
            updateSizeLabel()
            Log.i(TAG, "Widget size decreased to ${sizeNames[currentSizeIndex]}")
        }
    }

    /**
     * Apply current scale to widget
     */
    private fun applyScale() {
        widgetView?.let { view ->
            val scale = sizeScales[currentSizeIndex]
            view.scaleX = scale
            view.scaleY = scale
            view.pivotX = view.width / 2f
            view.pivotY = 0f // Scale from top
        }
    }

    /**
     * Update size label text
     */
    private fun updateSizeLabel() {
        sizeLabel?.text = "Size: ${sizeNames[currentSizeIndex]}"
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
