/**
 * AndroidActionExecutor.kt - Android implementation of action execution
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Android-specific implementation using AccessibilityService.
 * Executes voice commands by performing accessibility actions.
 */
package com.augmentalis.voiceoscoreng.handlers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_HOME
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_RECENTS
import android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT
import android.accessibilityservice.GestureDescription
import android.content.Context
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.common.CommandActionType
import com.augmentalis.voiceoscoreng.common.QuantizedCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Android implementation of IActionExecutor.
 *
 * Uses AccessibilityService to perform actions on UI elements
 * and system-level operations.
 *
 * @param service AccessibilityService instance for performing actions
 * @param context Android context for system services
 */
class AndroidActionExecutor(
    private val service: AccessibilityService,
    private val context: Context = service.applicationContext
) : IActionExecutor {

    // Element cache for VUID lookups
    private val elementCache = mutableMapOf<String, AccessibilityNodeInfo>()

    // ═══════════════════════════════════════════════════════════════════
    // Element Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun tap(vuid: String): ActionResult = withContext(Dispatchers.Main) {
        val node = findNodeByVuid(vuid)
            ?: return@withContext ActionResult.ElementNotFound(vuid)

        if (!node.isEnabled) {
            return@withContext ActionResult.ElementNotActionable(vuid, "Element is disabled")
        }

        val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (result) {
            ActionResult.Success("Tapped element")
        } else {
            // Try gesture-based tap as fallback
            tapByGesture(node)
        }
    }

    override suspend fun longPress(vuid: String, durationMs: Long): ActionResult = withContext(Dispatchers.Main) {
        val node = findNodeByVuid(vuid)
            ?: return@withContext ActionResult.ElementNotFound(vuid)

        if (!node.isEnabled) {
            return@withContext ActionResult.ElementNotActionable(vuid, "Element is disabled")
        }

        val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        if (result) {
            ActionResult.Success("Long pressed element")
        } else {
            // Try gesture-based long press as fallback
            longPressByGesture(node, durationMs)
        }
    }

    override suspend fun focus(vuid: String): ActionResult = withContext(Dispatchers.Main) {
        val node = findNodeByVuid(vuid)
            ?: return@withContext ActionResult.ElementNotFound(vuid)

        val result = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        if (result) {
            ActionResult.Success("Focused element")
        } else {
            // Try accessibility focus
            node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS)
            ActionResult.Success("Accessibility focused element")
        }
    }

    override suspend fun enterText(text: String, vuid: String?): ActionResult = withContext(Dispatchers.Main) {
        val node = if (vuid != null) {
            findNodeByVuid(vuid) ?: return@withContext ActionResult.ElementNotFound(vuid)
        } else {
            findFocusedEditableNode() ?: return@withContext ActionResult.Error("No editable element focused")
        }

        if (!node.isEditable) {
            return@withContext ActionResult.ElementNotActionable(
                vuid ?: "focused",
                "Element is not editable"
            )
        }

        val arguments = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }

        val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        if (result) {
            ActionResult.Success("Entered text")
        } else {
            ActionResult.Error("Failed to enter text")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Scroll Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun scroll(
        direction: ScrollDirection,
        amount: Float,
        vuid: String?
    ): ActionResult = withContext(Dispatchers.Main) {
        val node = if (vuid != null) {
            findNodeByVuid(vuid) ?: return@withContext ActionResult.ElementNotFound(vuid)
        } else {
            findScrollableNode() ?: return@withContext ActionResult.Error("No scrollable element found")
        }

        val action = when (direction) {
            ScrollDirection.UP, ScrollDirection.LEFT -> AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            ScrollDirection.DOWN, ScrollDirection.RIGHT -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
        }

        val result = node.performAction(action)
        if (result) {
            ActionResult.Success("Scrolled ${direction.name.lowercase()}")
        } else {
            // Try gesture-based scroll as fallback
            scrollByGesture(direction, amount)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Navigation Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun back(): ActionResult = withContext(Dispatchers.Main) {
        val result = service.performGlobalAction(GLOBAL_ACTION_BACK)
        if (result) ActionResult.Success("Navigated back")
        else ActionResult.Error("Failed to go back")
    }

    override suspend fun home(): ActionResult = withContext(Dispatchers.Main) {
        val result = service.performGlobalAction(GLOBAL_ACTION_HOME)
        if (result) ActionResult.Success("Navigated home")
        else ActionResult.Error("Failed to go home")
    }

    override suspend fun recentApps(): ActionResult = withContext(Dispatchers.Main) {
        val result = service.performGlobalAction(GLOBAL_ACTION_RECENTS)
        if (result) ActionResult.Success("Showing recent apps")
        else ActionResult.Error("Failed to show recents")
    }

    override suspend fun appDrawer(): ActionResult = withContext(Dispatchers.Main) {
        // App drawer access varies by launcher, try common approaches
        home().let {
            if (!it.isSuccess) return@withContext it
        }
        // Swipe up gesture to open app drawer (common on most launchers)
        swipeUp()
    }

    // ═══════════════════════════════════════════════════════════════════
    // System Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun openSettings(): ActionResult = withContext(Dispatchers.Main) {
        try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Opened settings")
        } catch (e: Exception) {
            ActionResult.Error("Failed to open settings: ${e.message}")
        }
    }

    override suspend fun showNotifications(): ActionResult = withContext(Dispatchers.Main) {
        val result = service.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
        if (result) ActionResult.Success("Showing notifications")
        else ActionResult.Error("Failed to show notifications")
    }

    override suspend fun clearNotifications(): ActionResult = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val result = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            if (result) ActionResult.Success("Cleared notifications")
            else ActionResult.Error("Failed to clear notifications")
        } else {
            ActionResult.NotSupported(CommandActionType.CLEAR_NOTIFICATIONS)
        }
    }

    override suspend fun screenshot(): ActionResult = withContext(Dispatchers.Main) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val result = service.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            if (result) ActionResult.Success("Screenshot taken")
            else ActionResult.Error("Failed to take screenshot")
        } else {
            ActionResult.NotSupported(CommandActionType.SCREENSHOT)
        }
    }

    override suspend fun flashlight(on: Boolean): ActionResult = withContext(Dispatchers.Main) {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return@withContext ActionResult.Error("No camera available")

            cameraManager.setTorchMode(cameraId, on)
            ActionResult.Success("Flashlight ${if (on) "on" else "off"}")
        } catch (e: Exception) {
            ActionResult.Error("Failed to control flashlight: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Media Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun mediaPlayPause(): ActionResult = withContext(Dispatchers.Main) {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
    }

    override suspend fun mediaNext(): ActionResult = withContext(Dispatchers.Main) {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
    }

    override suspend fun mediaPrevious(): ActionResult = withContext(Dispatchers.Main) {
        sendMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
    }

    override suspend fun volume(direction: VolumeDirection): ActionResult = withContext(Dispatchers.Main) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        when (direction) {
            VolumeDirection.UP -> audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            VolumeDirection.DOWN -> audioManager.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            VolumeDirection.MUTE -> audioManager.adjustVolume(AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            VolumeDirection.UNMUTE -> audioManager.adjustVolume(AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
        }
        ActionResult.Success("Volume ${direction.name.lowercase()}")
    }

    // ═══════════════════════════════════════════════════════════════════
    // App Actions
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun openApp(appType: String): ActionResult = withContext(Dispatchers.Main) {
        val packageName = resolveAppType(appType)
            ?: return@withContext ActionResult.Error("Unknown app type: $appType")

        openAppByPackage(packageName)
    }

    override suspend fun openAppByPackage(packageName: String): ActionResult = withContext(Dispatchers.Main) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                ?: return@withContext ActionResult.Error("App not found: $packageName")

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult.Success("Opened app")
        } catch (e: Exception) {
            ActionResult.Error("Failed to open app: ${e.message}")
        }
    }

    override suspend fun closeApp(): ActionResult = withContext(Dispatchers.Main) {
        // Close current app by going home then recent apps and swiping
        back()
    }

    // ═══════════════════════════════════════════════════════════════════
    // Generic Execution
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun executeCommand(command: QuantizedCommand): ActionResult {
        val targetVuid = command.targetVuid ?: return ActionResult.Error("No target VUID specified")
        return when (command.actionType) {
            CommandActionType.CLICK -> tap(targetVuid)
            CommandActionType.LONG_CLICK -> longPress(targetVuid)
            CommandActionType.FOCUS -> focus(targetVuid)
            CommandActionType.TYPE -> enterText(command.phrase, targetVuid)
            CommandActionType.SCROLL_DOWN -> scroll(ScrollDirection.DOWN)
            CommandActionType.SCROLL_UP -> scroll(ScrollDirection.UP)
            CommandActionType.SCROLL_LEFT -> scroll(ScrollDirection.LEFT)
            CommandActionType.SCROLL_RIGHT -> scroll(ScrollDirection.RIGHT)
            CommandActionType.BACK -> back()
            CommandActionType.HOME -> home()
            CommandActionType.RECENT_APPS -> recentApps()
            CommandActionType.APP_DRAWER -> appDrawer()
            CommandActionType.NAVIGATE -> tap(targetVuid) // Navigate = tap on nav element
            else -> executeAction(command.actionType)
        }
    }

    override suspend fun executeAction(
        actionType: CommandActionType,
        params: Map<String, Any>
    ): ActionResult {
        return when (actionType) {
            CommandActionType.BACK -> back()
            CommandActionType.HOME -> home()
            CommandActionType.RECENT_APPS -> recentApps()
            CommandActionType.APP_DRAWER -> appDrawer()
            CommandActionType.OPEN_SETTINGS -> openSettings()
            CommandActionType.NOTIFICATIONS -> showNotifications()
            CommandActionType.CLEAR_NOTIFICATIONS -> clearNotifications()
            CommandActionType.SCREENSHOT -> screenshot()
            CommandActionType.FLASHLIGHT_ON -> flashlight(true)
            CommandActionType.FLASHLIGHT_OFF -> flashlight(false)
            CommandActionType.MEDIA_PLAY, CommandActionType.MEDIA_PAUSE -> mediaPlayPause()
            CommandActionType.MEDIA_NEXT -> mediaNext()
            CommandActionType.MEDIA_PREVIOUS -> mediaPrevious()
            CommandActionType.VOLUME_UP -> volume(VolumeDirection.UP)
            CommandActionType.VOLUME_DOWN -> volume(VolumeDirection.DOWN)
            CommandActionType.VOLUME_MUTE -> volume(VolumeDirection.MUTE)
            CommandActionType.OPEN_APP -> {
                val appType = params["app_type"] as? String
                if (appType != null) openApp(appType)
                else ActionResult.Error("Missing app_type parameter")
            }
            CommandActionType.CLOSE_APP -> closeApp()
            CommandActionType.SCROLL_DOWN -> scroll(ScrollDirection.DOWN)
            CommandActionType.SCROLL_UP -> scroll(ScrollDirection.UP)
            CommandActionType.SCROLL_LEFT -> scroll(ScrollDirection.LEFT)
            CommandActionType.SCROLL_RIGHT -> scroll(ScrollDirection.RIGHT)
            else -> ActionResult.NotSupported(actionType)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Element Lookup
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun elementExists(vuid: String): Boolean = withContext(Dispatchers.Main) {
        findNodeByVuid(vuid) != null
    }

    override suspend fun getElementBounds(vuid: String): ElementBounds? = withContext(Dispatchers.Main) {
        val node = findNodeByVuid(vuid) ?: return@withContext null
        val rect = Rect()
        node.getBoundsInScreen(rect)
        ElementBounds(rect.left, rect.top, rect.right, rect.bottom)
    }

    // ═══════════════════════════════════════════════════════════════════
    // Private Helpers
    // ═══════════════════════════════════════════════════════════════════

    private fun findNodeByVuid(vuid: String): AccessibilityNodeInfo? {
        // Check cache first
        elementCache[vuid]?.let { return it }

        // Search accessibility tree
        val root = service.rootInActiveWindow ?: return null
        return searchForVuid(root, vuid)
    }

    private fun searchForVuid(node: AccessibilityNodeInfo, vuid: String): AccessibilityNodeInfo? {
        // Check if this node matches (viewIdResourceName often contains VUID-like identifiers)
        val nodeId = node.viewIdResourceName ?: ""
        val nodeText = node.text?.toString() ?: ""
        val nodeDesc = node.contentDescription?.toString() ?: ""

        // Match by resource ID, text, or content description
        if (nodeId.contains(vuid, ignoreCase = true) ||
            nodeText.equals(vuid, ignoreCase = true) ||
            nodeDesc.equals(vuid, ignoreCase = true)) {
            return node
        }

        // Search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = searchForVuid(child, vuid)
            if (result != null) return result
        }

        return null
    }

    private fun findFocusedEditableNode(): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
    }

    private fun findScrollableNode(): AccessibilityNodeInfo? {
        val root = service.rootInActiveWindow ?: return null
        return findFirstScrollable(root)
    }

    private fun findFirstScrollable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findFirstScrollable(child)
            if (result != null) return result
        }
        return null
    }

    private suspend fun tapByGesture(node: AccessibilityNodeInfo): ActionResult {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return performGestureTap(rect.centerX().toFloat(), rect.centerY().toFloat())
    }

    private suspend fun longPressByGesture(node: AccessibilityNodeInfo, durationMs: Long): ActionResult {
        val rect = Rect()
        node.getBoundsInScreen(rect)
        return performGestureLongPress(rect.centerX().toFloat(), rect.centerY().toFloat(), durationMs)
    }

    private suspend fun performGestureTap(x: Float, y: Float): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return ActionResult.NotSupported(CommandActionType.CLICK)
        }

        return suspendCancellableCoroutine { continuation ->
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
                .build()

            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    continuation.resume(ActionResult.Success("Tapped via gesture"))
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    continuation.resume(ActionResult.Error("Gesture cancelled"))
                }
            }, null)
        }
    }

    private suspend fun performGestureLongPress(x: Float, y: Float, durationMs: Long): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return ActionResult.NotSupported(CommandActionType.LONG_CLICK)
        }

        return suspendCancellableCoroutine { continuation ->
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()

            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    continuation.resume(ActionResult.Success("Long pressed via gesture"))
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    continuation.resume(ActionResult.Error("Gesture cancelled"))
                }
            }, null)
        }
    }

    private suspend fun scrollByGesture(direction: ScrollDirection, amount: Float): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return ActionResult.NotSupported(CommandActionType.SCROLL_DOWN)
        }

        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val (startX, startY, endX, endY) = when (direction) {
            ScrollDirection.DOWN -> listOf(
                screenWidth / 2f, screenHeight * 0.7f,
                screenWidth / 2f, screenHeight * 0.3f
            )
            ScrollDirection.UP -> listOf(
                screenWidth / 2f, screenHeight * 0.3f,
                screenWidth / 2f, screenHeight * 0.7f
            )
            ScrollDirection.LEFT -> listOf(
                screenWidth * 0.7f, screenHeight / 2f,
                screenWidth * 0.3f, screenHeight / 2f
            )
            ScrollDirection.RIGHT -> listOf(
                screenWidth * 0.3f, screenHeight / 2f,
                screenWidth * 0.7f, screenHeight / 2f
            )
        }

        return performGestureSwipe(startX, startY, endX, endY)
    }

    private suspend fun swipeUp(): ActionResult {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        return performGestureSwipe(
            screenWidth / 2f, screenHeight * 0.8f,
            screenWidth / 2f, screenHeight * 0.2f
        )
    }

    private suspend fun performGestureSwipe(
        startX: Float, startY: Float,
        endX: Float, endY: Float
    ): ActionResult {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return ActionResult.Error("Gestures require Android N+")
        }

        return suspendCancellableCoroutine { continuation ->
            val path = Path().apply {
                moveTo(startX, startY)
                lineTo(endX, endY)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 300))
                .build()

            service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription?) {
                    continuation.resume(ActionResult.Success("Swipe completed"))
                }
                override fun onCancelled(gestureDescription: GestureDescription?) {
                    continuation.resume(ActionResult.Error("Swipe cancelled"))
                }
            }, null)
        }
    }

    private fun sendMediaKey(keyCode: Int): ActionResult {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val keyDownEvent = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val keyUpEvent = KeyEvent(KeyEvent.ACTION_UP, keyCode)

        audioManager.dispatchMediaKeyEvent(keyDownEvent)
        audioManager.dispatchMediaKeyEvent(keyUpEvent)

        return ActionResult.Success("Media key sent")
    }

    private fun resolveAppType(appType: String): String? {
        return when (appType.lowercase()) {
            "browser" -> getBrowserPackage()
            "camera" -> getCameraPackage()
            "gallery", "photos" -> getGalleryPackage()
            "calculator" -> "com.android.calculator2"
            "calendar" -> "com.android.calendar"
            "phone", "dialer" -> "com.android.dialer"
            "messages", "sms" -> "com.android.mms"
            "contacts" -> "com.android.contacts"
            else -> null
        }
    }

    private fun getBrowserPackage(): String {
        // Try common browsers
        val browsers = listOf(
            "com.android.chrome",
            "org.mozilla.firefox",
            "com.brave.browser",
            "com.opera.browser"
        )
        return browsers.firstOrNull { isPackageInstalled(it) }
            ?: "com.android.browser"
    }

    private fun getCameraPackage(): String {
        val cameras = listOf(
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera"
        )
        return cameras.firstOrNull { isPackageInstalled(it) }
            ?: "com.android.camera"
    }

    private fun getGalleryPackage(): String {
        val galleries = listOf(
            "com.google.android.apps.photos",
            "com.android.gallery3d",
            "com.android.gallery"
        )
        return galleries.firstOrNull { isPackageInstalled(it) }
            ?: "com.android.gallery3d"
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clear element cache (call when screen changes)
     */
    fun clearCache() {
        elementCache.clear()
    }
}
