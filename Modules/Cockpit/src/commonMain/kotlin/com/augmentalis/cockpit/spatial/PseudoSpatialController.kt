package com.augmentalis.cockpit.spatial

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * PseudoSpatial controller for creating depth illusion on flat screens.
 * Uses 4-layer parallax driven by device orientation (gyroscope).
 *
 * Unlike [SpatialViewportController] which pans a virtual viewport (for glasses),
 * this controller creates a parallax depth illusion on phones/tablets by shifting
 * content layers at different rates based on gyroscope input.
 *
 * Layer system:
 * - Background (0.3x): subtle ambient movement, 12dp max offset
 * - Mid-ground (0.6x): session cards, secondary content, 8dp max offset
 * - Foreground (1.0x): module tiles, active content, 4dp max offset
 * - HUD (0x): status bar, command bar, locked in place (no movement)
 *
 * The controller also provides subtle card tilt (3 degrees max) for
 * a pseudo-3D perspective effect on foreground elements.
 */
class PseudoSpatialController {

    /**
     * Computed parallax offsets for all layers, plus card tilt angles.
     * All offset values are in dp. Tilt values are in degrees.
     */
    data class ParallaxState(
        val backgroundOffsetX: Float = 0f,
        val backgroundOffsetY: Float = 0f,
        val midgroundOffsetX: Float = 0f,
        val midgroundOffsetY: Float = 0f,
        val foregroundOffsetX: Float = 0f,
        val foregroundOffsetY: Float = 0f,
        /** Y-axis rotation for foreground cards (left/right tilt) */
        val cardRotationY: Float = 0f,
        /** X-axis rotation for foreground cards (forward/back tilt) */
        val cardRotationX: Float = 0f
    )

    private val _state = MutableStateFlow(ParallaxState())

    /** Current parallax state for composable consumption via collectAsState. */
    val state: StateFlow<ParallaxState> = _state.asStateFlow()

    private val _isEnabled = MutableStateFlow(true)

    /** Whether parallax effects are enabled. Disabled = all offsets zero. */
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()

    // Layer parallax multipliers (how much each layer moves relative to input)
    private val backgroundMultiplier = 0.3f
    private val midgroundMultiplier = 0.6f
    private val foregroundMultiplier = 1.0f

    // Maximum offset bounds per layer (dp)
    private val maxBackgroundOffset = 12f
    private val maxMidgroundOffset = 8f
    private val maxForegroundOffset = 4f

    // Maximum card tilt (degrees)
    private val maxCardTilt = 3f

    // Smoothing factor (0-1, lower = smoother/laggier, higher = more responsive)
    // 0.15 provides buttery-smooth transitions without feeling sluggish
    private val smoothing = 0.15f

    // Normalization range: +-30 degrees of device tilt maps to full parallax
    private val normalizationRange = 30f

    private var collectJob: Job? = null

    /**
     * Update parallax state from orientation data.
     *
     * Normalizes the input angles to [-1, 1] range (clamped at +-[normalizationRange] degrees),
     * then applies per-layer multipliers and lerp smoothing.
     *
     * @param yawDegrees Horizontal rotation from the device orientation source (-180 to 180)
     * @param pitchDegrees Vertical rotation from the device orientation source (-90 to 90)
     */
    fun updateOrientation(yawDegrees: Float, pitchDegrees: Float) {
        if (!_isEnabled.value) return

        val normalizedX = (yawDegrees / normalizationRange).coerceIn(-1f, 1f)
        val normalizedY = (pitchDegrees / normalizationRange).coerceIn(-1f, 1f)

        val current = _state.value
        _state.value = ParallaxState(
            backgroundOffsetX = lerp(
                current.backgroundOffsetX,
                normalizedX * maxBackgroundOffset * backgroundMultiplier,
                smoothing
            ),
            backgroundOffsetY = lerp(
                current.backgroundOffsetY,
                normalizedY * maxBackgroundOffset * backgroundMultiplier,
                smoothing
            ),
            midgroundOffsetX = lerp(
                current.midgroundOffsetX,
                normalizedX * maxMidgroundOffset * midgroundMultiplier,
                smoothing
            ),
            midgroundOffsetY = lerp(
                current.midgroundOffsetY,
                normalizedY * maxMidgroundOffset * midgroundMultiplier,
                smoothing
            ),
            foregroundOffsetX = lerp(
                current.foregroundOffsetX,
                normalizedX * maxForegroundOffset * foregroundMultiplier,
                smoothing
            ),
            foregroundOffsetY = lerp(
                current.foregroundOffsetY,
                normalizedY * maxForegroundOffset * foregroundMultiplier,
                smoothing
            ),
            cardRotationY = lerp(
                current.cardRotationY,
                normalizedX * maxCardTilt,
                smoothing
            ),
            cardRotationX = lerp(
                current.cardRotationX,
                -normalizedY * maxCardTilt, // Inverted: tilt phone forward = card tilts toward viewer
                smoothing
            )
        )
    }

    /**
     * Connect to an [ISpatialOrientationSource] and automatically update parallax.
     * This bridges the existing orientation infrastructure with the pseudo-spatial system.
     *
     * Cancels any previous connection before starting a new one.
     *
     * @param source The orientation data source (gyroscope/IMU)
     * @param scope Coroutine scope for the collection (typically viewModelScope or rememberCoroutineScope)
     * @param consumerId Unique identifier for this consumer (for multi-consumer tracking)
     */
    fun connectToSource(
        source: ISpatialOrientationSource,
        scope: CoroutineScope,
        consumerId: String = "pseudo_spatial"
    ) {
        source.startTracking(consumerId)
        collectJob?.cancel()
        collectJob = scope.launch {
            source.orientationFlow.collect { orientation ->
                updateOrientation(orientation.yawDegrees, orientation.pitchDegrees)
            }
        }
    }

    /**
     * Disconnect from the orientation source.
     */
    fun disconnect(source: ISpatialOrientationSource, consumerId: String = "pseudo_spatial") {
        collectJob?.cancel()
        collectJob = null
        source.stopTracking(consumerId)
    }

    /** Enable parallax effects. */
    fun enable() {
        _isEnabled.value = true
    }

    /** Disable parallax effects and reset all offsets to zero. */
    fun disable() {
        _isEnabled.value = false
        reset()
    }

    /** Toggle enabled state. */
    fun toggle() {
        if (_isEnabled.value) disable() else enable()
    }

    /** Reset all parallax offsets to zero (neutral position). */
    fun reset() {
        _state.value = ParallaxState()
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float =
        start + (end - start) * fraction
}
