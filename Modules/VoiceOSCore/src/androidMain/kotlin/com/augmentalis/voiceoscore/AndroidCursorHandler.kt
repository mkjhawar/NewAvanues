/**
 * AndroidCursorHandler.kt - IHandler for cursor voice commands
 *
 * Routes "show cursor", "hide cursor", "cursor click", cursor movement,
 * and cursor-specific gestures to the CursorOverlayService. Integrates
 * with the ActionCoordinator handler system (IHandler).
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
import com.augmentalis.devicemanager.imu.IMUManager
import com.augmentalis.voicecursor.core.ClickDispatcher
import com.augmentalis.voicecursor.overlay.CursorOverlayService
import com.augmentalis.voiceoscore.commandmanager.actions.CursorActions
import com.augmentalis.voiceoscore.commandmanager.actions.CursorDirection

private const val TAG = "AndroidCursorHandler"
private const val WIRE_RETRY_DELAY_MS = 100L
private const val WIRE_MAX_RETRIES = 20

/**
 * Wire CursorActions, ClickDispatcher, and IMU tracking to the overlay service.
 *
 * Called from both AndroidCursorHandler (voice "show cursor") and
 * VoiceAvanueAccessibilityService (settings toggle). Extracted as a
 * top-level function to avoid duplicating the retry + wiring logic.
 *
 * The service starts asynchronously, so we retry on the main handler until
 * the service instance is available (up to WIRE_MAX_RETRIES times).
 *
 * @param service The AccessibilityService to use for gesture dispatch and IMU
 * @param retryCount Current retry count (starts at 0)
 */
fun wireCursorDependencies(service: AccessibilityService, retryCount: Int = 0) {
    val svc = CursorOverlayService.getInstance()
    if (svc == null) {
        if (retryCount < WIRE_MAX_RETRIES) {
            Handler(Looper.getMainLooper()).postDelayed({
                wireCursorDependencies(service, retryCount + 1)
            }, WIRE_RETRY_DELAY_MS)
        } else {
            Log.w(TAG, "Service not ready after $WIRE_MAX_RETRIES retries, skipping wire")
        }
        return
    }

    Log.i(TAG, "Cursor dependencies wired after ${retryCount + 1} attempts (${(retryCount + 1) * WIRE_RETRY_DELAY_MS}ms)")

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

    // Wire IMU head tracking so cursor follows head motion
    val imuManager = IMUManager.getInstance(service.applicationContext)
    val imuStarted = imuManager.startIMUTracking("cursor_voice")
    if (imuStarted) {
        svc.startIMUTracking(imuManager)
        Log.i(TAG, "IMU head tracking wired to cursor controller")
    } else {
        Log.w(TAG, "IMU tracking not started — no sensors or capabilities not injected")
    }
}

/**
 * IHandler implementation for cursor voice commands.
 *
 * Handles show/hide (start/stop CursorOverlayService), click, movement
 * (up/down/left/right), and cursor-specific long press / double tap.
 *
 * Registered in AndroidHandlerFactory alongside AndroidGestureHandler,
 * SystemHandler, and AppHandler.
 */
class AndroidCursorHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.GAZE

    override val supportedActions: List<String> = listOf(
        // Show/Hide
        "show cursor", "cursor on", "enable cursor",
        "hide cursor", "cursor off", "disable cursor",
        // Click
        "cursor click", "click here",
        // Movement
        "cursor up", "cursor down", "cursor left", "cursor right",
        "move cursor up", "move cursor down", "move cursor left", "move cursor right",
        // Cursor-specific gestures
        "long press here", "double tap here"
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

            // Movement
            phrase in listOf("cursor up", "move cursor up") -> cursorMove(CursorDirection.UP)
            phrase in listOf("cursor down", "move cursor down") -> cursorMove(CursorDirection.DOWN)
            phrase in listOf("cursor left", "move cursor left") -> cursorMove(CursorDirection.LEFT)
            phrase in listOf("cursor right", "move cursor right") -> cursorMove(CursorDirection.RIGHT)

            // Cursor-specific gestures
            phrase == "long press here" -> cursorLongPress()
            phrase == "double tap here" -> cursorDoubleTap()

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
            wireCursorDependencies(service, retryCount = 0)

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
            // Stop IMU tracking before stopping the service to release sensor resources
            IMUManager.getInstance(context).stopIMUTracking("cursor_voice")

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

    private suspend fun cursorMove(direction: CursorDirection): HandlerResult {
        if (!CursorActions.isInitialized()) {
            return HandlerResult.failure("Cursor not active — say 'show cursor' first")
        }

        val success = CursorActions.moveCursor(direction)
        val dirName = direction.name.lowercase()
        return if (success) {
            HandlerResult.success("Cursor moved $dirName")
        } else {
            HandlerResult.failure("Failed to move cursor $dirName")
        }
    }

    private suspend fun cursorLongPress(): HandlerResult {
        if (!CursorActions.isInitialized()) {
            return HandlerResult.failure("Cursor not active — say 'show cursor' first")
        }

        val success = CursorActions.longPress()
        return if (success) {
            HandlerResult.success("Long pressed at cursor position")
        } else {
            HandlerResult.failure("Failed to long press at cursor")
        }
    }

    private suspend fun cursorDoubleTap(): HandlerResult {
        if (!CursorActions.isInitialized()) {
            return HandlerResult.failure("Cursor not active — say 'show cursor' first")
        }

        val success = CursorActions.doubleClick()
        return if (success) {
            HandlerResult.success("Double tapped at cursor position")
        } else {
            HandlerResult.failure("Failed to double tap at cursor")
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
