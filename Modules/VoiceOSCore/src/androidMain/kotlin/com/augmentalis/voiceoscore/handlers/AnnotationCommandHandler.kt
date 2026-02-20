/**
 * AnnotationCommandHandler.kt - IHandler for AnnotationAvanue voice commands
 *
 * Handles: tool selection (pen, highlighter, shapes, eraser),
 * color picker, undo/redo, clear, save, share, pen size.
 *
 * Dispatches to the active IAnnotationController via
 * ModuleCommandCallbacks.annotationExecutor. When AnnotationAvanue
 * is not in the foreground, commands return failure.
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
private const val MODULE_NAME = "AnnotationAvanue"

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

        val executor = ModuleCommandCallbacks.annotationExecutor
            ?: return moduleNotActive(command.actionType)

        return try {
            executor(command.actionType, extractMetadata(command))
        } catch (e: Exception) {
            Log.e(TAG, "Annotation command failed: ${command.actionType}", e)
            HandlerResult.failure("Annotation command failed: ${e.message}", recoverable = true)
        }
    }

    private fun moduleNotActive(action: CommandActionType): HandlerResult {
        Log.w(TAG, "$MODULE_NAME not active for: $action")
        return HandlerResult.failure(
            "$MODULE_NAME not active — open an annotation to use this command",
            recoverable = true
        )
    }

    private fun extractMetadata(command: QuantizedCommand): Map<String, String> =
        command.metadata.mapValues { it.value.toString() }
}
