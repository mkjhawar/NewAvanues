package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Layout modes for arranging frames within a Cockpit session.
 *
 * Each mode defines how frames are positioned and sized:
 * - FREEFORM: User drags windows anywhere, resizes freely (default)
 * - GRID: Auto-arranged in a grid (2x2, 2x3, 3x3 etc. based on frame count)
 * - SPLIT_LEFT: One large frame on left, smaller frames stacked on right
 * - SPLIT_RIGHT: One large frame on right, smaller frames stacked on left
 * - COCKPIT: Horizontal pager with swipe navigation (AR glasses optimized)
 * - FULLSCREEN: Single selected frame fills the entire display
 * - WORKFLOW: Vertical numbered step list linked to frames
 * - ROW: Horizontal scrollable strip of equal-width frames
 *
 * Future spatial modes (Phase 6):
 * - SPATIAL_ARC: Curved arc arrangement in 3D space
 * - SPATIAL_THEATER: Tiered curved arrangement
 * - SPATIAL_CYLINDER: 360-degree cylindrical wrap
 */
@Serializable
enum class LayoutMode {
    FREEFORM,
    GRID,
    SPLIT_LEFT,
    SPLIT_RIGHT,
    COCKPIT,
    FULLSCREEN,
    WORKFLOW,
    ROW;

    companion object {
        val DEFAULT = FREEFORM

        fun fromString(value: String): LayoutMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
        }
    }
}
