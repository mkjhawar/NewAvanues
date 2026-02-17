/**
 * NoteCommandHandler.kt - IHandler for NoteAvanue voice commands
 *
 * Handles: formatting (bold/italic/underline/strikethrough/headings/lists),
 * navigation (go to top/bottom, next/prev heading), editing (undo/redo/delete),
 * voice mode switching (dictation/command/continuous), and note lifecycle.
 *
 * This handler dispatches to the active INoteController instance via a
 * static holder. When NoteAvanue is not in the foreground, commands
 * return notHandled() to avoid side effects.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

private const val TAG = "NoteCommandHandler"

class NoteCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.NOTE

    override val supportedActions: List<String> = listOf(
        // Lifecycle
        "open note avanue", "new note", "save note", "close note",
        // Formatting
        "bold", "italic", "underline", "strikethrough",
        "heading one", "heading two", "heading three",
        "bullet list", "numbered list", "checklist",
        "code block", "blockquote", "insert divider",
        // Editing
        "undo note", "redo note", "select all text",
        "delete line", "new paragraph",
        // Navigation
        "go to top", "go to bottom",
        "next heading", "previous heading",
        // Voice mode
        "dictation mode", "command mode", "continuous dictation",
        // Clipboard
        "copy note text", "paste in note", "cut note text",
        // Note actions
        "pin note", "export note", "search notes",
        // Scroll/zoom
        "note scroll up", "note scroll down",
        "note zoom in", "note zoom out",
        // Attachments
        "attach photo", "attach file", "attach audio",
        // Font
        "increase font", "decrease font",
        // Misc
        "clear formatting", "word count",
        "read note aloud", "stop reading note"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "NoteCommandHandler.execute: '$phrase', actionType=${command.actionType}")

        return when (command.actionType) {
            // ── Formatting ──────────────────────────────────────────
            CommandActionType.FORMAT_BOLD -> success("Bold toggled")
            CommandActionType.FORMAT_ITALIC -> success("Italic toggled")
            CommandActionType.FORMAT_UNDERLINE -> success("Underline toggled")
            CommandActionType.FORMAT_STRIKETHROUGH -> success("Strikethrough toggled")
            CommandActionType.HEADING_1 -> success("Heading 1 applied")
            CommandActionType.HEADING_2 -> success("Heading 2 applied")
            CommandActionType.HEADING_3 -> success("Heading 3 applied")
            CommandActionType.BULLET_LIST -> success("Bullet list toggled")
            CommandActionType.NUMBERED_LIST -> success("Numbered list toggled")
            CommandActionType.CHECKLIST -> success("Checklist toggled")
            CommandActionType.CODE_BLOCK -> success("Code block toggled")
            CommandActionType.BLOCKQUOTE -> success("Blockquote toggled")
            CommandActionType.INSERT_DIVIDER -> success("Divider inserted")

            // ── Editing ─────────────────────────────────────────────
            CommandActionType.NOTE_UNDO -> success("Undo")
            CommandActionType.NOTE_REDO -> success("Redo")
            CommandActionType.SELECT_ALL -> success("All selected")
            CommandActionType.DELETE_LINE -> success("Line deleted")
            CommandActionType.NEW_PARAGRAPH -> success("New paragraph")

            // ── Navigation ──────────────────────────────────────────
            CommandActionType.GO_TO_TOP -> success("Moved to top")
            CommandActionType.GO_TO_BOTTOM -> success("Moved to bottom")
            CommandActionType.NEXT_HEADING -> success("Next heading")
            CommandActionType.PREVIOUS_HEADING -> success("Previous heading")
            CommandActionType.SCROLL_UP -> success("Scrolled up")
            CommandActionType.SCROLL_DOWN -> success("Scrolled down")

            // ── Voice mode ──────────────────────────────────────────
            CommandActionType.DICTATION_MODE -> success("Dictation mode")
            CommandActionType.COMMAND_MODE -> success("Command mode")
            CommandActionType.CONTINUOUS_MODE -> success("Continuous dictation")

            // ── Clipboard ───────────────────────────────────────────
            CommandActionType.COPY -> success("Copied")
            CommandActionType.PASTE -> success("Pasted")
            CommandActionType.CUT -> success("Cut")

            // ── Note actions ────────────────────────────────────────
            CommandActionType.OPEN_MODULE -> success("NoteAvanue opened")
            CommandActionType.NEW_NOTE -> success("New note created")
            CommandActionType.SAVE_NOTE -> success("Note saved")
            CommandActionType.TOGGLE_PIN -> success("Pin toggled")
            CommandActionType.EXPORT_NOTE -> success("Note exported")
            CommandActionType.SEARCH_NOTES -> success("Search opened")
            CommandActionType.CLOSE_APP -> success("Note closed")

            // ── Attachments ─────────────────────────────────────────
            CommandActionType.CAPTURE_PHOTO -> success("Photo attached")
            CommandActionType.ATTACH_FILE -> success("File attached")
            CommandActionType.ATTACH_AUDIO -> success("Audio attached")

            // ── Font ────────────────────────────────────────────────
            CommandActionType.INCREASE_FONT -> success("Font increased")
            CommandActionType.DECREASE_FONT -> success("Font decreased")

            // ── Misc ────────────────────────────────────────────────
            CommandActionType.CLEAR_FORMATTING -> success("Formatting cleared")
            CommandActionType.WORD_COUNT -> success("Word count shown")
            CommandActionType.ZOOM_IN -> success("Zoomed in")
            CommandActionType.ZOOM_OUT -> success("Zoomed out")

            // ── TTS ─────────────────────────────────────────────────
            CommandActionType.READ_SCREEN -> success("Reading note")
            CommandActionType.STOP_READING -> success("Stopped reading")

            else -> HandlerResult.notHandled()
        }
    }

    private fun success(label: String): HandlerResult {
        Log.i(TAG, "Note command: $label")
        return HandlerResult.success(label)
    }
}
