/**
 * AndroidCursorHandler.kt - IHandler for cursor voice commands
 *
 * Routes "show cursor", "hide cursor", "cursor click" and related phrases
 * to the CursorOverlayService. Integrates with the ActionCoordinator
 * handler system (IHandler), replacing the legacy CursorCommandHandler.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.augmentalis.voicecursor.overlay.CursorOverlayService

private const val TAG = "AndroidCursorHandler"

/**
 * IHandler implementation for cursor voice commands.
 *
 * Handles show/hide (start/stop CursorOverlayService) and click
 * (dispatch click at current cursor position via CursorOverlayService).
 *
 * Registered in AndroidHandlerFactory alongside AndroidGestureHandler,
 * SystemHandler, and AppHandler.
 */
class AndroidCursorHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.GAZE

    override val supportedActions: List<String> = listOf(
        "show cursor", "cursor on", "enable cursor",
        "hide cursor", "cursor off", "disable cursor",
        "cursor click", "click here"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()

        return when {
            phrase in listOf("show cursor", "cursor on", "enable cursor") -> showCursor()
            phrase in listOf("hide cursor", "cursor off", "disable cursor") -> hideCursor()
            phrase in listOf("cursor click", "click here") -> cursorClick()
            else -> HandlerResult.notHandled()
        }
    }

    private fun showCursor(): HandlerResult {
        val context = service.applicationContext
        if (!Settings.canDrawOverlays(context)) {
            Log.w(TAG, "Cannot show cursor — overlay permission not granted")
            return HandlerResult.failure("Overlay permission not granted")
        }

        if (CursorOverlayService.getInstance() != null) {
            return HandlerResult.success("Cursor already visible")
        }

        return try {
            val intent = Intent(context, CursorOverlayService::class.java)
            context.startForegroundService(intent)
            Log.i(TAG, "CursorOverlayService started via voice command")
            HandlerResult.success("Cursor shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CursorOverlayService", e)
            HandlerResult.failure("Failed to show cursor: ${e.message}")
        }
    }

    private fun hideCursor(): HandlerResult {
        val context = service.applicationContext

        if (CursorOverlayService.getInstance() == null) {
            return HandlerResult.success("Cursor already hidden")
        }

        return try {
            context.stopService(Intent(context, CursorOverlayService::class.java))
            Log.i(TAG, "CursorOverlayService stopped via voice command")
            HandlerResult.success("Cursor hidden")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop CursorOverlayService", e)
            HandlerResult.failure("Failed to hide cursor: ${e.message}")
        }
    }

    private fun cursorClick(): HandlerResult {
        val instance = CursorOverlayService.getInstance()
            ?: return HandlerResult.failure("Cursor not active — say 'show cursor' first")

        return if (instance.performClickAtCurrentPosition()) {
            HandlerResult.success("Clicked at cursor position")
        } else {
            HandlerResult.failure("Cursor not visible or click dispatcher not set")
        }
    }
}
