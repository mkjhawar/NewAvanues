package com.augmentalis.cockpit

/**
 * Constants for the Cockpit module.
 */
object CockpitConstants {
    /** Maximum number of frames per session */
    const val MAX_FRAMES_PER_SESSION = 20

    /** Maximum number of sessions */
    const val MAX_SESSIONS = 50

    /** Default frame width in dp */
    const val DEFAULT_FRAME_WIDTH = 400f

    /** Default frame height in dp */
    const val DEFAULT_FRAME_HEIGHT = 300f

    /** Minimum frame width in dp (can't resize smaller than this) */
    const val MIN_FRAME_WIDTH = 120f

    /** Minimum frame height in dp (can't resize smaller than this) */
    const val MIN_FRAME_HEIGHT = 80f

    /** Maximum frame width in dp */
    const val MAX_FRAME_WIDTH = 2000f

    /** Maximum frame height in dp */
    const val MAX_FRAME_HEIGHT = 2000f

    /** Title bar height in dp */
    const val TITLE_BAR_HEIGHT = 40f

    /** Minimized frame height (title bar only) */
    const val MINIMIZED_HEIGHT = TITLE_BAR_HEIGHT

    /** Snap distance in dp for magnetic edge snapping */
    const val SNAP_THRESHOLD = 12f

    /** Debounce delay (ms) for auto-saving frame state changes */
    const val AUTO_SAVE_DEBOUNCE_MS = 500L

    /** Name of the auto-created default session */
    const val DEFAULT_SESSION_NAME = "Quick View"

    /** Maximum workflow steps per session */
    const val MAX_WORKFLOW_STEPS = 50

    /** Maximum attachments per note */
    const val MAX_NOTE_ATTACHMENTS = 50

    // ── Spatial Grid ────────────────────────────────────────
    /** Default spatial grid dimensions (3x3 = center + 8 directions) */
    const val SPATIAL_GRID_COLUMNS = 3
    const val SPATIAL_GRID_ROWS = 3

    // Spatial sensitivity constants moved to SpatialSensitivity enum
    // in SpatialViewportController.kt (GLASS_LOW / NORMAL / HIGH presets)

    // ── Command Bar ────────────────────────────────────────
    /** Command bar height in dp (tablet/phone) */
    const val COMMAND_BAR_HEIGHT = 56f

    /** Command bar height in dp (glass displays) */
    const val COMMAND_BAR_HEIGHT_GLASS = 48f

    /** Max buttons shown on glass command bar */
    const val COMMAND_BAR_MAX_GLASS_BUTTONS = 5

    // ── Carousel ────────────────────────────────────────────
    /** Scale factor for adjacent carousel frames */
    const val CAROUSEL_ADJACENT_SCALE = 0.80f

    /** Y-axis rotation in degrees for adjacent carousel frames */
    const val CAROUSEL_ROTATION_DEGREES = 15f

    /** Alpha for adjacent carousel frames */
    const val CAROUSEL_ADJACENT_ALPHA = 0.6f

    // ── Dice-5 ──────────────────────────────────────────────
    /** Center frame weight in Dice-5 layout (fraction of total area) */
    const val DICE_CENTER_WEIGHT = 0.55f

    /** Corner frame weight in Dice-5 layout (fraction of total area) */
    const val DICE_CORNER_WEIGHT = 0.45f
}
