/**
 * DeviceHandler.kt - Handles device control actions
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-27
 * 
 * Handles volume, brightness, connectivity, and device settings.
 * Implements ActionHandler interface (approved VOS4 exception).
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.util.Log
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService

/**
 * Handler for device control actions
 */
class DeviceHandler(
    private val service: VoiceAccessibilityService
) : ActionHandler {
    
    companion object {
        private const val TAG = "DeviceHandler"
        
        private val SUPPORTED_ACTIONS = listOf(
            "volume up", "volume down", "volume mute", "volume unmute",
            "brightness up", "brightness down", "brightness max", "brightness min",
            "wifi on", "wifi off", "bluetooth on", "bluetooth off",
            "airplane mode on", "airplane mode off", "flashlight on", "flashlight off",
            "do not disturb on", "do not disturb off", "silent mode", "vibrate mode"
        )
    }
    
    private val audioManager: AudioManager = 
        service.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()
        
        Log.d(TAG, "Executing device action: $normalizedAction")
        
        return when {
            // Volume controls
            normalizedAction == "volume up" -> {
                adjustVolume(AudioManager.ADJUST_RAISE)
            }
            
            normalizedAction == "volume down" -> {
                adjustVolume(AudioManager.ADJUST_LOWER)
            }
            
            normalizedAction == "volume mute" || normalizedAction == "mute" -> {
                adjustVolume(AudioManager.ADJUST_MUTE)
            }
            
            normalizedAction == "volume unmute" || normalizedAction == "unmute" -> {
                adjustVolume(AudioManager.ADJUST_UNMUTE)
            }
            
            // Brightness controls
            normalizedAction == "brightness up" -> {
                adjustBrightness(increase = true)
            }
            
            normalizedAction == "brightness down" -> {
                adjustBrightness(increase = false)
            }
            
            normalizedAction == "brightness max" || normalizedAction == "maximum brightness" -> {
                setBrightness(255)
            }
            
            normalizedAction == "brightness min" || normalizedAction == "minimum brightness" -> {
                setBrightness(10) // Not 0 to avoid completely dark screen
            }
            
            // Connectivity controls
            normalizedAction == "wifi on" || normalizedAction == "turn on wifi" -> {
                openSettings(Settings.ACTION_WIFI_SETTINGS)
            }
            
            normalizedAction == "wifi off" || normalizedAction == "turn off wifi" -> {
                openSettings(Settings.ACTION_WIFI_SETTINGS)
            }
            
            normalizedAction == "bluetooth on" || normalizedAction == "turn on bluetooth" -> {
                openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            
            normalizedAction == "bluetooth off" || normalizedAction == "turn off bluetooth" -> {
                openSettings(Settings.ACTION_BLUETOOTH_SETTINGS)
            }
            
            normalizedAction == "airplane mode on" || normalizedAction == "flight mode on" -> {
                openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            }
            
            normalizedAction == "airplane mode off" || normalizedAction == "flight mode off" -> {
                openSettings(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
            }
            
            // Sound modes
            normalizedAction == "silent mode" || normalizedAction == "silent" -> {
                setRingerMode(AudioManager.RINGER_MODE_SILENT)
            }
            
            normalizedAction == "vibrate mode" || normalizedAction == "vibrate" -> {
                setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            }
            
            normalizedAction == "normal mode" || normalizedAction == "sound on" -> {
                setRingerMode(AudioManager.RINGER_MODE_NORMAL)
            }
            
            // Do Not Disturb
            normalizedAction == "do not disturb on" || normalizedAction == "dnd on" -> {
                openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            }
            
            normalizedAction == "do not disturb off" || normalizedAction == "dnd off" -> {
                openSettings(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            }
            
            else -> {
                Log.w(TAG, "Unknown device action: $normalizedAction")
                false
            }
        }
    }
    
    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.contains(it) } ||
               normalized.contains("volume") || 
               normalized.contains("brightness") ||
               normalized.contains("wifi") ||
               normalized.contains("bluetooth") ||
               normalized.contains("airplane") ||
               normalized.contains("flashlight") ||
               normalized.contains("silent") ||
               normalized.contains("vibrate")
    }
    
    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS
    }
    
    private fun adjustVolume(direction: Int): Boolean {
        return try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust volume", e)
            false
        }
    }
    
    private fun setRingerMode(mode: Int): Boolean {
        return try {
            audioManager.ringerMode = mode
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set ringer mode", e)
            false
        }
    }
    
    private fun adjustBrightness(increase: Boolean): Boolean {
        return try {
            val resolver = service.contentResolver
            val currentBrightness = Settings.System.getInt(
                resolver, 
                Settings.System.SCREEN_BRIGHTNESS,
                128
            )
            
            val step = 25 // Adjust in steps of ~10%
            val newBrightness = if (increase) {
                (currentBrightness + step).coerceAtMost(255)
            } else {
                (currentBrightness - step).coerceAtLeast(10)
            }
            
            setBrightness(newBrightness)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust brightness", e)
            false
        }
    }
    
    private fun setBrightness(value: Int): Boolean {
        return try {
            Settings.System.putInt(
                service.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                service.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value
            )
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set brightness", e)
            false
        }
    }
    
    private fun openSettings(action: String): Boolean {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            service.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings: $action", e)
            false
        }
    }
}