/**
 * NoteCommandHandler.kt - IHandler for NoteAvanue voice commands
 *
 * Handles: formatting (bold/italic/underline/strikethrough/headings/lists),
 * navigation (go to top/bottom, next/prev heading), editing (undo/redo/delete),
 * voice mode switching (dictation/command/continuous), and note lifecycle.
 *
 * Dispatches to the active INoteController via ModuleCommandCallbacks.noteExecutor.
 * When NoteAvanue is not in the foreground, commands return failure with
 * "NoteAvanue not active" to provide honest feedback.
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
private const val MODULE_NAME = "NoteAvanue"

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
        Log.d(TAG, "NoteCommandHandler.execute: '${command.phrase}', actionType=${command.actionType}")

        val executor = ModuleCommandCallbacks.noteExecutor
            ?: return moduleNotActive(command.actionType)

        return try {
            executor(command.actionType, extractMetadata(command))
        } catch (e: Exception) {
            Log.e(TAG, "Note command failed: ${command.actionType}", e)
            HandlerResult.failure("Note command failed: ${e.message}", recoverable = true)
        }
    }

    private fun moduleNotActive(action: CommandActionType): HandlerResult {
        Log.w(TAG, "$MODULE_NAME not active for: $action")
        return HandlerResult.failure(
            "$MODULE_NAME not active — open a note to use this command",
            recoverable = true,
            suggestedAction = "Say 'open note avanue' first"
        )
    }

    private fun extractMetadata(command: QuantizedCommand): Map<String, String> =
        command.metadata.mapValues { it.value.toString() }
}
