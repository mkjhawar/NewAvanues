package com.avanues.cockpit.layout.presets

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.layout.LayoutPreset
import com.avanues.cockpit.layout.WindowDimensions
import com.avanues.cockpit.layout.WindowPosition

/**
 * LINEAR_HORIZONTAL Layout Preset (DEFAULT)
 *
 * Arranges 5-6 windows in a horizontal array, macOS/Vision Pro style.
 * This is the default layout for Cockpit workspaces.
 *
 * **Visual:**
 * ```
 *     [Win1] [Win2] [Win3] [Win4] [Win5]
 *        ↖      ↖     ↑     ↗      ↗
 *       Slight rotation for 3D effect
 * ```
 *
 * **Features:**
 * - Even horizontal spacing (40cm between windows)
 * - Slight 3D perspective tilt (5-10 degrees rotation)
 * - Center window slightly forward for focus
 * - Soft drop shadows (handled by renderer)
 *
 * **Voice Commands:**
 * - "Linear mode" → Activates this layout
 * - "Horizontal layout" → Alternative activation
 * - "Default layout" → Returns to this layout
 *
 * **Preferred Embodiment:**
 * Matches the macOS/Vision Pro style horizontal window array from
 * the design reference image.
 */
object LinearHorizontalLayout : LayoutPreset {

    override val id = "LINEAR_HORIZONTAL"
    override val voiceCommand = "Linear mode"
    override val maxWindows = 6
    override val minWindows = 1
    override val description = "Horizontal array of windows with slight 3D tilt (macOS/Vision Pro style)"

    /**
     * Window spacing in meters
     * 40cm provides comfortable separation without feeling sparse
     */
    private const val WINDOW_SPACING = 0.4f

    /**
     * Z-offset for center window (10cm forward)
     * Makes the center window slightly more prominent
     */
    private const val CENTER_Z_OFFSET = 0.1f

    /**
     * Base rotation angle for edge windows (degrees)
     * Creates the subtle 3D arc effect
     */
    private const val BASE_ROTATION = 5f

    /**
     * Rotation increment per window position
     * Further from center = more rotation
     */
    private const val ROTATION_INCREMENT = 2f

    override fun calculatePositions(
        windows: List<AppWindow>,
        centerPoint: Vector3D
    ): List<WindowPosition> {
        val count = windows.size.coerceIn(minWindows, maxWindows)

        // Total width of window array
        val totalWidth = (count - 1) * WINDOW_SPACING

        // Start position (leftmost window)
        val startX = centerPoint.x - (totalWidth / 2)

        return windows.take(count).mapIndexed { index, window ->
            val x = startX + (index * WINDOW_SPACING)
            val y = centerPoint.y
            val z = centerPoint.z

            // Center window position
            val centerIndex = count / 2

            // Make center window slightly forward
            val zOffset = if (index == centerIndex) CENTER_Z_OFFSET else 0f

            // Calculate rotation for 3D effect
            // Windows to the left rotate left, windows to the right rotate right
            val rotationY = when {
                index < centerIndex -> BASE_ROTATION + (centerIndex - index) * ROTATION_INCREMENT
                index > centerIndex -> -(BASE_ROTATION + (index - centerIndex) * ROTATION_INCREMENT)
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
        // ~31 inches width × 24 inches height at 2m distance
        return WindowDimensions(
            widthMeters = 0.8f,
            heightMeters = 0.6f
        )
    }

    /**
     * Calculates the ideal center point for a given number of windows
     *
     * Adjusts depth based on window count to maintain comfortable viewing distance.
     *
     * @param windowCount Number of windows to display
     * @return Optimal center point
     */
    fun calculateOptimalCenterPoint(windowCount: Int): Vector3D {
        // More windows = slightly farther back for better field of view
        val baseDepth = -2.0f
        val depthAdjustment = when {
            windowCount <= 2 -> 0.2f  // Closer for 1-2 windows
            windowCount >= 5 -> -0.3f // Farther for 5-6 windows
            else -> 0f
        }

        return Vector3D(
            x = 0f,
            y = 0f,
            z = baseDepth + depthAdjustment
        )
    }

    /**
     * Checks if adding another window would exceed layout capacity
     *
     * Voice: "Can I add another window?" → VoiceOS checks this
     */
    fun canAddWindow(currentCount: Int): Boolean {
        return currentCount < maxWindows
    }

    /**
     * Suggests optimal window count for this layout
     */
    fun getRecommendedWindowCount(): Int = 5

    /**
     * Returns voice description of layout state
     *
     * Example: "Linear layout with 5 windows in horizontal array"
     */
    fun getVoiceDescription(windowCount: Int): String {
        return when (windowCount) {
            1 -> "Linear layout with single centered window"
            2 -> "Linear layout with 2 windows side by side"
            in 3..4 -> "Linear layout with $windowCount windows in horizontal array"
            5 -> "Linear layout with 5 windows in optimal horizontal array"
            6 -> "Linear layout with 6 windows at maximum capacity"
            else -> "Linear layout"
        }
    }
}
