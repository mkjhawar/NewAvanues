/**
 * CursorController.kt - Main cursor control orchestrator (KMP)
 *
 * Coordinates cursor movement, filtering, and dwell click.
 * Platform-agnostic core logic.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voicecursor.core

import com.augmentalis.voicecursor.filter.CompositeCursorFilter
import com.augmentalis.voicecursor.filter.ICursorFilter
import com.augmentalis.voicecursor.gaze.GazeClickManager
import com.augmentalis.voicecursor.gaze.GazeResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main cursor controller that orchestrates:
 * - Position tracking and updates
 * - Input filtering (jitter reduction)
 * - Dwell click (gaze-based auto-click)
 * - Cursor state management
 */
class CursorController(
    config: CursorConfig = CursorConfig()
) {
    private var _config = config

    // State
    private val _state = MutableStateFlow(CursorState())
    val state: StateFlow<CursorState> = _state.asStateFlow()

    // Sub-components
    private val filter: ICursorFilter = CompositeCursorFilter(_config.filterStrength)
    private val gazeManager = GazeClickManager(
        GazeConfig(
            autoClickTimeMs = _config.dwellClickDelayMs,
            cancelDistance = 50f,
            centerDistanceTolerance = 6f
        )
    )

    // Screen bounds
    private var screenWidth: Float = 1920f
    private var screenHeight: Float = 1080f

    // Callbacks
    private var onClickCallback: ((CursorPosition) -> Unit)? = null
    private var onDwellStartCallback: (() -> Unit)? = null
    private var onDwellEndCallback: (() -> Unit)? = null

    /**
     * Initialize with screen dimensions
     */
    fun initialize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        if (_config.dwellClickEnabled) {
            gazeManager.enable()
        }
    }

    /**
     * Update cursor with input
     *
     * @param input Cursor input (head movement, eye gaze, direct position, delta)
     * @param currentTimeMs Current time in milliseconds
     * @param isOverInteractive Whether cursor is over an interactive element
     * @return CursorAction if any action should be triggered
     */
    fun update(
        input: CursorInput,
        currentTimeMs: Long,
        isOverInteractive: Boolean = true
    ): CursorAction? {
        val currentState = _state.value

        // Convert input to position
        val rawPosition = when (input) {
            is CursorInput.DirectPosition -> CursorPosition(input.x, input.y)
            is CursorInput.Delta -> CursorPosition(
                currentState.position.x + input.dx * _config.speed,
                currentState.position.y + input.dy * _config.speed
            )
            is CursorInput.HeadMovement -> CursorPosition(
                currentState.position.x + input.yaw * _config.speed * 10,
                currentState.position.y + input.pitch * _config.speed * 10
            )
            is CursorInput.EyeGaze -> CursorPosition(
                input.x * screenWidth,
                input.y * screenHeight
            )
        }

        // Apply filter
        val filteredPosition = if (_config.jitterFilterEnabled) {
            filter.filter(rawPosition)
        } else {
            rawPosition
        }

        // Clamp to screen bounds
        val clampedPosition = CursorPosition(
            x = filteredPosition.x.coerceIn(0f, screenWidth),
            y = filteredPosition.y.coerceIn(0f, screenHeight)
        )

        // Check dwell click
        var action: CursorAction? = null
        var dwellProgress = 0f
        var isDwelling = false

        if (_config.dwellClickEnabled) {
            val gazeResult = gazeManager.update(clampedPosition, currentTimeMs, isOverInteractive)

            when (gazeResult) {
                is GazeResult.Click -> {
                    action = CursorAction.DwellClick
                    onClickCallback?.invoke(gazeResult.position)
                    onDwellEndCallback?.invoke()
                }
                is GazeResult.Dwelling -> {
                    dwellProgress = gazeResult.progress
                    isDwelling = true
                    if (gazeResult.progress > 0.01f && !currentState.isDwellInProgress) {
                        onDwellStartCallback?.invoke()
                    }
                }
                is GazeResult.Moved, is GazeResult.Tracking -> {
                    if (currentState.isDwellInProgress) {
                        onDwellEndCallback?.invoke()
                    }
                }
                GazeResult.Inactive -> { /* No dwell tracking */ }
            }
        }

        // Update state
        _state.value = currentState.copy(
            position = clampedPosition,
            isDwellInProgress = isDwelling,
            dwellProgress = dwellProgress,
            isGazeActive = gazeManager.isEnabled()
        )

        return action
    }

    /**
     * Set cursor visibility
     */
    fun setVisible(visible: Boolean) {
        _state.value = _state.value.copy(isVisible = visible)
    }

    /**
     * Lock cursor at current position
     */
    fun lock() {
        val current = _state.value
        _state.value = current.copy(
            isLocked = true,
            lockedPosition = current.position
        )
    }

    /**
     * Unlock cursor
     */
    fun unlock() {
        _state.value = _state.value.copy(isLocked = false)
    }

    /**
     * Enable/disable dwell click
     */
    fun setDwellClickEnabled(enabled: Boolean) {
        _config = _config.copy(dwellClickEnabled = enabled)
        if (enabled) {
            gazeManager.enable()
        } else {
            gazeManager.disable()
        }
    }

    /**
     * Set dwell click delay
     */
    fun setDwellClickDelay(delayMs: Long) {
        _config = _config.copy(dwellClickDelayMs = delayMs)
        // Would need to recreate gazeManager with new config
    }

    /**
     * Update configuration
     */
    fun updateConfig(config: CursorConfig) {
        _config = config
    }

    /**
     * Get current configuration
     */
    fun getConfig(): CursorConfig = _config

    /**
     * Set click callback
     */
    fun onClick(callback: (CursorPosition) -> Unit) {
        onClickCallback = callback
    }

    /**
     * Set dwell start callback
     */
    fun onDwellStart(callback: () -> Unit) {
        onDwellStartCallback = callback
    }

    /**
     * Set dwell end callback
     */
    fun onDwellEnd(callback: () -> Unit) {
        onDwellEndCallback = callback
    }

    /**
     * Reset cursor to center
     */
    fun resetToCenter() {
        filter.reset()
        gazeManager.reset()
        _state.value = CursorState(
            position = CursorPosition(screenWidth / 2, screenHeight / 2),
            isVisible = true
        )
    }

    /**
     * Dispose and cleanup
     */
    fun dispose() {
        gazeManager.dispose()
        filter.reset()
        onClickCallback = null
        onDwellStartCallback = null
        onDwellEndCallback = null
    }
}
