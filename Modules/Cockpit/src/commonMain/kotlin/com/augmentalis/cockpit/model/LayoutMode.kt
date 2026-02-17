package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Layout modes for arranging frames within a Cockpit session.
 *
 * Each mode defines how frames are positioned and sized:
 * - FREEFORM: User drags windows anywhere, resizes freely
 * - GRID: Auto-arranged in a uniform grid (2x2, 2x3, etc.)
 * - SPLIT_LEFT: One large frame on left, smaller frames stacked on right
 * - SPLIT_RIGHT: One large frame on right, smaller frames stacked on left
 * - COCKPIT: Flight Deck — fixed 6-slot instrument panel (DEFAULT)
 * - T_PANEL: Primary frame 60% top, secondaries in bottom row
 * - MOSAIC: Primary frame 50% area, remaining frames tile around it
 * - FULLSCREEN: Single selected frame fills the entire display
 * - WORKFLOW: Vertical numbered step list linked to frames
 * - ROW: Horizontal scrollable strip of equal-width frames
 * - CAROUSEL: Curved 3D swipe-through with perspective scaling
 * - SPATIAL_DICE: 4 corners + 1 center (dice-5 pattern)
 * - GALLERY: Media-only filtered grid (image, video, camera, screen cast)
 */
@Serializable
enum class LayoutMode {
    FREEFORM,
    GRID,
    SPLIT_LEFT,
    SPLIT_RIGHT,
    COCKPIT,
    T_PANEL,
    MOSAIC,
    FULLSCREEN,
    WORKFLOW,
    ROW,
    CAROUSEL,
    SPATIAL_DICE,
    GALLERY;

    companion object {
        val DEFAULT = COCKPIT

        fun fromString(value: String): LayoutMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: DEFAULT
        }

        /** Layouts that support spatial canvas overlay (head-tracking viewport) */
        val SPATIAL_CAPABLE = setOf(FREEFORM, COCKPIT, MOSAIC, T_PANEL, SPATIAL_DICE)

        /** Content type IDs eligible for gallery filtering */
        val GALLERY_CONTENT_TYPES = setOf("image", "video", "camera", "screen_cast")
    }
}
