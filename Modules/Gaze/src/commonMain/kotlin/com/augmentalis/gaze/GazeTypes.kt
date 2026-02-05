/**
 * GazeTypes.kt - Core gaze tracking data types (KMP)
 *
 * Platform-agnostic gaze types for eye tracking and gaze-based input.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.gaze

import kotlinx.serialization.Serializable

/**
 * Gaze tracking source
 */
@Serializable
enum class GazeSource {
    FRONT_CAMERA,      // Device front camera
    EXTERNAL_TRACKER,  // External eye tracker
    GLASSES,           // Smart glasses eye tracking
    SIMULATED          // For testing
}

/**
 * Eye identifier
 */
@Serializable
enum class Eye {
    LEFT,
    RIGHT,
    BOTH
}

/**
 * 2D gaze position
 */
@Serializable
data class GazePoint(
    val x: Float,
    val y: Float,
    val confidence: Float = 1.0f,
    val timestamp: Long = 0L
) {
    companion object {
        val Zero = GazePoint(0f, 0f)
        val Invalid = GazePoint(-1f, -1f, 0f)
    }

    val isValid: Boolean get() = confidence > 0.0f && x >= 0f && y >= 0f
}

/**
 * Eye state information
 */
@Serializable
data class EyeState(
    val eye: Eye,
    val isOpen: Boolean = true,
    val openness: Float = 1.0f, // 0.0 = closed, 1.0 = fully open
    val pupilSize: Float = 0f   // Relative pupil size
)

/**
 * Complete gaze sample from tracker
 */
@Serializable
data class GazeSample(
    val point: GazePoint,
    val leftEye: EyeState? = null,
    val rightEye: EyeState? = null,
    val source: GazeSource = GazeSource.FRONT_CAMERA,
    val timestamp: Long = 0L
) {
    val isValid: Boolean get() = point.isValid
    val isBothEyesOpen: Boolean
        get() = (leftEye?.isOpen ?: true) && (rightEye?.isOpen ?: true)
}

/**
 * Calibration point for gaze calibration
 */
@Serializable
data class CalibrationPoint(
    val x: Float,
    val y: Float,
    val index: Int,
    val samples: List<GazePoint> = emptyList()
) {
    val isComplete: Boolean get() = samples.size >= REQUIRED_SAMPLES

    companion object {
        const val REQUIRED_SAMPLES = 10
    }
}

/**
 * Calibration state
 */
@Serializable
sealed class CalibrationState {
    @Serializable
    data object NotCalibrated : CalibrationState()

    @Serializable
    data class InProgress(
        val currentPoint: Int,
        val totalPoints: Int,
        val samplesCollected: Int
    ) : CalibrationState()

    @Serializable
    data class Completed(
        val accuracy: Float,
        val timestamp: Long
    ) : CalibrationState()

    @Serializable
    data class Failed(
        val reason: String
    ) : CalibrationState()
}

/**
 * Gaze tracker configuration
 */
@Serializable
data class GazeConfig(
    val source: GazeSource = GazeSource.FRONT_CAMERA,
    val samplingRateHz: Int = 30,
    val smoothingEnabled: Boolean = true,
    val smoothingFactor: Float = 0.5f,
    val dwellEnabled: Boolean = true,
    val dwellTimeMs: Long = 1000L,
    val dwellRadiusPx: Float = 50f,
    val calibrationPoints: Int = 9
)

/**
 * Gaze event for dwell/fixation detection
 */
@Serializable
sealed class GazeEvent {
    @Serializable
    data class Fixation(
        val point: GazePoint,
        val durationMs: Long
    ) : GazeEvent()

    @Serializable
    data class Saccade(
        val from: GazePoint,
        val to: GazePoint,
        val velocityPxPerSec: Float
    ) : GazeEvent()

    @Serializable
    data class Blink(
        val eye: Eye,
        val durationMs: Long
    ) : GazeEvent()

    @Serializable
    data class DwellActivated(
        val point: GazePoint,
        val dwellTimeMs: Long
    ) : GazeEvent()
}

/**
 * Gaze tracker state
 */
@Serializable
data class GazeTrackerState(
    val isTracking: Boolean = false,
    val currentGaze: GazePoint? = null,
    val calibrationState: CalibrationState = CalibrationState.NotCalibrated,
    val lastError: String? = null
)
