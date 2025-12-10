package com.avanues.cockpit.layout.presets

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D

/**
 * LINEAR_HORIZONTAL Layout Preset (DEFAULT)
 *
 * Arranges 5-6 windows in horizontal array, macOS/Vision Pro style
 *
 * Features:
 * - Even horizontal spacing
 * - Slight 3D perspective tilt
 * - Soft drop shadows
 * - Center window slightly forward (optional)
 *
 * Voice: "Linear mode"
 */
object LinearHorizontalLayout : LayoutPreset {

    override val id = "LINEAR_HORIZONTAL"
    override val voiceCommand = "Linear mode"
    override val maxWindows = 6
    override val minWindows = 1

    override fun calculatePositions(
        windows: List<AppWindow>,
        centerPoint: Vector3D
    ): List<WindowPosition> {
        val count = windows.size.coerceIn(minWindows, maxWindows)

        // Spacing between windows (in meters)
        val windowSpacing = 0.4f

        // Total width of window array
        val totalWidth = (count - 1) * windowSpacing

        // Start position (leftmost window)
        val startX = centerPoint.x - (totalWidth / 2)

        return windows.take(count).mapIndexed { index, window ->
            val x = startX + (index * windowSpacing)
            val y = centerPoint.y
            val z = centerPoint.z

            // Optional: Make center window slightly forward
            val centerIndex = count / 2
            val zOffset = if (index == centerIndex) 0.1f else 0f

            // Slight rotation for 3D effect (5-10 degrees)
            val rotationY = when {
                index < centerIndex -> 5f + (centerIndex - index) * 2f
                index > centerIndex -> -5f - (index - centerIndex) * 2f
                else -> 0f
            }

            WindowPosition(
                windowId = window.id,
                position = Vector3D(x, y, z + zOffset),
                rotationY = rotationY,
                rotationX = 0f,
                rotationZ = 0f
            )
        }
    }

    override fun calculateDimensions(
        window: AppWindow,
        index: Int,
        totalWindows: Int
    ): WindowDimensions {
        // Standard dimensions for linear layout
        return WindowDimensions(
            widthMeters = 0.8f,   // ~31 inches at 2m distance
            heightMeters = 0.6f   // ~24 inches at 2m distance
        )
    }
}

data class WindowPosition(
    val windowId: String,
    val position: Vector3D,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f
)

data class WindowDimensions(
    val widthMeters: Float,
    val heightMeters: Float
)

interface LayoutPreset {
    val id: String
    val voiceCommand: String
    val maxWindows: Int
    val minWindows: Int

    fun calculatePositions(
        windows: List<AppWindow>,
        centerPoint: Vector3D = Vector3D(0f, 0f, -2f)
    ): List<WindowPosition>

    fun calculateDimensions(
        window: AppWindow,
        index: Int,
        totalWindows: Int
    ): WindowDimensions
}
