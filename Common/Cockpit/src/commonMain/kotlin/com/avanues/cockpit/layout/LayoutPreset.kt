package com.avanues.cockpit.layout

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D

/**
 * Layout Preset Interface
 *
 * Defines how windows are spatially arranged in a workspace.
 * Each preset implements a different arrangement pattern (linear, arc, grid, etc.).
 *
 * **Voice Commands:**
 * - "Linear mode" → LINEAR_HORIZONTAL preset
 * - "Arc mode" → ARC_3_FRONT preset
 * - "Grid mode" → GRID_2x2 preset
 *
 * @see LinearHorizontalLayout
 */
interface LayoutPreset {
    /**
     * Unique identifier for this preset
     * Used in Workspace.layoutPresetId
     */
    val id: String

    /**
     * Voice command to activate this preset
     * Example: "Linear mode", "Arc mode"
     */
    val voiceCommand: String

    /**
     * Maximum number of windows supported by this layout
     */
    val maxWindows: Int

    /**
     * Minimum number of windows required for this layout
     */
    val minWindows: Int get() = 1

    /**
     * Human-readable description
     */
    val description: String

    /**
     * Calculates positions for all windows
     *
     * @param windows List of windows to position
     * @param centerPoint 3D center point for the layout
     * @return List of window positions with rotations
     */
    fun calculatePositions(
        windows: List<AppWindow>,
        centerPoint: Vector3D = Vector3D.DEFAULT
    ): List<WindowPosition>

    /**
     * Calculates dimensions for a specific window
     *
     * @param window Window to calculate dimensions for
     * @param index Window index in the list
     * @param totalWindows Total number of windows
     * @return Window dimensions in meters
     */
    fun calculateDimensions(
        window: AppWindow,
        index: Int,
        totalWindows: Int
    ): WindowDimensions {
        // Default implementation: use window's current dimensions
        return WindowDimensions(
            widthMeters = window.widthMeters,
            heightMeters = window.heightMeters
        )
    }

    /**
     * Checks if this layout can accommodate the given number of windows
     */
    fun canAccommodate(windowCount: Int): Boolean {
        return windowCount in minWindows..maxWindows
    }
}

/**
 * Window Position with Rotation
 *
 * Represents where a window should be positioned and how it should be rotated.
 *
 * @property windowId Window identifier
 * @property position 3D position in space
 * @property rotationX Rotation around X axis (degrees)
 * @property rotationY Rotation around Y axis (degrees)
 * @property rotationZ Rotation around Z axis (degrees)
 */
data class WindowPosition(
    val windowId: String,
    val position: Vector3D,
    val rotationX: Float = 0f,
    val rotationY: Float = 0f,
    val rotationZ: Float = 0f
)

/**
 * Window Dimensions
 *
 * @property widthMeters Width in meters at viewing distance
 * @property heightMeters Height in meters at viewing distance
 */
data class WindowDimensions(
    val widthMeters: Float,
    val heightMeters: Float
)
