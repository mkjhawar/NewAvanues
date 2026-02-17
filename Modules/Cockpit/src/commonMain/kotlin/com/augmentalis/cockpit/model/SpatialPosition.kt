package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Grid position for a frame in the spatial canvas.
 *
 * The spatial canvas is a 3x3 virtual grid centered at (0,0). Frames can be
 * "locked" to a position by setting their spatialPosition. The viewport pans
 * via head-tracking or touch gestures to reveal off-center frames.
 *
 * Grid coordinates:
 * ```
 *  (-1,-1)  (0,-1)  (1,-1)
 *  (-1, 0)  (0, 0)  (1, 0)   ← center row
 *  (-1, 1)  (0, 1)  (1, 1)
 * ```
 */
@Serializable
data class SpatialPosition(
    /** Horizontal grid coordinate: -1 (left), 0 (center), 1 (right) */
    val gridX: Int = 0,
    /** Vertical grid coordinate: -1 (up), 0 (center), 1 (down) */
    val gridY: Int = 0
) {
    /** Whether this position is the center (default, always visible) */
    val isCenter: Boolean get() = gridX == 0 && gridY == 0

    /** Human-readable label for this grid position */
    val label: String get() = when {
        gridX == 0 && gridY == 0 -> "Center"
        gridX == -1 && gridY == 0 -> "Left"
        gridX == 1 && gridY == 0 -> "Right"
        gridX == 0 && gridY == -1 -> "Above"
        gridX == 0 && gridY == 1 -> "Below"
        gridX == -1 && gridY == -1 -> "Top-Left"
        gridX == 1 && gridY == -1 -> "Top-Right"
        gridX == -1 && gridY == 1 -> "Bottom-Left"
        gridX == 1 && gridY == 1 -> "Bottom-Right"
        else -> "($gridX,$gridY)"
    }

    companion object {
        val CENTER = SpatialPosition(0, 0)
        val LEFT = SpatialPosition(-1, 0)
        val RIGHT = SpatialPosition(1, 0)
        val ABOVE = SpatialPosition(0, -1)
        val BELOW = SpatialPosition(0, 1)
        val TOP_LEFT = SpatialPosition(-1, -1)
        val TOP_RIGHT = SpatialPosition(1, -1)
        val BOTTOM_LEFT = SpatialPosition(-1, 1)
        val BOTTOM_RIGHT = SpatialPosition(1, 1)

        /** All 9 grid positions in reading order (top-left to bottom-right) */
        val ALL = listOf(
            TOP_LEFT, ABOVE, TOP_RIGHT,
            LEFT, CENTER, RIGHT,
            BOTTOM_LEFT, BELOW, BOTTOM_RIGHT
        )
    }
}
