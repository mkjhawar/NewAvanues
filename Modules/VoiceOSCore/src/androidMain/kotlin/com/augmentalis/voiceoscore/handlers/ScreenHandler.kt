/**
 * ScreenHandler.kt - IHandler for screen/device hardware commands
 *
 * Handles: brightness, WiFi, Bluetooth, screenshot, flashlight, rotate
 * Uses system APIs for hardware control.
 *
 * All methods that require special or runtime permissions guard with
 * a check before calling the API, and fall back to opening the relevant
 * system settings screen when the permission is not granted.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat
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

    // ── Brightness ──────────────────────────────────────────────────────

    private fun adjustBrightness(delta: Int): HandlerResult {
        return try {
            val context = service.applicationContext
            if (!Settings.System.canWrite(context)) {
                return requestWriteSettingsPermission(context, "brightness")
            }
            val current = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
            val newValue = (current + delta).coerceIn(0, 255)
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, newValue)
            val percentage = (newValue * 100 / 255)
            val label = if (delta > 0) "Brightness increased to $percentage%" else "Brightness decreased to $percentage%"
            HandlerResult.success(label)
        } catch (e: Exception) {
            Log.e(TAG, "Brightness adjustment failed", e)
            HandlerResult.failure("Cannot adjust brightness: ${e.message}")
        }
    }

    // ── WiFi ────────────────────────────────────────────────────────────

    /**
     * Toggle WiFi state.
     *
     * On Android 10+ (API 29), WifiManager.setWifiEnabled() is a no-op for
     * non-system apps and silently returns false. We open the WiFi settings
     * panel instead, which lets the user toggle it with one tap.
     *
     * On Android 9 and below, the direct API still works.
     */
    private fun toggleWifi(): HandlerResult {
        return try {
            val context = service.applicationContext
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: Direct toggle blocked for non-system apps.
                // Open the WiFi settings panel (shows as bottom sheet on 10+).
                val intent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                HandlerResult.success("Opening WiFi panel")
            } else {
                // Android 9 and below: direct toggle works
                val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val newState = !wifiManager.isWifiEnabled
                @Suppress("DEPRECATION")
                val toggled = wifiManager.setWifiEnabled(newState)
                if (toggled) {
                    HandlerResult.success(if (newState) "WiFi enabled" else "WiFi disabled")
                } else {
                    HandlerResult.failure("Cannot toggle WiFi")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "WiFi toggle failed", e)
            HandlerResult.failure("Cannot toggle WiFi: ${e.message}")
        }
    }

    // ── Bluetooth ───────────────────────────────────────────────────────

    /**
     * Toggle Bluetooth state.
     *
     * Android 12+ (API 31) requires BLUETOOTH_CONNECT runtime permission.
     * Android 13+ (API 33) deprecated enable()/disable() entirely — they
     * may silently fail. We check the permission first and fall back to
     * opening Bluetooth settings if not granted or if the direct API fails.
     */
    @Suppress("DEPRECATION")
    private fun toggleBluetooth(): HandlerResult {
        return try {
            val context = service.applicationContext

            // Android 12+: check BLUETOOTH_CONNECT runtime permission
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    return openBluetoothSettings(context, "Bluetooth permission not granted")
                }
            }

            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
                ?: return HandlerResult.failure("Bluetooth not available")

            val wasEnabled = adapter.isEnabled
            val success = if (wasEnabled) adapter.disable() else adapter.enable()

            if (success) {
                HandlerResult.success(if (wasEnabled) "Bluetooth disabled" else "Bluetooth enabled")
            } else {
                // enable()/disable() returned false — likely Android 13+ restriction.
                // Fall back to Bluetooth settings.
                openBluetoothSettings(context, "Opening Bluetooth settings")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth toggle permission denied", e)
            openBluetoothSettings(service.applicationContext, "Bluetooth permission required")
        } catch (e: Exception) {
            Log.e(TAG, "Bluetooth toggle failed", e)
            HandlerResult.failure("Cannot toggle Bluetooth: ${e.message}")
        }
    }

    private fun openBluetoothSettings(context: Context, message: String): HandlerResult {
        return try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            HandlerResult.failure(message)
        } catch (e: Exception) {
            HandlerResult.failure("Cannot open Bluetooth settings")
        }
    }

    // ── Screenshot ──────────────────────────────────────────────────────

    private fun takeScreenshot(): HandlerResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val success = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            if (success) HandlerResult.success("Screenshot taken")
            else HandlerResult.failure("Failed to take screenshot")
        } else {
            HandlerResult.failure("Screenshot requires Android 9+")
        }
    }

    // ── Flashlight ──────────────────────────────────────────────────────

    private fun setFlashlight(on: Boolean): HandlerResult {
        return try {
            val cameraManager = service.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraId = cameraManager.cameraIdList.firstOrNull()
                ?: return HandlerResult.failure("No camera available")
            cameraManager.setTorchMode(cameraId, on)
            val label = if (on) "Flashlight on" else "Flashlight off"
            HandlerResult.success(label)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Flashlight control failed: camera in use", e)
            HandlerResult.failure("Flashlight unavailable: camera is in use by another app")
        } catch (e: Exception) {
            Log.e(TAG, "Flashlight control failed", e)
            HandlerResult.failure("Cannot control flashlight: ${e.message}")
        }
    }

    // ── Rotation ────────────────────────────────────────────────────────

    private fun toggleRotation(): HandlerResult {
        return try {
            val context = service.applicationContext
            if (!Settings.System.canWrite(context)) {
                return requestWriteSettingsPermission(context, "rotation")
            }
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

    // ── Settings ────────────────────────────────────────────────────────

    private fun openSettings(): HandlerResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            service.applicationContext.startActivity(intent)
            HandlerResult.success("Settings opened")
        } catch (e: Exception) {
            Log.e(TAG, "Open settings failed", e)
            HandlerResult.failure("Cannot open settings: ${e.message}")
        }
    }

    // ── Notifications ───────────────────────────────────────────────────

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

    // ── Permission helpers ──────────────────────────────────────────────

    /**
     * Launch the system "Modify System Settings" permission screen for this app.
     * Called when Settings.System.canWrite() returns false.
     */
    private fun requestWriteSettingsPermission(context: Context, feature: String): HandlerResult {
        return try {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Log.w(TAG, "WRITE_SETTINGS not granted, opening permission screen for $feature")
            HandlerResult.failure("Please grant 'Modify System Settings' permission to adjust $feature")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open WRITE_SETTINGS permission screen", e)
            HandlerResult.failure("Cannot adjust $feature: permission not granted")
        }
    }
}
