/**
 * CursorPositionTracker.kt - Tracks voice cursor position in real-time
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 */
package com.augmentalis.voiceoscore.accessibility.cursor

import android.content.Context
import android.content.res.Configuration
import android.graphics.PointF
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.WindowManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Position data class representing cursor position
 *
 * @param x Absolute x coordinate in pixels
 * @param y Absolute y coordinate in pixels
 * @param normalizedX Normalized x coordinate (0.0 - 1.0)
 * @param normalizedY Normalized y coordinate (0.0 - 1.0)
 * @param displayId Display ID for multi-display support
 * @param timestamp Timestamp when position was updated
 */
data class CursorPosition(
    val x: Float,
    val y: Float,
    val normalizedX: Float,
    val normalizedY: Float,
    val displayId: Int = Display.DEFAULT_DISPLAY,
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Create position from absolute coordinates
         */
        fun fromAbsolute(
            x: Float,
            y: Float,
            screenWidth: Int,
            screenHeight: Int,
            displayId: Int = Display.DEFAULT_DISPLAY
        ): CursorPosition {
            return CursorPosition(
                x = x,
                y = y,
                normalizedX = x / screenWidth.toFloat(),
                normalizedY = y / screenHeight.toFloat(),
                displayId = displayId
            )
        }

        /**
         * Create position from normalized coordinates (0.0 - 1.0)
         */
        fun fromNormalized(
            normalizedX: Float,
            normalizedY: Float,
            screenWidth: Int,
            screenHeight: Int,
            displayId: Int = Display.DEFAULT_DISPLAY
        ): CursorPosition {
            return CursorPosition(
                x = normalizedX * screenWidth,
                y = normalizedY * screenHeight,
                normalizedX = normalizedX,
                normalizedY = normalizedY,
                displayId = displayId
            )
        }
    }

    /**
     * Convert to PointF for graphics operations
     */
    fun toPointF(): PointF = PointF(x, y)

    /**
     * Check if position is within screen bounds
     */
    fun isInBounds(screenWidth: Int, screenHeight: Int): Boolean {
        return x >= 0 && x <= screenWidth && y >= 0 && y <= screenHeight
    }
}

/**
 * Screen bounds data class
 *
 * @param width Screen width in pixels
 * @param height Screen height in pixels
 * @param density Screen density
 * @param displayId Display ID
 */
data class ScreenBounds(
    val width: Int,
    val height: Int,
    val density: Float,
    val displayId: Int = Display.DEFAULT_DISPLAY
) {
    /**
     * Check if point is within bounds
     */
    fun contains(x: Float, y: Float): Boolean {
        return x >= 0 && x <= width && y >= 0 && y <= height
    }

    /**
     * Get center point
     */
    fun getCenter(): PointF = PointF(width / 2f, height / 2f)
}

/**
 * Cursor position tracker
 *
 * Tracks cursor position in real-time across all apps with:
 * - Reactive position updates via StateFlow
 * - Multi-display support
 * - Coordinate normalization
 * - Screen bounds awareness
 */
class CursorPositionTracker(
    private val context: Context
) {
    companion object {
        private const val TAG = "CursorPositionTracker"
        private const val DEFAULT_POSITION_X = 0.5f // Center of screen (normalized)
        private const val DEFAULT_POSITION_Y = 0.5f // Center of screen (normalized)
    }

    // Window manager for display metrics
    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    // Current screen bounds
    private var currentBounds: ScreenBounds

    // Position state flow (reactive updates)
    private val _positionFlow = MutableStateFlow<CursorPosition>(getDefaultPosition())
    val positionFlow: StateFlow<CursorPosition> = _positionFlow.asStateFlow()

    // Position change callbacks
    private val positionChangeCallbacks = mutableListOf<(CursorPosition) -> Unit>()

    init {
        // Initialize with current screen bounds
        currentBounds = getCurrentScreenBounds()
        Log.d(TAG, "CursorPositionTracker initialized with bounds: $currentBounds")
    }

    /**
     * Get current cursor position
     */
    fun getCurrentPosition(): CursorPosition = _positionFlow.value

    /**
     * Update cursor position with absolute coordinates
     *
     * @param x Absolute x coordinate in pixels
     * @param y Absolute y coordinate in pixels
     * @param displayId Optional display ID for multi-display support
     */
    fun updatePosition(x: Float, y: Float, displayId: Int = Display.DEFAULT_DISPLAY) {
        val newPosition = CursorPosition.fromAbsolute(
            x = x,
            y = y,
            screenWidth = currentBounds.width,
            screenHeight = currentBounds.height,
            displayId = displayId
        )

        updatePositionInternal(newPosition)
    }

    /**
     * Update cursor position with normalized coordinates (0.0 - 1.0)
     *
     * @param normalizedX Normalized x coordinate (0.0 - 1.0)
     * @param normalizedY Normalized y coordinate (0.0 - 1.0)
     * @param displayId Optional display ID for multi-display support
     */
    fun updateNormalizedPosition(
        normalizedX: Float,
        normalizedY: Float,
        displayId: Int = Display.DEFAULT_DISPLAY
    ) {
        val newPosition = CursorPosition.fromNormalized(
            normalizedX = normalizedX,
            normalizedY = normalizedY,
            screenWidth = currentBounds.width,
            screenHeight = currentBounds.height,
            displayId = displayId
        )

        updatePositionInternal(newPosition)
    }

    /**
     * Move cursor by delta (relative movement)
     *
     * @param deltaX Delta x in pixels
     * @param deltaY Delta y in pixels
     */
    fun moveBy(deltaX: Float, deltaY: Float) {
        val current = _positionFlow.value
        updatePosition(
            x = current.x + deltaX,
            y = current.y + deltaY,
            displayId = current.displayId
        )
    }

    /**
     * Center cursor on screen
     */
    fun centerCursor() {
        val center = currentBounds.getCenter()
        updatePosition(center.x, center.y)
        Log.d(TAG, "Cursor centered at: $center")
    }

    /**
     * Register position change callback
     *
     * @param callback Function to call when position changes
     */
    fun addPositionChangeCallback(callback: (CursorPosition) -> Unit) {
        positionChangeCallbacks.add(callback)
        Log.d(TAG, "Position change callback registered (total: ${positionChangeCallbacks.size})")
    }

    /**
     * Unregister position change callback
     *
     * @param callback Function to remove
     */
    fun removePositionChangeCallback(callback: (CursorPosition) -> Unit) {
        positionChangeCallbacks.remove(callback)
        Log.d(TAG, "Position change callback unregistered (total: ${positionChangeCallbacks.size})")
    }

    /**
     * Clear all position change callbacks
     */
    fun clearCallbacks() {
        positionChangeCallbacks.clear()
        Log.d(TAG, "All position change callbacks cleared")
    }

    /**
     * Update screen bounds (call when configuration changes)
     */
    fun updateScreenBounds() {
        val oldBounds = currentBounds
        currentBounds = getCurrentScreenBounds()

        Log.d(TAG, "Screen bounds updated: $oldBounds -> $currentBounds")

        // Update current position to maintain relative position after rotation
        val current = _positionFlow.value
        updateNormalizedPosition(
            normalizedX = current.normalizedX,
            normalizedY = current.normalizedY,
            displayId = current.displayId
        )
    }

    /**
     * Get current screen bounds
     */
    fun getScreenBounds(): ScreenBounds = currentBounds

    /**
     * Check if cursor is within screen bounds
     */
    fun isInBounds(): Boolean {
        val position = _positionFlow.value
        return position.isInBounds(currentBounds.width, currentBounds.height)
    }

    /**
     * Internal position update with bounds checking and callbacks
     */
    private fun updatePositionInternal(newPosition: CursorPosition) {
        val oldPosition = _positionFlow.value

        // Update state flow
        _positionFlow.value = newPosition

        // Notify callbacks if position actually changed
        if (oldPosition.x != newPosition.x || oldPosition.y != newPosition.y) {
            Log.v(TAG, "Position updated: (${newPosition.x}, ${newPosition.y}) " +
                    "normalized: (${newPosition.normalizedX}, ${newPosition.normalizedY})")

            // Call all registered callbacks
            positionChangeCallbacks.forEach { callback ->
                try {
                    callback(newPosition)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in position change callback", e)
                }
            }
        }
    }

    /**
     * Get default position (center of screen)
     */
    private fun getDefaultPosition(): CursorPosition {
        val bounds = getCurrentScreenBounds()
        return CursorPosition.fromNormalized(
            normalizedX = DEFAULT_POSITION_X,
            normalizedY = DEFAULT_POSITION_Y,
            screenWidth = bounds.width,
            screenHeight = bounds.height
        )
    }

    /**
     * Get current screen bounds from display metrics
     */
    private fun getCurrentScreenBounds(): ScreenBounds {
        val displayMetrics = DisplayMetrics()

        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)

        return ScreenBounds(
            width = displayMetrics.widthPixels,
            height = displayMetrics.heightPixels,
            density = displayMetrics.density,
            displayId = Display.DEFAULT_DISPLAY
        )
    }

    /**
     * Handle configuration changes (orientation, screen size)
     */
    fun onConfigurationChanged(newConfig: Configuration) {
        Log.d(TAG, "Configuration changed: orientation=${newConfig.orientation}")
        updateScreenBounds()
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        clearCallbacks()
        Log.d(TAG, "CursorPositionTracker disposed")
    }
}
