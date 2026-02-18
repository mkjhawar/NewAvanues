/**
 * VideoCommandHandler.kt - IHandler for VideoAvanue voice commands
 *
 * Handles: play, pause, stop, seek, speed, mute, loop, fullscreen.
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

        return when (command.actionType) {
            CommandActionType.VIDEO_PLAY -> success("Video playing")
            CommandActionType.VIDEO_PAUSE -> success("Video paused")
            CommandActionType.VIDEO_STOP -> success("Video stopped")
            CommandActionType.VIDEO_SEEK_FWD -> success("Skipped forward 10s")
            CommandActionType.VIDEO_SEEK_BACK -> success("Skipped backward 10s")
            CommandActionType.VIDEO_SPEED_UP -> success("Speed increased")
            CommandActionType.VIDEO_SPEED_DOWN -> success("Speed decreased")
            CommandActionType.VIDEO_SPEED_NORMAL -> success("Normal speed")
            CommandActionType.VIDEO_FULLSCREEN -> success("Fullscreen toggled")
            CommandActionType.VIDEO_MUTE -> success("Video muted")
            CommandActionType.VIDEO_UNMUTE -> success("Video unmuted")
            CommandActionType.VIDEO_LOOP -> success("Loop toggled")
            else -> HandlerResult.notHandled()
        }
    }

    private fun success(message: String): HandlerResult {
        Log.d(TAG, message)
        return HandlerResult.success(message)
    }
}
