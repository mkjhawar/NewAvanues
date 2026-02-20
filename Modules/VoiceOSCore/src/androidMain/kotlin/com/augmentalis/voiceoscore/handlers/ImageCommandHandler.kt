/**
 * ImageCommandHandler.kt - IHandler for ImageAvanue voice commands
 *
 * Handles: gallery, viewer, filters, rotate, flip, crop, share, delete, info, navigation.
 *
 * Dispatches to the active IImageController via
 * ModuleCommandCallbacks.imageExecutor. When ImageAvanue is not
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

private const val TAG = "ImageCmdHandler"
private const val MODULE_NAME = "ImageAvanue"

class ImageCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.IMAGE

    override val supportedActions: List<String> = listOf(
        "open image", "open gallery", "grayscale", "sepia",
        "blur image", "sharpen image", "adjust brightness", "adjust contrast",
        "rotate left", "rotate right", "flip horizontal", "flip vertical",
        "crop image", "share image", "delete image", "image info",
        "next image", "previous image"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        Log.d(TAG, "execute: '${command.phrase}', actionType=${command.actionType}")

        val executor = ModuleCommandCallbacks.imageExecutor
            ?: return moduleNotActive(command.actionType)

        return try {
            executor(command.actionType, extractMetadata(command))
        } catch (e: Exception) {
            Log.e(TAG, "Image command failed: ${command.actionType}", e)
            HandlerResult.failure("Image command failed: ${e.message}", recoverable = true)
        }
    }

    private fun moduleNotActive(action: CommandActionType): HandlerResult {
        Log.w(TAG, "$MODULE_NAME not active for: $action")
        return HandlerResult.failure(
            "$MODULE_NAME not active — open an image to use this command",
            recoverable = true
        )
    }

    private fun extractMetadata(command: QuantizedCommand): Map<String, String> =
        command.metadata.mapValues { it.value.toString() }
}
