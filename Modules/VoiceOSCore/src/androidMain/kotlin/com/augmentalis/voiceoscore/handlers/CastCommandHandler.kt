/**
 * CastCommandHandler.kt - IHandler for RemoteCast voice commands
 *
 * Handles: start/stop casting, connect/disconnect, quality change.
 *
 * Dispatches to the active ICastManager via
 * ModuleCommandCallbacks.castExecutor. When no cast session is
 * active, commands return failure with recovery guidance.
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
private const val MODULE_NAME = "RemoteCast"

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

        val executor = ModuleCommandCallbacks.castExecutor
            ?: return moduleNotActive(command.actionType)

        return try {
            executor(command.actionType, extractMetadata(command))
        } catch (e: Exception) {
            Log.e(TAG, "Cast command failed: ${command.actionType}", e)
            HandlerResult.failure("Cast command failed: ${e.message}", recoverable = true)
        }
    }

    private fun moduleNotActive(action: CommandActionType): HandlerResult {
        Log.w(TAG, "$MODULE_NAME not active for: $action")
        return HandlerResult.failure(
            "$MODULE_NAME not active — start a cast session to use this command",
            recoverable = true,
            suggestedAction = "Say 'open cockpit' then 'start casting'"
        )
    }

    private fun extractMetadata(command: QuantizedCommand): Map<String, String> =
        command.metadata.mapValues { it.value.toString() }
}
