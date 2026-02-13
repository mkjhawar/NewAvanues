/**
 * AndroidCursorHandler.kt - IHandler for cursor voice commands
 *
 * Routes "show cursor", "hide cursor", "cursor click" and related phrases
 * to the CursorOverlayService. Integrates with the ActionCoordinator
 * handler system (IHandler), replacing the legacy CursorCommandHandler.
 *
 * After starting the overlay service, wires:
 * - CursorActions to the service's CursorController (enables voice movement)
 * - AndroidGestureDispatcher for click/scroll via voice
 * - ClickDispatcher for dwell-click dispatch
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.augmentalis.voicecursor.core.ClickDispatcher
import com.augmentalis.voicecursor.overlay.CursorOverlayService
import com.augmentalis.voiceoscore.managers.commandmanager.actions.CursorActions

private const val TAG = "AndroidCursorHandler"
private const val WIRE_RETRY_DELAY_MS = 100L
private const val WIRE_MAX_RETRIES = 10

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

    private val mainHandler = Handler(Looper.getMainLooper())

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

            // Wire dependencies after service initializes on main thread
            wireServiceDependencies(retryCount = 0)

            HandlerResult.success("Cursor shown")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CursorOverlayService", e)
            HandlerResult.failure("Failed to show cursor: ${e.message}")
        }
    }

    /**
     * Wire CursorActions and ClickDispatcher to the overlay service's controller.
     *
     * The service starts asynchronously, so we retry on the main handler until
     * the service instance is available (up to WIRE_MAX_RETRIES times).
     */
    private fun wireServiceDependencies(retryCount: Int) {
        val svc = CursorOverlayService.getInstance()
        if (svc == null) {
            if (retryCount < WIRE_MAX_RETRIES) {
                mainHandler.postDelayed({
                    wireServiceDependencies(retryCount + 1)
                }, WIRE_RETRY_DELAY_MS)
            } else {
                Log.w(TAG, "Service not ready after $WIRE_MAX_RETRIES retries, skipping wire")
            }
            return
        }

        val ctrl = svc.getCursorController()
        if (ctrl == null) {
            Log.w(TAG, "CursorController not available from service")
            return
        }

        // Wire CursorActions so voice movement commands ("cursor up/down/left/right")
        // affect the same controller the overlay observes
        val gestureDispatcher = AndroidGestureDispatcher(service)
        CursorActions.initialize(ctrl, gestureDispatcher)
        Log.i(TAG, "CursorActions wired to overlay controller + gesture dispatcher")

        // Wire ClickDispatcher for dwell-click and service-level click dispatch
        svc.setClickDispatcher(AccessibilityClickDispatcherImpl(service))
        Log.i(TAG, "ClickDispatcher wired to overlay service")
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

/**
 * ClickDispatcher implementation using AccessibilityService gesture dispatch.
 * Created inline by AndroidCursorHandler to wire into CursorOverlayService.
 */
private class AccessibilityClickDispatcherImpl(
    private val service: AccessibilityService
) : ClickDispatcher {

    override fun dispatchClick(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 50L))
            .build()
        service.dispatchGesture(gesture, null, null)
    }

    override fun dispatchLongPress(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 1000L))
            .build()
        service.dispatchGesture(gesture, null, null)
    }
}
