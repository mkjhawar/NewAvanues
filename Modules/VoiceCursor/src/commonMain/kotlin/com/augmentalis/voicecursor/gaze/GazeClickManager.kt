/**
 * GazeClickManager.kt - Dwell click / gaze-based auto-click (KMP)
 *
 * Thread-safe gaze detection and auto-click functionality.
 * Triggers click when cursor remains stationary for configured duration.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voicecursor.gaze

import com.augmentalis.voicecursor.core.CursorPosition
import com.augmentalis.voicecursor.core.GazeConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Manages gaze/dwell-based auto-click functionality
 *
 * Usage:
 * ```kotlin
 * val gazeManager = GazeClickManager()
 * gazeManager.enable()
 *
 * // In your cursor update loop:
 * val result = gazeManager.update(cursorPosition, currentTimeMs)
 * if (result.shouldClick) {
 *     performClick(cursorPosition)
 * }
 * ```
 */
class GazeClickManager(
    private val config: GazeConfig = GazeConfig()
) {
    // State
    private val _state = MutableStateFlow(GazeState())
    val state: StateFlow<GazeState> = _state.asStateFlow()

    @Volatile
    private var isEnabled = false

    @Volatile
    private var dwellStartTimeMs: Long = 0

    @Volatile
    private var lastPosition = CursorPosition.Zero

    @Volatile
    private var anchorPosition = CursorPosition.Zero

    private val stateLock = Any()

    /**
     * Enable dwell click tracking
     */
    fun enable() {
        synchronized(stateLock) {
            isEnabled = true
            reset()
        }
    }

    /**
     * Disable dwell click tracking
     */
    fun disable() {
        synchronized(stateLock) {
            isEnabled = false
            reset()
        }
    }

    /**
     * Update gaze tracking with current cursor position
     *
     * @param position Current cursor position
     * @param currentTimeMs Current time in milliseconds
     * @param isInteractive Whether cursor is over an interactive element
     * @return GazeResult indicating if click should be triggered
     */
    fun update(
        position: CursorPosition,
        currentTimeMs: Long,
        isInteractive: Boolean = true
    ): GazeResult {
        if (!isEnabled) {
            return GazeResult.Inactive
        }

        synchronized(stateLock) {
            val distance = position.distanceTo(lastPosition)
            lastPosition = position

            // Check if cursor moved too far - reset tracking
            if (distance > config.cancelDistance) {
                resetInternal(position, currentTimeMs)
                updateState(isDwelling = false, progress = 0f)
                return GazeResult.Moved
            }

            // Check distance from anchor point
            val anchorDistance = position.distanceTo(anchorPosition)
            if (anchorDistance > config.centerDistanceTolerance) {
                // Moved away from anchor, reset
                resetInternal(position, currentTimeMs)
                updateState(isDwelling = false, progress = 0f)
                return GazeResult.Tracking
            }

            // Calculate dwell progress
            val dwellDuration = currentTimeMs - dwellStartTimeMs
            val progress = (dwellDuration.toFloat() / config.autoClickTimeMs).coerceIn(0f, 1f)

            // Check if dwell time reached
            if (dwellDuration >= config.autoClickTimeMs && isInteractive) {
                resetInternal(position, currentTimeMs)
                updateState(isDwelling = false, progress = 0f)
                return GazeResult.Click(position)
            }

            // Still dwelling
            updateState(isDwelling = true, progress = progress)
            return GazeResult.Dwelling(progress)
        }
    }

    /**
     * Force reset the gaze tracking
     */
    fun reset() {
        synchronized(stateLock) {
            dwellStartTimeMs = 0
            lastPosition = CursorPosition.Zero
            anchorPosition = CursorPosition.Zero
            updateState(isDwelling = false, progress = 0f)
        }
    }

    /**
     * Check if currently enabled
     */
    fun isEnabled(): Boolean = isEnabled

    /**
     * Check if currently dwelling (cursor stationary)
     */
    fun isDwelling(): Boolean = _state.value.isDwelling

    /**
     * Get current dwell progress (0.0 to 1.0)
     */
    fun getProgress(): Float = _state.value.progress

    private fun resetInternal(position: CursorPosition, currentTimeMs: Long) {
        dwellStartTimeMs = currentTimeMs
        anchorPosition = position
    }

    private fun updateState(isDwelling: Boolean, progress: Float) {
        _state.value = GazeState(
            isEnabled = isEnabled,
            isDwelling = isDwelling,
            progress = progress,
            anchorPosition = anchorPosition
        )
    }

    /**
     * Cleanup resources
     */
    fun dispose() {
        synchronized(stateLock) {
            isEnabled = false
            reset()
        }
    }
}

/**
 * Gaze tracking state
 */
data class GazeState(
    val isEnabled: Boolean = false,
    val isDwelling: Boolean = false,
    val progress: Float = 0f,
    val anchorPosition: CursorPosition = CursorPosition.Zero
)

/**
 * Result from gaze update
 */
sealed class GazeResult {
    /** Gaze tracking is disabled */
    data object Inactive : GazeResult()

    /** Cursor moved, tracking reset */
    data object Moved : GazeResult()

    /** Cursor is being tracked but not dwelling yet */
    data object Tracking : GazeResult()

    /** Cursor is stationary, dwelling in progress */
    data class Dwelling(val progress: Float) : GazeResult()

    /** Dwell complete, trigger click */
    data class Click(val position: CursorPosition) : GazeResult()
}
