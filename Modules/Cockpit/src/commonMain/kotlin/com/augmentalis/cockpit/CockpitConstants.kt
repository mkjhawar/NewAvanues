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
}
