package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Cross-frame linking — connects content between frames.
 *
 * Enables interactions like:
 * - Select text in PDF → auto-search in web frame
 * - Highlight text → copy to note frame
 * - Drag image from web frame → drop into note frame
 * - Click location in map → open streetview in web frame
 *
 * Part of the Modules/ContentLink reusable module.
 */
@Serializable
data class CrossFrameLink(
    /** Unique link identifier */
    val id: String,
    /** Source frame ID (where the action originates) */
    val sourceFrameId: String,
    /** Target frame ID (where the action is applied) */
    val targetFrameId: String,
    /** What triggers this link */
    val trigger: LinkTrigger,
    /** What happens in the target frame */
    val action: LinkAction,
    /** Whether this link is currently active */
    val isEnabled: Boolean = true,
)

/**
 * What triggers a cross-frame link.
 */
@Serializable
enum class LinkTrigger {
    /** User selects/highlights text */
    TEXT_SELECTION,
    /** User long-presses content */
    LONG_PRESS,
    /** User drags content toward another frame */
    DRAG_DROP,
    /** Content updates in source (e.g., new page loaded) */
    CONTENT_CHANGE,
    /** Manual "send to" action from quick actions bar */
    SEND_TO,
    /** Automatic — always synced */
    AUTO_SYNC,
}

/**
 * What happens in the target frame when a link fires.
 */
@Serializable
enum class LinkAction {
    /** Search the selected text in web frame */
    WEB_SEARCH,
    /** Append content to a note frame */
    APPEND_TO_NOTE,
    /** Insert content (image, text) at note cursor position */
    INSERT_INTO_NOTE,
    /** Navigate to URL in web frame */
    NAVIGATE_URL,
    /** Set map coordinates from parsed location */
    SET_MAP_LOCATION,
    /** Copy content to clipboard of target frame */
    COPY_CONTENT,
    /** Open file/document in target frame */
    OPEN_DOCUMENT,
    /** Take screenshot of source and insert into target */
    SCREENSHOT_TO_TARGET,
}
