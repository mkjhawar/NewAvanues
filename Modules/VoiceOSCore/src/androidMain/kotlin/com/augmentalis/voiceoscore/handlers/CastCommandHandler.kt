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
            CommandActionType.CAST_START ->
                failure("Start casting requires an active RemoteCast session", recoverable = true)
            CommandActionType.CAST_STOP ->
                failure("No active cast session to stop", recoverable = false)
            CommandActionType.CAST_CONNECT ->
                failure("Cast connect requires RemoteCast module configuration", recoverable = true)
            CommandActionType.CAST_DISCONNECT ->
                failure("No active cast connection to disconnect", recoverable = false)
            CommandActionType.CAST_QUALITY ->
                failure("Cast quality change requires an active cast session", recoverable = true)
            else -> HandlerResult.notHandled()
        }
    }

    private fun failure(message: String, recoverable: Boolean): HandlerResult {
        Log.w(TAG, message)
        return HandlerResult.failure(message, recoverable = recoverable)
    }
}
