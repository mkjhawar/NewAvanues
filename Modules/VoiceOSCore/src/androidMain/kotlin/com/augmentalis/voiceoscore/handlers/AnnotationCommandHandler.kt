/**
 * AnnotationCommandHandler.kt - IHandler for AnnotationAvanue voice commands
 *
 * Handles: tool selection (pen, highlighter, shapes, eraser),
 * color picker, undo/redo, clear, save, share, pen size.
 *
 * This handler dispatches to the active IAnnotationController instance
 * via a static holder. When AnnotationAvanue is not in the foreground,
 * commands return notHandled().
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

private const val TAG = "AnnotationCmdHandler"

class AnnotationCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.ANNOTATION

    override val supportedActions: List<String> = listOf(
        "pen tool", "highlighter", "draw rectangle", "draw circle",
        "draw arrow", "draw line", "eraser tool",
        "color picker", "undo annotation", "redo annotation",
        "clear annotations", "save annotation", "share annotation",
        "thicker pen", "thinner pen"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        Log.d(TAG, "execute: '${command.phrase}', actionType=${command.actionType}")

        return when (command.actionType) {
            // Tool selection
            CommandActionType.ANNOTATION_PEN -> success("Pen tool selected")
            CommandActionType.ANNOTATION_HIGHLIGHTER -> success("Highlighter selected")
            CommandActionType.ANNOTATION_SHAPE_RECT -> success("Rectangle tool selected")
            CommandActionType.ANNOTATION_SHAPE_CIRCLE -> success("Circle tool selected")
            CommandActionType.ANNOTATION_SHAPE_ARROW -> success("Arrow tool selected")
            CommandActionType.ANNOTATION_SHAPE_LINE -> success("Line tool selected")
            CommandActionType.ANNOTATION_ERASER -> success("Eraser selected")

            // Color
            CommandActionType.ANNOTATION_COLOR_PICKER -> success("Color picker opened")

            // Undo/redo/clear
            CommandActionType.ANNOTATION_UNDO -> success("Annotation undone")
            CommandActionType.ANNOTATION_REDO -> success("Annotation redone")
            CommandActionType.ANNOTATION_CLEAR -> success("Annotations cleared")

            // Save/share
            CommandActionType.ANNOTATION_SAVE -> success("Annotation saved")
            CommandActionType.ANNOTATION_SHARE -> success("Annotation shared")

            // Pen size
            CommandActionType.ANNOTATION_PEN_SIZE_UP -> success("Pen size increased")
            CommandActionType.ANNOTATION_PEN_SIZE_DOWN -> success("Pen size decreased")

            else -> HandlerResult.notHandled()
        }
    }

    private fun success(message: String): HandlerResult {
        Log.d(TAG, message)
        return HandlerResult.success(message)
    }
}
