/**
 * VoiceUIReceiver.kt - Broadcast receiver for VoiceUI intents
 * Handles all intent-based API calls
 */

package com.augmentalis.voiceui.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voiceui.VoiceUIModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Receives and processes VoiceUI intents from other apps
 * Direct implementation - no abstraction layers (VOS4)
 */
class VoiceUIReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "VoiceUIReceiver"
        
        // Intent action prefixes
        private const val ACTION_PREFIX = "com.augmentalis.voiceui.action."
        private const val BROADCAST_PREFIX = "com.augmentalis.voiceui.broadcast."
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.d(TAG, "Received intent: $action")
        
        // Get VoiceUI instance
        val voiceUI = VoiceUIModule.getInstance(context)
        
        // Ensure module is initialized
        if (!voiceUI.isReady()) {
            scope.launch {
                voiceUI.initialize()
                processIntent(context, voiceUI, intent)
            }
        } else {
            processIntent(context, voiceUI, intent)
        }
    }
    
    private fun processIntent(context: Context, voiceUI: VoiceUIModule, intent: Intent) {
        when (intent.action) {
            // Theme actions
            "${ACTION_PREFIX}THEME_CHANGE" -> {
                val themeName = intent.getStringExtra("theme_name") ?: "material"
                val animate = intent.getBooleanExtra("animate", true)
                voiceUI.themeEngine.setTheme(themeName, animate)
                broadcastThemeChanged(context, themeName)
            }
            
            "${ACTION_PREFIX}THEME_GET" -> {
                val currentTheme = voiceUI.themeEngine.getCurrentTheme()
                val response = Intent("${BROADCAST_PREFIX}THEME_CURRENT").apply {
                    putExtra("theme_name", currentTheme)
                }
                context.sendBroadcast(response)
            }
            
            "${ACTION_PREFIX}THEME_REGISTER" -> {
                val themeName = intent.getStringExtra("theme_name") ?: return
                val themeConfig = intent.getStringExtra("theme_config") ?: return
                voiceUI.themeEngine.registerCustomTheme(themeName, themeConfig)
            }
            
            // Gesture actions
            "${ACTION_PREFIX}GESTURE_ENABLE" -> {
                val gestureType = intent.getStringExtra("gesture_type") ?: "all"
                val enabled = intent.getBooleanExtra("enabled", true)
                voiceUI.gestureManager.enableGesture(gestureType, enabled)
            }
            
            "${ACTION_PREFIX}GESTURE_TRIGGER" -> {
                val gestureType = intent.getStringExtra("gesture_type") ?: "tap"
                val x = intent.getFloatExtra("x", 0f)
                val y = intent.getFloatExtra("y", 0f)
                voiceUI.gestureManager.triggerGesture(gestureType, x, y)
            }
            
            "${ACTION_PREFIX}GESTURE_CONFIG" -> {
                val sensitivity = intent.getFloatExtra("sensitivity", 0.8f)
                val multiTouch = intent.getBooleanExtra("multi_touch", true)
                voiceUI.gestureManager.configure(sensitivity, multiTouch)
            }
            
            // Window actions
            "${ACTION_PREFIX}WINDOW_CREATE" -> {
                val windowId = intent.getStringExtra("window_id") ?: UUID.randomUUID().toString()
                val title = intent.getStringExtra("title") ?: "Window"
                val width = intent.getIntExtra("width", 800)
                val height = intent.getIntExtra("height", 600)
                val x = intent.getFloatExtra("x", 0f)
                val y = intent.getFloatExtra("y", 0f)
                val z = intent.getFloatExtra("z", -2f)
                
                val createdId = voiceUI.windowManager.createWindow(
                    windowId, title, width, height, x, y, z
                )
                
                broadcastWindowCreated(context, createdId)
            }
            
            "${ACTION_PREFIX}WINDOW_SHOW" -> {
                val windowId = intent.getStringExtra("window_id") ?: return
                val animated = intent.getBooleanExtra("animated", true)
                voiceUI.windowManager.showWindow(windowId, animated)
            }
            
            "${ACTION_PREFIX}WINDOW_HIDE" -> {
                val windowId = intent.getStringExtra("window_id") ?: return
                val animated = intent.getBooleanExtra("animated", true)
                voiceUI.windowManager.hideWindow(windowId, animated)
            }
            
            "${ACTION_PREFIX}WINDOW_MOVE" -> {
                val windowId = intent.getStringExtra("window_id") ?: return
                val x = intent.getFloatExtra("x", 0f)
                val y = intent.getFloatExtra("y", 0f)
                val z = intent.getFloatExtra("z", -2f)
                voiceUI.windowManager.moveWindow(windowId, x, y, z)
            }
            
            "${ACTION_PREFIX}WINDOW_RESIZE" -> {
                val windowId = intent.getStringExtra("window_id") ?: return
                val width = intent.getIntExtra("width", 800)
                val height = intent.getIntExtra("height", 600)
                voiceUI.windowManager.resizeWindow(windowId, width, height)
            }
            
            // HUD actions
            "${ACTION_PREFIX}HUD_NOTIFY" -> {
                val message = intent.getStringExtra("message") ?: return
                val duration = intent.getIntExtra("duration", 2000)
                val position = intent.getStringExtra("position") ?: "TOP_CENTER"
                val priority = intent.getStringExtra("priority") ?: "NORMAL"
                
                voiceUI.hudSystem.showNotification(
                    message, duration, position, priority
                )
            }
            
            "${ACTION_PREFIX}HUD_UPDATE" -> {
                val overlayId = intent.getStringExtra("overlay_id") ?: return
                val content = intent.getStringExtra("content") ?: return
                voiceUI.hudSystem.updateOverlay(overlayId, content)
            }
            
            "${ACTION_PREFIX}HUD_TOGGLE" -> {
                val visible = intent.getBooleanExtra("visible", true)
                val fadeDuration = intent.getIntExtra("fade_duration", 300)
                voiceUI.hudSystem.toggleVisibility(visible, fadeDuration)
            }
            
            // Notification actions
            "${ACTION_PREFIX}NOTIFY_CUSTOM" -> {
                val notificationId = intent.getStringExtra("notification_id") ?: UUID.randomUUID().toString()
                val title = intent.getStringExtra("title") ?: "VoiceUI"
                val message = intent.getStringExtra("message") ?: ""
                val style = intent.getStringExtra("style") ?: "STANDARD"
                val actions = intent.getStringArrayExtra("actions") ?: emptyArray()
                
                voiceUI.notificationSystem.showCustomNotification(
                    notificationId, title, message, style, actions.toList()
                )
            }
            
            "${ACTION_PREFIX}NOTIFY_CLEAR" -> {
                val notificationId = intent.getStringExtra("notification_id") ?: "ALL"
                if (notificationId == "ALL") {
                    voiceUI.notificationSystem.clearAll()
                } else {
                    voiceUI.notificationSystem.clear(notificationId)
                }
            }
            
            // Voice command actions
            "${ACTION_PREFIX}VOICE_REGISTER" -> {
                val command = intent.getStringExtra("command") ?: return
                val action = intent.getStringExtra("action") ?: return
                val language = intent.getStringExtra("language") ?: "en-US"
                voiceUI.voiceCommandSystem.registerCommand(command, action, language)
            }
            
            "${ACTION_PREFIX}VOICE_PROCESS" -> {
                val audioData = intent.getByteArrayExtra("audio_data") ?: return
                val language = intent.getStringExtra("language") ?: "en-US"
                voiceUI.voiceCommandSystem.processAudio(audioData, language)
            }
            
            "${ACTION_PREFIX}VOICE_ENABLE" -> {
                val enabled = intent.getBooleanExtra("enabled", true)
                val wakeWord = intent.getStringExtra("wake_word") ?: "hey voice"
                voiceUI.voiceCommandSystem.setEnabled(enabled, wakeWord)
            }
            
            // Data visualization actions
            "${ACTION_PREFIX}CHART_CREATE" -> {
                val chartType = intent.getStringExtra("chart_type") ?: "LINE"
                val data = intent.getStringExtra("data") ?: return
                val title = intent.getStringExtra("title") ?: "Chart"
                
                val chartId = voiceUI.dataVisualization.createChart(chartType, data, title)
                
                val response = Intent("${BROADCAST_PREFIX}CHART_CREATED").apply {
                    putExtra("chart_id", chartId)
                }
                context.sendBroadcast(response)
            }
            
            "${ACTION_PREFIX}CHART_UPDATE" -> {
                val chartId = intent.getStringExtra("chart_id") ?: return
                val data = intent.getStringExtra("data") ?: return
                val animate = intent.getBooleanExtra("animate", true)
                voiceUI.dataVisualization.updateChart(chartId, data, animate)
            }
        }
    }
    
    // Broadcast helper methods
    private fun broadcastThemeChanged(context: Context, themeName: String) {
        val broadcast = Intent("${BROADCAST_PREFIX}THEME_CHANGED").apply {
            putExtra("theme_name", themeName)
            putExtra("timestamp", System.currentTimeMillis())
        }
        context.sendBroadcast(broadcast)
    }
    
    private fun broadcastWindowCreated(context: Context, windowId: String) {
        val broadcast = Intent("${BROADCAST_PREFIX}WINDOW_CREATED").apply {
            putExtra("window_id", windowId)
            putExtra("timestamp", System.currentTimeMillis())
        }
        context.sendBroadcast(broadcast)
    }
    
    private fun broadcastGestureDetected(context: Context, type: String, x: Float, y: Float) {
        val broadcast = Intent("${BROADCAST_PREFIX}GESTURE_DETECTED").apply {
            putExtra("gesture_type", type)
            putExtra("x", x)
            putExtra("y", y)
            putExtra("timestamp", System.currentTimeMillis())
        }
        context.sendBroadcast(broadcast)
    }
}