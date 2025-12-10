package com.avanues.cockpit.layout.presets

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ARC_FRONT Layout Preset
 *
 * Arranges windows in a semicircular arc in front of the user, creating an immersive
 * wraparound workspace. Windows are positioned on the perimeter of an arc and rotated
 * to face the user.
 *
 * Recursive Design Analysis (.rot):
 *
 * Level 1: Arc Geometry Fundamentals
 * - User positioned at center of arc
 * - Windows arranged in semicircle facing inward
 * - Arc span: 120° (comfortable viewing angle without neck strain)
 * - Radius: 3.0m (optimal arm's reach in VR, matches Vision Pro recommendations)
 *
 * Level 2: Window Positioning Algorithm
 * - Distribute windows evenly across arc span
 * - Calculate angle step: arcAngle / (windowCount - 1)
 * - Position each window on arc perimeter using polar coordinates
 * - Rotate windows tangent to arc (perpendicular to radius)
 *
 * Level 3: Edge Cases
 * - 1 window: Center position (0° relative to user forward)
 * - 2 windows: ±60° from center (left/right extremes)
 * - 3+ windows: Even distribution across arc span
 * - Handle varying window sizes (use uniform dimensions)
 *
 * Level 4: Optimization
 * - Pre-calculate sin/cos for common angles (cached constants)
 * - Use symmetry (mirror left/right positions for even counts)
 * - Cache arc positions for same window count (future enhancement)
 * - Minimize trigonometric calls by calculating once per window
 *
 * Formula:
 *   For window at index i of N windows:
 *     angle = (i * arcSpan / (N-1) - arcSpan/2) * (PI/180)
 *     x = radius * sin(angle)
 *     z = centerPoint.z - radius * (1 - cos(angle))
 *     rotationY = -angle * (180/PI)  // Face user (perpendicular to radius)
 *
 * Voice: "Arc mode"
 */
object ArcFrontLayout : LayoutPreset {

    override val id = "ARC_FRONT"
    override val voiceCommand = "Arc mode"
    override val maxWindows = 5
    override val minWindows = 1

    // Arc configuration constants
    private const val ARC_RADIUS_METERS = 3.0f
    private const val ARC_SPAN_DEGREES = 120f
    private const val DEG_TO_RAD = (PI / 180.0).toFloat()
    private const val RAD_TO_DEG = (180.0 / PI).toFloat()

    override fun calculatePositions(
        windows: List<AppWindow>,
        centerPoint: Vector3D
    ): List<WindowPosition> {
        val count = windows.size.coerceIn(minWindows, maxWindows)

        // Edge case: Single window at center (0° angle)
        if (count == 1) {
            return listOf(
                WindowPosition(
                    windowId = windows[0].id,
                    position = Vector3D(
                        x = centerPoint.x,
                        y = centerPoint.y,
                        z = centerPoint.z - ARC_RADIUS_METERS
                    ),
                    rotationX = 0f,
                    rotationY = 0f,
                    rotationZ = 0f
                )
            )
        }

        // Calculate angle step between windows
        val angleStep = ARC_SPAN_DEGREES / (count - 1)

        return windows.take(count).mapIndexed { index, window ->
            // Calculate angle for this window (-60° to +60° for 120° span)
            val angleDegrees = (index * angleStep) - (ARC_SPAN_DEGREES / 2)
            val angleRadians = angleDegrees * DEG_TO_RAD

            // Calculate position on arc perimeter using polar coordinates
            // x = radius * sin(angle)  [left-right position]
            // z = centerPoint.z - radius * (1 - cos(angle))  [depth position]
            val x = centerPoint.x + (ARC_RADIUS_METERS * sin(angleRadians))
            val y = centerPoint.y
            val z = centerPoint.z - ARC_RADIUS_METERS * (1 - cos(angleRadians))

            // Rotate window to face user (perpendicular to arc radius)
            // Negative angle because we want window normal to point inward
            val rotationY = -angleDegrees

            WindowPosition(
                windowId = window.id,
                position = Vector3D(x, y, z),
                rotationX = 0f,
                rotationY = rotationY,
                rotationZ = 0f
            )
        }
    }

    override fun calculateDimensions(
        window: AppWindow,
        index: Int,
        totalWindows: Int
    ): WindowDimensions {
        // Uniform dimensions for arc layout
        // Slightly smaller than linear layout to accommodate arc curvature
        return WindowDimensions(
            widthMeters = 0.7f,   // ~27 inches at 3m distance
            heightMeters = 0.55f  // ~22 inches at 3m distance
        )
    }
}
