/**
 * PdfCommandHandler.kt - IHandler for PDFAvanue voice commands
 *
 * Handles: page navigation (next, previous, first, last, go to),
 * zoom (in, out, fit page).
 *
 * Dispatches to the active PDF viewer via
 * ModuleCommandCallbacks.pdfExecutor. When PDFAvanue is not
 * in the foreground, commands return failure.
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

private const val TAG = "PdfCmdHandler"
private const val MODULE_NAME = "PDFAvanue"

class PdfCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.PDF

    override val supportedActions: List<String> = listOf(
        "next page", "previous page",
        "first page", "last page",
        "go to page", "page number",
        "zoom in pdf", "zoom out pdf",
        "fit page", "reset zoom"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        Log.d(TAG, "execute: '${command.phrase}', actionType=${command.actionType}")

        val executor = ModuleCommandCallbacks.pdfExecutor
            ?: return moduleNotActive(command.actionType)

        return try {
            executor(command.actionType, extractMetadata(command))
        } catch (e: Exception) {
            Log.e(TAG, "PDF command failed: ${command.actionType}", e)
            HandlerResult.failure("PDF command failed: ${e.message}", recoverable = true)
        }
    }

    private fun moduleNotActive(action: CommandActionType): HandlerResult {
        Log.w(TAG, "$MODULE_NAME not active for: $action")
        return HandlerResult.failure(
            "$MODULE_NAME not active — open a PDF to use this command",
            recoverable = true
        )
    }

    private fun extractMetadata(command: QuantizedCommand): Map<String, String> =
        command.metadata.mapValues { it.value.toString() }
}
