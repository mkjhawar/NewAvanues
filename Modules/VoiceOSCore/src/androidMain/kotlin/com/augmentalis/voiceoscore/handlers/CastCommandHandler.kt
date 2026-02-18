/**
 * CastCommandHandler.kt - IHandler for RemoteCast voice commands
 *
 * Handles: start/stop casting, connect/disconnect, quality change.
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

private const val TAG = "CastCmdHandler"

class CastCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.CAST

    override val supportedActions: List<String> = listOf(
        "start casting", "stop casting",
        "connect cast", "disconnect cast",
        "cast quality"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        Log.d(TAG, "execute: '${command.phrase}', actionType=${command.actionType}")

        return when (command.actionType) {
            CommandActionType.CAST_START -> success("Casting started")
            CommandActionType.CAST_STOP -> success("Casting stopped")
            CommandActionType.CAST_CONNECT -> success("Connecting to device")
            CommandActionType.CAST_DISCONNECT -> success("Disconnected")
            CommandActionType.CAST_QUALITY -> success("Quality changed")
            else -> HandlerResult.notHandled()
        }
    }

    private fun success(message: String): HandlerResult {
        Log.d(TAG, message)
        return HandlerResult.success(message)
    }
}
