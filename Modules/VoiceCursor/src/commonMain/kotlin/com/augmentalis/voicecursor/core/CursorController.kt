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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlin.math.abs
import kotlin.math.tan

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
    companion object {
        /** Angular dead zone — movements below this threshold are ignored (sensor noise) */
        private const val DEAD_ZONE_RAD = 0.002f
        /** Minimum interval between head tracking updates (ms) */
        private const val HEAD_MIN_INTERVAL_MS = 8L
        /** Horizontal scale for roll-based displacement (primary axis) */
        private const val SCALE_X = 2.0f
        /** Vertical scale for pitch-based displacement */
        private const val SCALE_Y = 3.0f
        /** Horizontal scale for yaw-based displacement (secondary axis) */
        private const val SCALE_Z = 2.0f
    }

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

    // Head tracking state — tracks previous orientation for delta computation
    private var _prevPitch = 0f
    private var _prevYaw = 0f
    private var _prevRoll = 0f
    private var _headTrackingInitialized = false
    private var _lastHeadUpdateMs = 0L
    private var inputFlowJob: Job? = null

    /**
     * Initialize with screen dimensions.
     * Automatically centers the cursor on screen and sets it visible.
     */
    fun initialize(width: Float, height: Float) {
        screenWidth = width
        screenHeight = height
        if (_config.dwellClickEnabled) {
            gazeManager.enable()
        }
        // Auto-center cursor so it appears at screen center on all platforms
        _state.value = CursorState(
            position = CursorPosition(screenWidth / 2f, screenHeight / 2f),
            isVisible = true
        )
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
            is CursorInput.HeadMovement -> computeHeadMovementPosition(input, currentState)
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
     * Connect a continuous input flow for automatic cursor updates.
     * Used for IMU head tracking where data streams continuously from sensors.
     *
     * @param flow Source of cursor input events (e.g., IMU orientation mapped to HeadMovement)
     * @param scope CoroutineScope that controls the collection lifecycle
     */
    fun connectInputFlow(flow: Flow<CursorInput>, scope: CoroutineScope) {
        disconnectInputFlow()
        inputFlowJob = scope.launch {
            flow.collect { input ->
                val now = Clock.System.now().toEpochMilliseconds()
                // Rate-limit head tracking updates to prevent excessive processing
                if (input is CursorInput.HeadMovement) {
                    if (now - _lastHeadUpdateMs < HEAD_MIN_INTERVAL_MS) {
                        return@collect
                    }
                    _lastHeadUpdateMs = now
                }
                update(input, now)
            }
        }
    }

    /**
     * Disconnect the input flow, stopping automatic cursor updates.
     */
    fun disconnectInputFlow() {
        inputFlowJob?.cancel()
        inputFlowJob = null
    }

    /**
     * Compute cursor displacement from IMU head orientation using tangent-based projection.
     *
     * Algorithm (proven in CursorAdapter):
     * 1. Track previous orientation to compute angular deltas
     * 2. Apply dead zone to filter sensor noise
     * 3. Use tan(delta) * screenDimension * scale for natural displacement
     * 4. Fine-tune small movements (below 1% of screen) with 0.4x scaling for precision
     *
     * Axis mapping from IMUManager (via HeadTrackingBridge):
     * - roll (alpha)  → primary horizontal displacement
     * - yaw (gamma)   → secondary horizontal displacement (speed-scaled)
     * - pitch (beta)  → vertical displacement (inverted for natural head-down = cursor-down)
     */
    private fun computeHeadMovementPosition(
        input: CursorInput.HeadMovement,
        currentState: CursorState
    ): CursorPosition {
        // First-time initialization — latch current orientation as reference
        if (!_headTrackingInitialized) {
            _prevPitch = input.pitch
            _prevYaw = input.yaw
            _prevRoll = input.roll
            _headTrackingInitialized = true
            return currentState.position
        }

        // Compute angular deltas
        val dRoll = input.roll - _prevRoll
        val dPitch = input.pitch - _prevPitch
        val dYaw = input.yaw - _prevYaw

        _prevPitch = input.pitch
        _prevYaw = input.yaw
        _prevRoll = input.roll

        // Dead zone — filter out sensor noise below threshold
        if (abs(dRoll) < DEAD_ZONE_RAD &&
            abs(dPitch) < DEAD_ZONE_RAD &&
            abs(dYaw) < DEAD_ZONE_RAD
        ) {
            return currentState.position
        }

        // Safety clamp — reject extreme angles where tan() approaches infinity (~80 degrees)
        if (abs(dRoll) > 1.4f || abs(dPitch) > 1.4f || abs(dYaw) > 1.4f) {
            return currentState.position
        }

        // Tangent-based displacement (proven algorithm from CursorAdapter)
        // Roll drives primary horizontal, yaw adds speed-scaled horizontal, pitch drives vertical
        val speedMult = _config.speed * 0.2
        val disX = (tan(dRoll.toDouble()) * screenWidth * SCALE_X +
                   tan(dYaw.toDouble()) * screenWidth * SCALE_Z * speedMult).toFloat()
        val disY = (tan(dPitch.toDouble()) * screenHeight * SCALE_Y * speedMult).toFloat()

        // Fine tuning — scale down small movements for precision control
        val fineX = if (abs(disX) < screenWidth / 100f) disX * 0.4f else disX
        val fineY = if (abs(disY) < screenHeight / 100f) disY * 0.4f else disY

        return CursorPosition(
            currentState.position.x + fineX,
            currentState.position.y - fineY // Negative: pitch-up in IMU = cursor moves up (Y decreases)
        )
    }

    /**
     * Reset cursor to center
     */
    fun resetToCenter() {
        filter.reset()
        gazeManager.reset()
        _prevPitch = 0f
        _prevYaw = 0f
        _prevRoll = 0f
        _headTrackingInitialized = false
        _state.value = CursorState(
            position = CursorPosition(screenWidth / 2, screenHeight / 2),
            isVisible = true
        )
    }

    /**
     * Dispose and cleanup
     */
    fun dispose() {
        disconnectInputFlow()
        gazeManager.dispose()
        filter.reset()
        _prevPitch = 0f
        _prevYaw = 0f
        _prevRoll = 0f
        _headTrackingInitialized = false
        _lastHeadUpdateMs = 0L
        onClickCallback = null
        onDwellStartCallback = null
        onDwellEndCallback = null
    }
}
