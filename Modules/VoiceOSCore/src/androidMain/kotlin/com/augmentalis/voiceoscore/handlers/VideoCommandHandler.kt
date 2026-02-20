/**
 * VideoCommandHandler.kt - IHandler for VideoAvanue voice commands
 *
 * Handles: play, pause, stop, seek, speed, mute, loop, fullscreen.
 *
 * Dispatches to the active IVideoController via
 * ModuleCommandCallbacks.videoExecutor. When VideoAvanue is not
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

private const val TAG = "VideoCmdHandler"
private const val MODULE_NAME = "VideoAvanue"

class VideoCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.VIDEO

    override val supportedActions: List<String> = listOf(
        "play video", "pause video", "stop video",
        "skip forward", "skip backward",
        "speed up video", "slow down video", "normal speed",
        "fullscreen video", "mute video", "unmute video", "loop video"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        Log.d(TAG, "execute: '${command.phrase}', actionType=${command.actionType}")

        val executor = ModuleCommandCallbacks.videoExecutor
            ?: return moduleNotActive(command.actionType)

        return try {
            executor(command.actionType, extractMetadata(command))
        } catch (e: Exception) {
            Log.e(TAG, "Video command failed: ${command.actionType}", e)
            HandlerResult.failure("Video command failed: ${e.message}", recoverable = true)
        }
    }

    private fun moduleNotActive(action: CommandActionType): HandlerResult {
        Log.w(TAG, "$MODULE_NAME not active for: $action")
        return HandlerResult.failure(
            "$MODULE_NAME not active — open a video to use this command",
            recoverable = true
        )
    }

    private fun extractMetadata(command: QuantizedCommand): Map<String, String> =
        command.metadata.mapValues { it.value.toString() }
}
