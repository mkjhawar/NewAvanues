package com.augmentalis.cockpit.spatial

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Sensitivity presets for different display contexts.
 *
 * Each preset defines degrees-per-screen (how far to turn head to pan one screen width),
 * deadzone (no-movement zone for stability), and lerp factor (smoothing).
 *
 * @property degreesPerScreen Head rotation needed to pan one full screen width
 * @property deadzoneDegrees No-movement zone in degrees
 * @property lerpFactor Smoothing factor (0=frozen, 1=instant, 0.1-0.2 recommended)
 */
enum class SpatialSensitivity(
    val degreesPerScreen: Float,
    val deadzoneDegrees: Float,
    val lerpFactor: Float
) {
    /** Glass displays: wider range, larger deadzone for walking stability */
    GLASS_LOW(degreesPerScreen = 45f, deadzoneDegrees = 8f, lerpFactor = 0.10f),
    /** Default: balanced for tablet/desktop IMU or manual pan */
    NORMAL(degreesPerScreen = 30f, deadzoneDegrees = 5f, lerpFactor = 0.15f),
    /** High sensitivity: small head movements = large pans */
    HIGH(degreesPerScreen = 15f, deadzoneDegrees = 3f, lerpFactor = 0.20f)
}

/**
 * Maps spatial orientation input to viewport translation offset.
 *
 * Pipeline:
 * 1. Receives yaw/pitch from [ISpatialOrientationSource]
 * 2. Applies deadzone (±[sensitivity.deadzoneDegrees] = no movement)
 * 3. Maps degrees to pixel offset (sensitivity = [sensitivity.degreesPerScreen])
 * 4. Clamps offset to ±[BOUNDS_MULTIPLIER] × screen dimensions
 * 5. Applies lerp smoothing for buttery-smooth transitions
 * 6. Outputs [viewportOffset] as a [StateFlow] for composable consumption
 *
 * @param screenWidthPx Screen width in pixels (used to scale degrees to offset)
 * @param screenHeightPx Screen height in pixels
 * @param sensitivity Initial sensitivity preset
 */
class SpatialViewportController(
    screenWidthPx: Float,
    screenHeightPx: Float,
    sensitivity: SpatialSensitivity = SpatialSensitivity.NORMAL
) {
    private var screenWidthPx: Float = screenWidthPx
    private var screenHeightPx: Float = screenHeightPx

    private val _viewportOffset = MutableStateFlow(Offset.Zero)
    /** Current viewport translation offset in pixels */
    val viewportOffset: StateFlow<Offset> = _viewportOffset.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    /** Whether spatial panning is locked (frozen viewport) */
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    /** Active sensitivity preset */
    var sensitivity: SpatialSensitivity = sensitivity
        private set

    private var collectJob: Job? = null
    private var targetOffset = Offset.Zero

    companion object {
        /** Viewport can pan up to this multiplier × screen dimensions in each direction */
        const val BOUNDS_MULTIPLIER = 1.5f
    }

    /**
     * Start consuming orientation data and updating the viewport offset.
     */
    fun connectToSource(
        source: ISpatialOrientationSource,
        scope: CoroutineScope,
        consumerId: String = "cockpit_spatial"
    ) {
        source.startTracking(consumerId)

        collectJob?.cancel()
        collectJob = scope.launch {
            source.orientationFlow.collect { orientation ->
                if (_isLocked.value) return@collect

                val s = sensitivity

                // Apply deadzone
                val yaw = applyDeadzone(orientation.yawDegrees, s.deadzoneDegrees)
                val pitch = applyDeadzone(orientation.pitchDegrees, s.deadzoneDegrees)

                // Map degrees to pixel offset
                targetOffset = Offset(
                    x = -(yaw / s.degreesPerScreen) * screenWidthPx,
                    y = -(pitch / s.degreesPerScreen) * screenHeightPx
                )

                // Clamp to bounds
                targetOffset = clampToBounds(targetOffset)

                // Lerp toward target for smooth movement
                val current = _viewportOffset.value
                _viewportOffset.value = clampToBounds(
                    Offset(
                        x = lerp(current.x, targetOffset.x, s.lerpFactor),
                        y = lerp(current.y, targetOffset.y, s.lerpFactor)
                    )
                )
            }
        }
    }

    /**
     * Disconnect from the orientation source.
     */
    fun disconnect(source: ISpatialOrientationSource, consumerId: String = "cockpit_spatial") {
        collectJob?.cancel()
        collectJob = null
        source.stopTracking(consumerId)
    }

    /** Lock the viewport (freeze panning) */
    fun lock() { _isLocked.value = true }

    /** Unlock the viewport (resume panning) */
    fun unlock() { _isLocked.value = false }

    /** Toggle lock state */
    fun toggleLock() { _isLocked.value = !_isLocked.value }

    /** Reset viewport to center with immediate snap */
    fun centerView() {
        targetOffset = Offset.Zero
        _viewportOffset.value = Offset.Zero
    }

    /**
     * Update viewport from manual input (touch/mouse drag).
     * Used as fallback when no IMU is available.
     */
    fun applyManualOffset(deltaX: Float, deltaY: Float) {
        if (_isLocked.value) return
        val current = _viewportOffset.value
        val newOffset = clampToBounds(
            Offset(x = current.x + deltaX, y = current.y + deltaY)
        )
        _viewportOffset.value = newOffset
        targetOffset = newOffset
    }

    /**
     * Update screen dimensions (called when container resizes).
     * Re-clamps the current offset to the new bounds.
     */
    fun updateScreenSize(widthPx: Float, heightPx: Float) {
        screenWidthPx = widthPx
        screenHeightPx = heightPx
        // Re-clamp to new bounds so frames don't get stranded off-screen
        _viewportOffset.value = clampToBounds(_viewportOffset.value)
        targetOffset = clampToBounds(targetOffset)
    }

    /**
     * Switch to a different sensitivity preset.
     */
    fun setSensitivity(preset: SpatialSensitivity) {
        sensitivity = preset
    }

    /**
     * Clamp an offset to ±[BOUNDS_MULTIPLIER] × screen dimensions.
     * Prevents the viewport from panning so far that all content disappears.
     */
    private fun clampToBounds(offset: Offset): Offset {
        val maxX = screenWidthPx * BOUNDS_MULTIPLIER
        val maxY = screenHeightPx * BOUNDS_MULTIPLIER
        return Offset(
            x = offset.x.coerceIn(-maxX, maxX),
            y = offset.y.coerceIn(-maxY, maxY)
        )
    }

    private fun applyDeadzone(degrees: Float, deadzone: Float): Float {
        return when {
            abs(degrees) < deadzone -> 0f
            degrees > 0 -> degrees - deadzone
            else -> degrees + deadzone
        }
    }

    private fun lerp(start: Float, stop: Float, fraction: Float): Float {
        return start + (stop - start) * fraction
    }
}
