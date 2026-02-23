/**
 * CameraCommandHandler.kt - IHandler for PhotoAvanue voice commands
 *
 * Handles: capture photo, record/stop/pause/resume video,
 * flash mode cycling, lens switch, zoom, exposure, capture mode,
 * extensions (bokeh, HDR, night, retouch), pro mode controls
 * (ISO, focus, white balance, RAW).
 *
 * Dispatches to the active ICameraController via
 * ModuleCommandCallbacks.cameraExecutor. When PhotoAvanue is not
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

private const val TAG = "CameraCmdHandler"
private const val MODULE_NAME = "PhotoAvanue"

class CameraCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.CAMERA

    override val supportedActions: List<String> = listOf(
        "take photo", "capture photo", "take picture",
        "start recording", "stop recording", "pause recording", "resume recording",
        "switch camera", "switch lens", "flip camera",
        "flash on", "flash off", "flash auto", "torch",
        "zoom in camera", "zoom out camera",
        "exposure up", "exposure down",
        "photo mode", "video mode",
        "bokeh mode", "portrait mode", "hdr mode", "night mode",
        "retouch mode", "extensions off",
        "pro mode on", "pro mode off",
        "iso up", "iso down", "focus near", "focus far",
        "white balance auto", "white balance daylight", "white balance cloudy",
        "raw on", "raw off"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        Log.d(TAG, "execute: '${command.phrase}', actionType=${command.actionType}")

        val executor = ModuleCommandCallbacks.cameraExecutor
            ?: return moduleNotActive(command.actionType)

        return try {
            executor(command.actionType, extractMetadata(command))
        } catch (e: Exception) {
            Log.e(TAG, "Camera command failed: ${command.actionType}", e)
            HandlerResult.failure("Camera command failed: ${e.message}", recoverable = true)
        }
    }

    private fun moduleNotActive(action: CommandActionType): HandlerResult {
        Log.w(TAG, "$MODULE_NAME not active for: $action")
        return HandlerResult.failure(
            "$MODULE_NAME not active — open the camera to use this command",
            recoverable = true
        )
    }

    private fun extractMetadata(command: QuantizedCommand): Map<String, String> =
        command.metadata.mapValues { it.value.toString() }
}
