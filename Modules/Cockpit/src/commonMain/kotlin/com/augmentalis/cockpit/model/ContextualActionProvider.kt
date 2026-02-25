package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * A group of related actions for the "More" panel.
 * Actions are grouped by category for easy scanning.
 */
@Serializable
data class ActionGroup(
    val category: String,
    val actions: List<QuickAction>,
)

/**
 * Provides context-aware actions for the simplified command bar.
 *
 * Replaces the 13-state [CommandBarState] hierarchy with a flat, single-level
 * action provider. Instead of navigating through MAIN → FRAME_ACTIONS → WEB_ACTIONS,
 * the user sees 5-6 top actions for the focused content type, with a "More" option
 * that opens a searchable bottom sheet with all actions grouped by category.
 *
 * Voice commands remain accessible regardless of what's visually shown — this only
 * affects the visual UI. All voice commands route through VoiceOSCore as before.
 *
 * Usage:
 * ```kotlin
 * val provider = ContextualActionProvider
 * val topActions = provider.topActionsForContent("note")  // 5-6 most-used
 * val allActions = provider.allActionsForContent("note")   // full grouped list
 * ```
 */
object ContextualActionProvider {

    /**
     * Top 5-6 most-used actions for a content type.
     * Shown directly in the flat action bar (no nesting).
     */
    fun topActionsForContent(contentTypeId: String): List<QuickAction> =
        when (contentTypeId) {
            FrameContent.TYPE_WEB -> listOf(
                action("web_back", "Back", "arrow_back"),
                action("web_forward", "Forward", "arrow_forward"),
                action("web_refresh", "Refresh", "refresh"),
                action("web_zoom_in", "Zoom In", "zoom_in"),
                action("web_zoom_out", "Zoom Out", "zoom_out"),
            )

            FrameContent.TYPE_PDF -> listOf(
                action("pdf_prev", "Prev Page", "navigate_before"),
                action("pdf_next", "Next Page", "navigate_next"),
                action("pdf_zoom_in", "Zoom In", "zoom_in"),
                action("pdf_zoom_out", "Zoom Out", "zoom_out"),
                action("pdf_search", "Search", "search"),
            )

            FrameContent.TYPE_IMAGE -> listOf(
                action("image_zoom_in", "Zoom In", "zoom_in"),
                action("image_zoom_out", "Zoom Out", "zoom_out"),
                action("image_rotate", "Rotate", "rotate_right"),
                action("image_share", "Share", "share"),
            )

            FrameContent.TYPE_VIDEO -> listOf(
                action("video_rewind", "Rewind", "replay_10"),
                action("video_play_pause", "Play/Pause", "play_arrow"),
                action("video_forward", "Forward", "forward_10"),
                action("video_fullscreen", "Fullscreen", "fullscreen"),
            )

            FrameContent.TYPE_NOTE, FrameContent.TYPE_VOICE_NOTE -> listOf(
                action("note_bold", "Bold", "format_bold"),
                action("note_italic", "Italic", "format_italic"),
                action("note_underline", "Underline", "format_underlined"),
                action("note_undo", "Undo", "undo"),
                action("note_redo", "Redo", "redo"),
                action("note_save", "Save", "save"),
            )

            FrameContent.TYPE_CAMERA -> listOf(
                action("camera_flip", "Flip", "flip_camera_android"),
                action("camera_capture", "Capture", "camera"),
                action("camera_flash", "Flash", "flash_on"),
            )

            FrameContent.TYPE_WHITEBOARD -> listOf(
                action("wb_pen", "Pen", "edit"),
                action("wb_highlight", "Highlight", "highlight"),
                action("wb_eraser", "Eraser", "auto_fix_normal"),
                action("wb_undo", "Undo", "undo"),
                action("wb_redo", "Redo", "redo"),
                action("wb_clear", "Clear", "delete"),
            )

            FrameContent.TYPE_TERMINAL -> listOf(
                action("term_clear", "Clear", "delete_sweep"),
                action("term_copy", "Copy", "content_copy"),
                action("term_scroll_top", "Top", "vertical_align_top"),
                action("term_scroll_bottom", "Bottom", "vertical_align_bottom"),
            )

            FrameContent.TYPE_MAP -> listOf(
                action("map_zoom_in", "Zoom In", "zoom_in"),
                action("map_zoom_out", "Zoom Out", "zoom_out"),
                action("map_center", "Center", "my_location"),
                action("map_layers", "Layers", "layers"),
            )

            else -> listOf(
                action("frame_minimize", "Minimize", "minimize"),
                action("frame_maximize", "Maximize", "open_in_full"),
                action("frame_close", "Close", "close"),
            )
        }

    /**
     * All actions for a content type, organized into groups.
     * Shown in the "More" bottom sheet, searchable and categorized.
     */
    fun allActionsForContent(contentTypeId: String): List<ActionGroup> {
        val contentActions = topActionsForContent(contentTypeId)
        val frameActions = listOf(
            action("frame_minimize", "Minimize", "minimize"),
            action("frame_maximize", "Maximize", "open_in_full"),
            action("frame_close", "Close", "close"),
            action("frame_fullscreen", "Fullscreen", "fullscreen"),
            action("frame_pin", "Pin", "push_pin"),
        )
        val universalActions = listOf(
            action("share", "Share", "share"),
            action("screenshot", "Screenshot", "screenshot"),
            action("clone", "Clone Frame", "content_copy"),
            action("send_to", "Send To", "send"),
            action("ai_summarize", "AI Summary", "auto_awesome"),
        )
        val layoutActions = ArrangementIntent.entries.map { intent ->
            action(
                id = "layout_${intent.name.lowercase()}",
                label = intent.displayLabel,
                iconName = intent.iconName,
            )
        }

        return listOfNotNull(
            ActionGroup(
                category = contentCategoryLabel(contentTypeId),
                actions = contentActions
            ),
            ActionGroup(category = "Frame", actions = frameActions),
            ActionGroup(category = "Layout", actions = layoutActions),
            ActionGroup(category = "Tools", actions = universalActions),
        )
    }

    /**
     * Search across all actions for a content type.
     * Used by Lens variation to filter actions by query.
     */
    fun searchActions(contentTypeId: String, query: String): List<QuickAction> {
        if (query.isBlank()) return topActionsForContent(contentTypeId)
        val lowerQuery = query.lowercase()
        return allActionsForContent(contentTypeId)
            .flatMap { it.actions }
            .filter { it.label.lowercase().contains(lowerQuery) || it.id.contains(lowerQuery) }
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private fun action(id: String, label: String, iconName: String) =
        QuickAction(id = id, label = label, iconName = iconName)

    private fun contentCategoryLabel(typeId: String): String = when (typeId) {
        FrameContent.TYPE_WEB -> "Web"
        FrameContent.TYPE_PDF -> "PDF"
        FrameContent.TYPE_IMAGE -> "Image"
        FrameContent.TYPE_VIDEO -> "Video"
        FrameContent.TYPE_NOTE -> "Note"
        FrameContent.TYPE_VOICE_NOTE -> "Voice Note"
        FrameContent.TYPE_CAMERA -> "Camera"
        FrameContent.TYPE_WHITEBOARD -> "Whiteboard"
        FrameContent.TYPE_TERMINAL -> "Terminal"
        FrameContent.TYPE_MAP -> "Map"
        FrameContent.TYPE_FORM -> "Form"
        FrameContent.TYPE_SIGNATURE -> "Signature"
        FrameContent.TYPE_AI_SUMMARY -> "AI Summary"
        FrameContent.TYPE_SCREEN_CAST -> "Cast"
        FrameContent.TYPE_WIDGET -> "Widget"
        FrameContent.TYPE_EXTERNAL_APP -> "External App"
        else -> "Actions"
    }
}
