/**
 * NumberOverlayManager.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09 12:37:30 PDT
 */
/**
 * NumberOverlayManager.kt
 *
 * Purpose: Lifecycle and state management for number overlay system
 * Handles showing/hiding overlays, updating positions, window focus changes
 *
 * Responsibilities:
 * - Overlay window creation and attachment
 * - Show/hide animations
 * - Position updates when UI changes
 * - Window focus tracking
 * - Memory management (bitmap recycling)
 *
 * Created: 2025-10-09 12:37:30 PDT
 */
package com.augmentalis.voiceoscore.ui.overlays

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.*
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.view.isVisible
import kotlinx.coroutines.*
import java.lang.ref.WeakReference
import com.augmentalis.voiceos.accessibility.AnchorPoint
import com.augmentalis.voiceos.accessibility.BadgeStyle
import com.augmentalis.voiceos.accessibility.ElementVoiceState

/**
 * Manager for number overlay lifecycle
 *
 * Usage:
 * ```kotlin
 * val manager = NumberOverlayManager(context, config)
 * manager.show(overlayDataList)
 * manager.hide()
 * manager.release()
 * ```
 *
 * Thread Safety: All public methods are thread-safe
 */
class NumberOverlayManager(
    private val context: Context,
    private var config: NumberOverlayConfig
) {

    // Window manager for overlay attachment
    private val windowManager: WindowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Renderer view
    private var renderer: NumberOverlayRenderer? = null

    // Layout parameters for overlay window
    private var layoutParams: WindowManager.LayoutParams? = null

    // Current state
    private var isShowing = false
    private var currentOverlays = mutableListOf<OverlayData>()

    // Coroutine scope for async operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Window focus listener
    private var windowFocusCallback: WeakReference<WindowFocusListener>? = null

    // Performance tracking
    private var lastUpdateTimeMs: Long = 0
    private var updateCount: Int = 0

    init {
        setupRenderer()
    }

    /**
     * Setup the overlay renderer
     */
    private fun setupRenderer() {
        renderer = NumberOverlayRenderer(context).apply {
            setStyle(config.styleVariant.toStyle())
            setRenderConfig(RenderConfig(
                hardwareAcceleration = config.hardwareAcceleration,
                paintPooling = true,
                cacheTextBounds = true,
                partialInvalidation = true,
                maxOverlaysPerFrame = 100,
                targetFrameTimeMs = 16
            ))
        }

        layoutParams = createLayoutParams()
    }

    /**
     * Create window layout parameters for overlay
     */
    private fun createLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
    }

    /**
     * Show overlays on screen
     *
     * @param overlays List of overlay data to display
     */
    fun show(overlays: List<OverlayData>) {
        scope.launch {
            try {
                // Update current overlays
                currentOverlays.clear()
                currentOverlays.addAll(overlays)

                // Attach to window if not already showing
                if (!isShowing) {
                    attachToWindow()
                }

                // Update renderer
                renderer?.setOverlays(overlays)

                isShowing = true
                updateCount++

                Log.d(TAG, "Showing ${overlays.size} overlays")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to show ${overlays.size} number overlays - window attachment or renderer update failed", e)
            }
        }
    }

    /**
     * Hide overlays from screen
     */
    fun hide() {
        scope.launch {
            try {
                if (isShowing) {
                    detachFromWindow()
                    currentOverlays.clear()
                    isShowing = false

                    Log.d(TAG, "Overlays hidden")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to hide overlays and detach from window - cleanup may be incomplete", e)
            }
        }
    }

    /**
     * Toggle overlay visibility
     */
    fun toggle() {
        if (isShowing) {
            hide()
        } else if (currentOverlays.isNotEmpty()) {
            show(currentOverlays)
        }
    }

    /**
     * Update overlay positions (when UI changes)
     *
     * @param overlays Updated overlay data with new positions
     */
    fun updatePositions(overlays: List<OverlayData>) {
        scope.launch {
            if (isShowing) {
                currentOverlays.clear()
                currentOverlays.addAll(overlays)
                renderer?.setOverlays(overlays)

                val currentTime = System.currentTimeMillis()
                lastUpdateTimeMs = currentTime

                Log.d(TAG, "Updated ${overlays.size} overlay positions")
            }
        }
    }

    /**
     * Update single overlay
     */
    fun updateOverlay(number: Int, newBounds: Rect, newState: ElementVoiceState) {
        scope.launch {
            if (isShowing) {
                val index = currentOverlays.indexOfFirst { it.number == number }
                if (index >= 0) {
                    currentOverlays[index] = currentOverlays[index].copy(
                        elementBounds = newBounds,
                        state = newState
                    )
                    renderer?.setOverlays(currentOverlays)
                }
            }
        }
    }

    /**
     * Add overlay dynamically
     */
    fun addOverlay(overlay: OverlayData) {
        scope.launch {
            currentOverlays.add(overlay)
            if (isShowing) {
                renderer?.addOverlay(overlay)
            }
        }
    }

    /**
     * Remove overlay by number
     */
    fun removeOverlay(number: Int) {
        scope.launch {
            currentOverlays.removeAll { it.number == number }
            if (isShowing) {
                renderer?.removeOverlay(number)
            }
        }
    }

    /**
     * Clear all overlays
     */
    fun clearOverlays() {
        scope.launch {
            currentOverlays.clear()
            renderer?.clearOverlays()
            hide()
        }
    }

    /**
     * Update configuration
     */
    fun updateConfig(newConfig: NumberOverlayConfig) {
        scope.launch {
            this@NumberOverlayManager.config = newConfig

            // Update renderer style
            renderer?.setStyle(newConfig.styleVariant.toStyle())

            // Update render config
            renderer?.setRenderConfig(RenderConfig(
                hardwareAcceleration = newConfig.hardwareAcceleration,
                paintPooling = true,
                cacheTextBounds = true,
                partialInvalidation = true,
                maxOverlaysPerFrame = 100,
                targetFrameTimeMs = 16
            ))

            // Refresh if showing
            if (isShowing) {
                renderer?.setOverlays(currentOverlays)
            }

            Log.d(TAG, "Configuration updated: ${newConfig.styleVariant}")
        }
    }

    /**
     * Handle window focus change
     */
    fun onWindowFocusChanged(hasFocus: Boolean) {
        scope.launch {
            if (!hasFocus && config.hideOnWindowFocusLoss) {
                hide()
            } else if (hasFocus && config.enabled && currentOverlays.isNotEmpty()) {
                show(currentOverlays)
            }

            windowFocusCallback?.get()?.onWindowFocusChanged(hasFocus)
        }
    }

    /**
     * Set window focus listener
     */
    fun setWindowFocusListener(listener: WindowFocusListener) {
        windowFocusCallback = WeakReference(listener)
    }

    /**
     * Get current overlay count
     */
    fun getOverlayCount(): Int = currentOverlays.size

    /**
     * Check if overlays are showing
     */
    fun isShowing(): Boolean = isShowing

    /**
     * Get current configuration
     */
    fun getConfig(): NumberOverlayConfig = config

    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): PerformanceMetrics? {
        return renderer?.getPerformanceMetrics()
    }

    /**
     * Get manager statistics
     */
    fun getStatistics(): ManagerStatistics {
        return ManagerStatistics(
            currentOverlayCount = currentOverlays.size,
            isShowing = isShowing,
            updateCount = updateCount,
            lastUpdateTimeMs = lastUpdateTimeMs,
            performanceMetrics = renderer?.getPerformanceMetrics()
        )
    }

    /**
     * Attach renderer to window
     */
    private fun attachToWindow() {
        try {
            renderer?.let { view ->
                if (view.parent == null) {
                    windowManager.addView(view, layoutParams)
                    view.isVisible = true
                    Log.d(TAG, "Overlay attached to window")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to attach overlay view to WindowManager - check SYSTEM_ALERT_WINDOW permission or window state", e)
            throw e
        }
    }

    /**
     * Detach renderer from window
     */
    private fun detachFromWindow() {
        try {
            renderer?.let { view ->
                if (view.parent != null) {
                    view.isVisible = false
                    windowManager.removeView(view)
                    Log.d(TAG, "Overlay detached from window")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay view from WindowManager - view may still be attached", e)
        }
    }

    /**
     * Release resources
     * MUST be called when manager is no longer needed
     */
    fun release() {
        scope.launch {
            try {
                hide()
                renderer?.clearOverlays()
                renderer = null
                layoutParams = null
                currentOverlays.clear()
                windowFocusCallback = null

                scope.cancel()

                Log.d(TAG, "NumberOverlayManager released")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to release NumberOverlayManager resources - renderer, scope, or window cleanup incomplete", e)
            }
        }
    }

    companion object {
        private const val TAG = "NumberOverlayManager"
    }
}

/**
 * Listener for window focus changes
 */
interface WindowFocusListener {
    fun onWindowFocusChanged(hasFocus: Boolean)
}

/**
 * Statistics for manager monitoring
 */
data class ManagerStatistics(
    val currentOverlayCount: Int,
    val isShowing: Boolean,
    val updateCount: Int,
    val lastUpdateTimeMs: Long,
    val performanceMetrics: PerformanceMetrics?
) {
    /**
     * Get human-readable status
     */
    fun getStatusString(): String {
        return buildString {
            append("Overlays: $currentOverlayCount")
            append(", Showing: $isShowing")
            append(", Updates: $updateCount")
            if (performanceMetrics != null) {
                append(", ${performanceMetrics.getStatusString()}")
            }
        }
    }
}

/**
 * Extension function to create overlays from accessibility nodes
 */
fun List<AccessibilityNodeInfo>.toOverlayData(
    startNumber: Int = 1,
    stateProvider: (AccessibilityNodeInfo) -> ElementVoiceState = { ElementVoiceState.ENABLED_NO_NAME }
): List<OverlayData> {
    return this.mapIndexed { index, node ->
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        OverlayData(
            elementBounds = bounds,
            number = startNumber + index,
            state = stateProvider(node),
            id = node.viewIdResourceName?.toString()
        )
    }
}
