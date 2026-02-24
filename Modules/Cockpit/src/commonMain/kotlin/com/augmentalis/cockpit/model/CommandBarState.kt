package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Hierarchical state machine for the context-aware command bar.
 *
 * The command bar shows different action sets depending on the current state:
 * - **MAIN**: Root level — Add Frame, Layout Picker, Frame Actions
 * - **Level 1**: Category menus — content lists, layout grid, frame operations
 * - **Level 2**: Content-specific actions — web nav, PDF zoom, image tools, etc.
 *
 * Navigation: Each state has a [parent] for back-button behavior.
 * Content-specific states are auto-selected when a frame gains focus.
 */
@Serializable
enum class CommandBarState {
    // Level 0: Root
    MAIN,

    // Level 1: Category menus
    ADD_FRAME,
    LAYOUT_PICKER,
    FRAME_ACTIONS,

    // Level 2: Content-specific actions
    WEB_ACTIONS,
    PDF_ACTIONS,
    IMAGE_ACTIONS,
    VIDEO_ACTIONS,
    NOTE_ACTIONS,
    CAMERA_ACTIONS,
    WHITEBOARD_ACTIONS,

    // Level 2: Navigation
    SCROLL_COMMANDS,
    ZOOM_COMMANDS,
    SPATIAL_COMMANDS;

    /** Parent state for back navigation (null = root) */
    val parent: CommandBarState?
        get() = when (this) {
            MAIN -> null
            ADD_FRAME, LAYOUT_PICKER, FRAME_ACTIONS -> MAIN
            WEB_ACTIONS, PDF_ACTIONS, IMAGE_ACTIONS,
            VIDEO_ACTIONS, NOTE_ACTIONS, CAMERA_ACTIONS,
            WHITEBOARD_ACTIONS -> FRAME_ACTIONS
            SCROLL_COMMANDS, ZOOM_COMMANDS, SPATIAL_COMMANDS -> MAIN
        }

    /** Whether this state shows content-specific actions */
    val isContentSpecific: Boolean
        get() = this in CONTENT_SPECIFIC_STATES

    /** Nesting depth from MAIN (0 = root, 1 = category, 2 = content-specific) */
    val depth: Int
        get() = when (this) {
            MAIN -> 0
            ADD_FRAME, LAYOUT_PICKER, FRAME_ACTIONS,
            SCROLL_COMMANDS, ZOOM_COMMANDS, SPATIAL_COMMANDS -> 1
            else -> 2
        }

    companion object {
        private val CONTENT_SPECIFIC_STATES = setOf(
            WEB_ACTIONS, PDF_ACTIONS, IMAGE_ACTIONS,
            VIDEO_ACTIONS, NOTE_ACTIONS, CAMERA_ACTIONS,
            WHITEBOARD_ACTIONS
        )

        /** Map content type ID to the appropriate command bar state.
         *  Each content type routes to its dedicated action state. The actions
         *  are dispatched via ModuleCommandCallbacks executors in ContentRenderer. */
        fun forContentType(typeId: String): CommandBarState = when (typeId) {
            "web" -> WEB_ACTIONS
            "pdf" -> PDF_ACTIONS
            "image" -> IMAGE_ACTIONS
            "video" -> VIDEO_ACTIONS
            "note", "voice_note" -> NOTE_ACTIONS
            "camera" -> CAMERA_ACTIONS
            "whiteboard" -> WHITEBOARD_ACTIONS
            else -> FRAME_ACTIONS
        }
    }
}
