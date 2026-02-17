package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Quick action — contextual action available via swipe-up gesture on a frame.
 *
 * Actions are context-sensitive: the available actions depend on the frame's
 * content type and current state.
 */
@Serializable
data class QuickAction(
    /** Action identifier */
    val id: String,
    /** Display label */
    val label: String,
    /** Material icon name */
    val iconName: String,
    /** Which content types this action applies to (empty = all) */
    val applicableTypes: List<String> = emptyList(),
)

/**
 * Built-in quick actions available for all frames.
 */
object QuickActions {
    /** Send frame content to another frame (opens target picker) */
    val SEND_TO = QuickAction("send_to", "Send To", "send")

    /** Take a screenshot of this frame */
    val SCREENSHOT = QuickAction("screenshot", "Screenshot", "screenshot")

    /** Clone this frame into a new frame */
    val CLONE = QuickAction("clone", "Clone Frame", "content_copy")

    /** Share frame content externally */
    val SHARE = QuickAction("share", "Share", "share")

    /** Pin frame as PiP overlay */
    val PIN = QuickAction("pin", "Pin Frame", "push_pin")

    /** Move frame to different session */
    val MOVE_TO_SESSION = QuickAction("move_session", "Move to Session", "drive_file_move")

    /** Open AI summarizer for this frame */
    val AI_SUMMARIZE = QuickAction("ai_summarize", "AI Summary", "auto_awesome")

    /** Full-screen this frame */
    val FULLSCREEN = QuickAction("fullscreen", "Fullscreen", "fullscreen")

    /** Web-specific: open in external browser */
    val OPEN_EXTERNAL = QuickAction(
        "open_external", "Open in Browser", "open_in_new",
        applicableTypes = listOf(FrameContent.TYPE_WEB),
    )

    /** PDF-specific: go to page */
    val GOTO_PAGE = QuickAction(
        "goto_page", "Go to Page", "format_list_numbered",
        applicableTypes = listOf(FrameContent.TYPE_PDF),
    )

    /** Note-specific: insert photo */
    val INSERT_PHOTO = QuickAction(
        "insert_photo", "Insert Photo", "add_a_photo",
        applicableTypes = listOf(FrameContent.TYPE_NOTE),
    )

    /** All universal actions */
    val UNIVERSAL = listOf(SEND_TO, SCREENSHOT, CLONE, SHARE, PIN, MOVE_TO_SESSION, AI_SUMMARIZE, FULLSCREEN)
}
