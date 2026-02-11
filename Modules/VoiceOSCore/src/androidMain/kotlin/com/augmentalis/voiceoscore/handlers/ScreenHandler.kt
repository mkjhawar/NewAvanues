/**
 * ScreenHandler.kt - IHandler for screen/device hardware commands
 *
 * Handles: brightness, WiFi, Bluetooth, screenshot, flashlight, rotate
 * Uses system APIs for hardware control.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

private const val TAG = "ScreenHandler"

class ScreenHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.DEVICE

    override val supportedActions: List<String> = listOf(
        // Brightness
        "brightness up", "increase brightness", "brighter",
        "brightness down", "decrease brightness", "dimmer",
        // WiFi
        "toggle wifi", "wifi on", "wifi off", "turn on wifi", "turn off wifi",
        // Bluetooth
        "toggle bluetooth", "bluetooth on", "bluetooth off", "turn on bluetooth", "turn off bluetooth",
        // Screenshot
        "take screenshot", "screenshot", "capture screen",
        // Flashlight
        "turn on flashlight", "flashlight on", "torch on",
        "turn off flashlight", "flashlight off", "torch off",
        // Rotation
        "rotate screen", "rotate", "change orientation",
        // Settings
        "open settings", "settings", "show settings", "device settings",
        // Notifications
        "clear notifications", "dismiss notifications"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        Log.d(TAG, "ScreenHandler.execute: '$phrase'")

        return when {
            // Brightness
            phrase in listOf("brightness up", "increase brightness", "brighter") -> adjustBrightness(25)
            phrase in listOf("brightness down", "decrease brightness", "dimmer") -> adjustBrightness(-25)

            // WiFi
            phrase in listOf("toggle wifi", "wifi on", "wifi off", "turn on wifi", "turn off wifi") -> toggleWifi()

            // Bluetooth
            phrase in listOf("toggle bluetooth", "bluetooth on", "bluetooth off", "turn on bluetooth", "turn off bluetooth") -> toggleBluetooth()

            // Screenshot
            phrase in listOf("take screenshot", "screenshot", "capture screen") -> takeScreenshot()

            // Flashlight
            phrase in listOf("turn on flashlight", "flashlight on", "torch on") -> setFlashlight(true)
            phrase in listOf("turn off flashlight", "flashlight off", "torch off") -> setFlashlight(false)

            // Rotation
            phrase in listOf("rotate screen", "rotate", "change orientation") -> toggleRotation()

            // Settings
            phrase in listOf("open settings", "settings", "show settings", "device settings") -> openSettings()

            // Clear notifications
            phrase in listOf("clear notifications", "dismiss notifications") -> clearNotifications()

            else -> HandlerResult.notHandled()
        }
    }

    private fun adjustBrightness(delta: Int): HandlerResult {
        return try {
            val context = service.applicationContext
            val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            val newValue = (current + delta).coerceIn(0, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, newValue)
            val label = if (delta > 0) "Brightness increased" else "Brightness decreased"
            HandlerResult.success(label)
        } catch (e: Exception) {
            Log.e(TAG, "Brightness adjustment failed", e)
            HandlerResult.failure("Cannot adjust brightness: ${e.message}")
        }
    }

    @Suppress("DEPRECATION")
    private fun toggleWifi(): HandlerResult {
        return try {
            val wifiManager = service.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val newState = !wifiManager.isWifiEnabled
            wifiManager.isWifiEnabled = newState
            val label = if (newState) "WiFi enabled" else "WiFi disabled"
            HandlerResult.success(label)
        } catch (e: Exception) {
            Log.e(TAG, "WiFi toggle failed", e)
            HandlerResult.failure("Cannot toggle WiFi: ${e.message}")
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun toggleBluetooth(): HandlerResult {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter()
                ?: return HandlerResult.failure("Bluetooth not available")
            if (adapter.isEnabled) adapter.disable() else adapter.enable()
            val label = if (adapter.isEnabled) "Bluetooth disabled" else "Bluetooth enabled"
            HandlerResult.success(label)
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth toggle failed", e)
            HandlerResult.failure("Cannot toggle Bluetooth: ${e.message}")
        }
    }

    private fun takeScreenshot(): HandlerResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            if (success) HandlerResult.success("Screenshot taken")
            else HandlerResult.failure("Failed to take screenshot")
        } else {
            HandlerResult.failure("Screenshot requires Android 9+")
        }
    }

    private fun setFlashlight(on: Boolean): HandlerResult {
        return try {
            val cameraManager = service.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return HandlerResult.failure("No camera available")
            cameraManager.setTorchMode(cameraId, on)
            val label = if (on) "Flashlight on" else "Flashlight off"
            HandlerResult.success(label)
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight control failed", e)
            HandlerResult.failure("Cannot control flashlight: ${e.message}")
        }
    }

    private fun toggleRotation(): HandlerResult {
        return try {
            val context = service.applicationContext
            val current = Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0)
            val newValue = if (current == 0) 1 else 0
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, newValue)
            val label = if (newValue == 1) "Auto-rotation enabled" else "Auto-rotation disabled"
            HandlerResult.success(label)
        } catch (e: Exception) {
            Log.e(TAG, "Rotation toggle failed", e)
            HandlerResult.failure("Cannot toggle rotation: ${e.message}")
        }
    }

    private fun openSettings(): HandlerResult {
        return try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            service.applicationContext.startActivity(intent)
            HandlerResult.success("Settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Open settings failed", e)
            HandlerResult.failure("Cannot open settings: ${e.message}")
        }
    }

    private fun clearNotifications(): HandlerResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
            if (success) HandlerResult.success("Notifications cleared")
            else HandlerResult.failure("Failed to clear notifications")
        } else {
            // Fallback: open then close notification shade
            service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            HandlerResult.success("Notification shade opened")
        }
    }
}
