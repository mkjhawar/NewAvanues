/**
 * IosNoteCommandHandler.kt - iOS handler for NoteAvanue voice commands
 *
 * Handles: formatting (bold/italic/underline/strikethrough/headings/lists),
 * navigation (go to top/bottom, next/prev heading), editing (undo/redo/delete),
 * voice mode switching (dictation/command/continuous), and note lifecycle.
 *
 * This handler dispatches to the active INoteController instance via a
 * static holder. When NoteAvanue is not in the foreground, commands
 * return notHandled() to avoid side effects.
 *
 * No AccessibilityService needed — uses platform-agnostic controller interface.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import kotlin.concurrent.Volatile
import platform.Foundation.NSLog

private const val TAG = "IosNoteCommandHandler"

/**
 * Static holder for the active NoteAvanue controller on iOS.
 * Set by the NoteAvanue screen when it becomes active, cleared when dismissed.
 */
object IosNoteControllerHolder {
    /**
     * Callback to execute a note command by its CommandActionType.
     * Returns true if the command was handled successfully.
     */
    @Volatile
    var onNoteCommand: ((CommandActionType) -> Boolean)? = null
}

class IosNoteCommandHandler : BaseHandler() {

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
        NSLog("$TAG.execute: '$phrase', actionType=${command.actionType}")

        val handler = IosNoteControllerHolder.onNoteCommand
        if (handler == null) {
            NSLog("$TAG: NoteAvanue not active — returning notHandled")
            return HandlerResult.notHandled()
        }

        return when (command.actionType) {
            // Formatting
            CommandActionType.FORMAT_BOLD,
            CommandActionType.FORMAT_ITALIC,
            CommandActionType.FORMAT_UNDERLINE,
            CommandActionType.FORMAT_STRIKETHROUGH,
            CommandActionType.HEADING_1,
            CommandActionType.HEADING_2,
            CommandActionType.HEADING_3,
            CommandActionType.BULLET_LIST,
            CommandActionType.NUMBERED_LIST,
            CommandActionType.CHECKLIST,
            CommandActionType.CODE_BLOCK,
            CommandActionType.BLOCKQUOTE,
            CommandActionType.INSERT_DIVIDER,
            // Editing
            CommandActionType.NOTE_UNDO,
            CommandActionType.NOTE_REDO,
            CommandActionType.SELECT_ALL,
            CommandActionType.DELETE_LINE,
            CommandActionType.NEW_PARAGRAPH,
            // Navigation
            CommandActionType.GO_TO_TOP,
            CommandActionType.GO_TO_BOTTOM,
            CommandActionType.NEXT_HEADING,
            CommandActionType.PREVIOUS_HEADING,
            CommandActionType.SCROLL_UP,
            CommandActionType.SCROLL_DOWN,
            // Voice mode
            CommandActionType.DICTATION_MODE,
            CommandActionType.COMMAND_MODE,
            CommandActionType.CONTINUOUS_MODE,
            // Clipboard
            CommandActionType.COPY,
            CommandActionType.PASTE,
            CommandActionType.CUT,
            // Note actions
            CommandActionType.OPEN_MODULE,
            CommandActionType.NEW_NOTE,
            CommandActionType.SAVE_NOTE,
            CommandActionType.TOGGLE_PIN,
            CommandActionType.EXPORT_NOTE,
            CommandActionType.SEARCH_NOTES,
            CommandActionType.CLOSE_APP,
            // Attachments
            CommandActionType.CAPTURE_PHOTO,
            CommandActionType.ATTACH_FILE,
            CommandActionType.ATTACH_AUDIO,
            // Font
            CommandActionType.INCREASE_FONT,
            CommandActionType.DECREASE_FONT,
            // Misc
            CommandActionType.CLEAR_FORMATTING,
            CommandActionType.WORD_COUNT,
            CommandActionType.ZOOM_IN,
            CommandActionType.ZOOM_OUT,
            // TTS
            CommandActionType.READ_SCREEN,
            CommandActionType.STOP_READING -> {
                dispatch(handler, command.actionType)
            }
            else -> HandlerResult.notHandled()
        }
    }

    private fun dispatch(handler: (CommandActionType) -> Boolean, actionType: CommandActionType): HandlerResult {
        return if (handler.invoke(actionType)) {
            NSLog("$TAG: Note command dispatched: $actionType")
            HandlerResult.success("$actionType")
        } else {
            NSLog("$TAG: Note command not handled by controller: $actionType")
            HandlerResult.failure("Note command failed: $actionType", recoverable = true)
        }
    }
}
