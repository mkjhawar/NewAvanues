/**
 * SystemHandler.kt - Handles system-level accessibility actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-26
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.augmentalis.voiceoscore.accessibility.IVoiceOSContext
import android.accessibilityservice.AccessibilityService as AndroidAccessibilityService

/**
 * Handler for system-level actions
 * Direct implementation with ActionHandler interface
 */
class SystemHandler(
    private val context: IVoiceOSContext
) : ActionHandler {

    companion object {
        private const val TAG = "SystemHandler"

        // Supported action patterns
        val SUPPORTED_ACTIONS = listOf(
            "back", "go back",
            "home", "go home",
            "recent", "recent apps", "recents",
            "notifications", "notification panel",
            "settings", "quick settings",
            "power", "power menu",
            "screenshot", "take screenshot",
            "split screen", "split",
            "assistant", "voice assistant",
            "lock", "lock screen",
            "all apps", "app drawer"
        )
    }

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()

        Log.d(TAG, "Executing system action: $normalizedAction")

        return when {
            // Navigation actions
            normalizedAction == "back" || normalizedAction == "go back" -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_BACK)
            }

            normalizedAction == "home" || normalizedAction == "go home" -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_HOME)
            }

            normalizedAction in listOf("recent", "recent apps", "recents") -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_RECENTS)
            }

            // System panels
            normalizedAction == "notifications" || normalizedAction == "notification panel" -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            }

            normalizedAction == "settings" || normalizedAction == "quick settings" -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            }

            normalizedAction == "power" || normalizedAction == "power menu" -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            }

            // Screenshot (Android P+)
            normalizedAction == "screenshot" || normalizedAction == "take screenshot" -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            }

            // Split screen (Android N+)
            normalizedAction == "split screen" || normalizedAction == "split" -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            }

            // Voice assistant (Android P+)
            normalizedAction == "assistant" || normalizedAction == "voice assistant" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_SHORTCUT)
                } else {
                    false
                }
            }

            // Lock screen (Android P+)
            normalizedAction == "lock" || normalizedAction == "lock screen" -> {
                context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            }

            // All apps (Android S+)
            normalizedAction == "all apps" || normalizedAction == "app drawer" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS)
                } else {
                    // Fallback to home long press simulation
                    context.performGlobalAction(AndroidAccessibilityService.GLOBAL_ACTION_HOME)
                    true
                }
            }

            // Settings intents
            normalizedAction.startsWith("open settings") -> {
                openSettings(normalizedAction)
            }

            else -> {
                Log.w(TAG, "Unknown system action: $normalizedAction")
                false
            }
        }
    }

    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.contains(it) } || normalized.startsWith("open settings")
    }

    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }

    override fun initialize() {
        Log.d(TAG, "SystemHandler initialized")
    }

    override fun dispose() {
        Log.d(TAG, "SystemHandler disposed")
    }

    /**
     * Open specific settings screens
     */
    private fun openSettings(action: String): Boolean {
        val intent = when {
            action.contains("wifi") -> {
                Intent(Settings.ACTION_WIFI_SETTINGS)
            }

            action.contains("bluetooth") -> {
                Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            }

            action.contains("accessibility") -> {
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            }

            action.contains("display") -> {
                Intent(Settings.ACTION_DISPLAY_SETTINGS)
            }

            action.contains("sound") || action.contains("audio") -> {
                Intent(Settings.ACTION_SOUND_SETTINGS)
            }

            action.contains("battery") -> {
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            }

            action.contains("storage") -> {
                Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            }

            action.contains("security") -> {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }

            action.contains("location") -> {
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            }

            action.contains("developer") -> {
                Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            }

            else -> {
                Intent(Settings.ACTION_SETTINGS)
            }
        }

        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings", e)
            false
        }
    }
}