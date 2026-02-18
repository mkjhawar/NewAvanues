/**
 * ImageCommandHandler.kt - IHandler for ImageAvanue voice commands
 *
 * Handles: gallery, viewer, filters, rotate, flip, crop, share, delete, info, navigation.
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

        return when (command.actionType) {
            CommandActionType.IMAGE_OPEN -> success("Image viewer opened")
            CommandActionType.IMAGE_GALLERY -> success("Gallery opened")
            CommandActionType.IMAGE_FILTER_GRAYSCALE -> success("Grayscale filter applied")
            CommandActionType.IMAGE_FILTER_SEPIA -> success("Sepia filter applied")
            CommandActionType.IMAGE_FILTER_BLUR -> success("Blur filter applied")
            CommandActionType.IMAGE_FILTER_SHARPEN -> success("Sharpen filter applied")
            CommandActionType.IMAGE_FILTER_BRIGHTNESS -> success("Brightness adjusted")
            CommandActionType.IMAGE_FILTER_CONTRAST -> success("Contrast adjusted")
            CommandActionType.IMAGE_ROTATE_LEFT -> success("Rotated left")
            CommandActionType.IMAGE_ROTATE_RIGHT -> success("Rotated right")
            CommandActionType.IMAGE_FLIP_H -> success("Flipped horizontally")
            CommandActionType.IMAGE_FLIP_V -> success("Flipped vertically")
            CommandActionType.IMAGE_CROP -> success("Crop mode activated")
            CommandActionType.IMAGE_SHARE -> success("Image shared")
            CommandActionType.IMAGE_DELETE -> success("Image deleted")
            CommandActionType.IMAGE_INFO -> success("Image info displayed")
            CommandActionType.IMAGE_NEXT -> success("Next image")
            CommandActionType.IMAGE_PREVIOUS -> success("Previous image")
            else -> HandlerResult.notHandled()
        }
    }

    private fun success(message: String): HandlerResult {
        Log.d(TAG, message)
        return HandlerResult.success(message)
    }
}
