/**
 * GazeTracker.kt - Core gaze tracking interface (KMP)
 *
 * Platform-agnostic interface for eye tracking.
 * Platform implementations in androidMain/iosMain/desktopMain.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.gaze

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Main gaze tracking interface
 */
interface IGazeTracker {
    /**
     * Current tracker state
     */
    val state: StateFlow<GazeTrackerState>

    /**
     * Flow of gaze samples at configured sample rate
     */
    val gazeFlow: Flow<GazeSample>

    /**
     * Flow of gaze events (fixations, saccades, blinks, dwells)
     */
    val eventFlow: Flow<GazeEvent>

    /**
     * Initialize the tracker with configuration
     */
    suspend fun initialize(config: GazeConfig = GazeConfig()): Result<Unit>

    /**
     * Start tracking
     */
    suspend fun startTracking(): Result<Unit>

    /**
     * Stop tracking
     */
    suspend fun stopTracking()

    /**
     * Start calibration process
     */
    suspend fun startCalibration(): Result<Unit>

    /**
     * Add calibration sample at current gaze position
     */
    suspend fun addCalibrationSample(pointIndex: Int): Result<Unit>

    /**
     * Complete calibration and compute mapping
     */
    suspend fun completeCalibration(): Result<CalibrationState.Completed>

    /**
     * Reset calibration
     */
    fun resetCalibration()

    /**
     * Release resources
     */
    fun release()
}

/**
 * Gaze tracker callback interface
 */
interface GazeTrackerCallback {
    fun onGazeSample(sample: GazeSample)
    fun onGazeEvent(event: GazeEvent)
    fun onCalibrationStateChanged(state: CalibrationState)
    fun onError(error: String)
}

/**
 * Factory for creating platform-specific gaze tracker instances
 */
expect object GazeTrackerFactory {
    /**
     * Create a gaze tracker instance for the current platform
     */
    fun create(): IGazeTracker

    /**
     * Check if gaze tracking is available on this platform
     */
    fun isAvailable(): Boolean
}

/**
 * Stub implementation for platforms without gaze tracking
 */
class StubGazeTracker : IGazeTracker {
    override val state: StateFlow<GazeTrackerState>
        get() = kotlinx.coroutines.flow.MutableStateFlow(GazeTrackerState())

    override val gazeFlow: Flow<GazeSample>
        get() = kotlinx.coroutines.flow.emptyFlow()

    override val eventFlow: Flow<GazeEvent>
        get() = kotlinx.coroutines.flow.emptyFlow()

    override suspend fun initialize(config: GazeConfig): Result<Unit> =
        Result.failure(UnsupportedOperationException("Gaze tracking not available"))

    override suspend fun startTracking(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Gaze tracking not available"))

    override suspend fun stopTracking() {}

    override suspend fun startCalibration(): Result<Unit> =
        Result.failure(UnsupportedOperationException("Gaze tracking not available"))

    override suspend fun addCalibrationSample(pointIndex: Int): Result<Unit> =
        Result.failure(UnsupportedOperationException("Gaze tracking not available"))

    override suspend fun completeCalibration(): Result<CalibrationState.Completed> =
        Result.failure(UnsupportedOperationException("Gaze tracking not available"))

    override fun resetCalibration() {}

    override fun release() {}
}
