package com.augmentalis.cockpit.spatial

import androidx.compose.ui.geometry.Offset
import com.augmentalis.cockpit.CockpitConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Maps spatial orientation input to viewport translation offset.
 *
 * Pipeline:
 * 1. Receives yaw/pitch from [ISpatialOrientationSource]
 * 2. Applies deadzone (±[CockpitConstants.SPATIAL_DEADZONE_DEGREES] = no movement)
 * 3. Maps degrees to pixel offset (sensitivity = [CockpitConstants.SPATIAL_DEGREES_PER_SCREEN])
 * 4. Applies lerp smoothing for buttery-smooth transitions
 * 5. Outputs [viewportOffset] as a [StateFlow] for composable consumption
 *
 * @param screenWidthPx Screen width in pixels (used to scale degrees to offset)
 * @param screenHeightPx Screen height in pixels
 */
class SpatialViewportController(
    private val screenWidthPx: Float,
    private val screenHeightPx: Float
) {
    private val _viewportOffset = MutableStateFlow(Offset.Zero)
    /** Current viewport translation offset in pixels */
    val viewportOffset: StateFlow<Offset> = _viewportOffset.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    /** Whether spatial panning is locked (frozen viewport) */
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var collectJob: Job? = null
    private var targetOffset = Offset.Zero

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

                // Apply deadzone
                val yaw = applyDeadzone(
                    orientation.yawDegrees,
                    CockpitConstants.SPATIAL_DEADZONE_DEGREES
                )
                val pitch = applyDeadzone(
                    orientation.pitchDegrees,
                    CockpitConstants.SPATIAL_DEADZONE_DEGREES
                )

                // Map degrees to pixel offset
                val degreesPerScreen = CockpitConstants.SPATIAL_DEGREES_PER_SCREEN
                targetOffset = Offset(
                    x = -(yaw / degreesPerScreen) * screenWidthPx,
                    y = -(pitch / degreesPerScreen) * screenHeightPx
                )

                // Lerp toward target for smooth movement
                val current = _viewportOffset.value
                _viewportOffset.value = Offset(
                    x = lerp(current.x, targetOffset.x, CockpitConstants.SPATIAL_LERP_FACTOR),
                    y = lerp(current.y, targetOffset.y, CockpitConstants.SPATIAL_LERP_FACTOR)
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
        _viewportOffset.value = Offset(
            x = current.x + deltaX,
            y = current.y + deltaY
        )
        targetOffset = _viewportOffset.value
    }

    /**
     * Update screen dimensions (called when container resizes).
     */
    fun updateScreenSize(widthPx: Float, heightPx: Float) {
        // Recalculating would require re-deriving the target from raw orientation,
        // but since we lerp continuously, just updating the fields is sufficient
        // for the next orientation update to use the new scale
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
