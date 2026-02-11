/**
 * AppControlHandler.kt - IHandler for app lifecycle commands
 *
 * Handles: close app, exit app, quit
 * Uses AccessibilityService global actions.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

private const val TAG = "AppControlHandler"

class AppControlHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.APP

    override val supportedActions: List<String> = listOf(
        "close app", "close this", "exit app", "quit"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "AppControlHandler.execute: '$phrase'")

        return when {
            phrase in listOf("close app", "close this", "exit app", "quit") -> closeApp()
            else -> HandlerResult.notHandled()
        }
    }

    private fun closeApp(): HandlerResult {
        // Use BACK action twice to close most apps, then HOME to return to launcher
        return try {
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            // Small delay then go home to ensure app is closed
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }, 300)
            HandlerResult.success("App closed")
        } catch (e: Exception) {
            Log.e(TAG, "Close app failed", e)
            HandlerResult.failure("Failed to close app: ${e.message}")
        }
    }
}
