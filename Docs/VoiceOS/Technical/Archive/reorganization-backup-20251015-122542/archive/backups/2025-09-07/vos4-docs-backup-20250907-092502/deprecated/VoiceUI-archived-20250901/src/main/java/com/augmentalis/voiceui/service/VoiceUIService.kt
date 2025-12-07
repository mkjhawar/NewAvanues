/**
 * VoiceUIService.kt - Main service for VoiceUI module
 * Provides bindable service for complex operations
 */

package com.augmentalis.voiceui.service

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import com.augmentalis.voiceui.VoiceUIModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Service providing direct access to VoiceUI components
 * Follows VOS4 direct access pattern - no interfaces
 */
class VoiceUIService : Service() {
    
    companion object {
        private const val TAG = "VoiceUIService"
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var voiceUI: VoiceUIModule
    
    inner class VoiceUIBinder : Binder() {
        // Direct property access - VOS4 pattern
        val themeEngine get() = voiceUI.themeEngine
        val gestureManager get() = voiceUI.gestureManager
        val windowManager get() = voiceUI.windowManager
        val hudSystem get() = voiceUI.hudSystem
        val notificationSystem get() = voiceUI.notificationSystem
        val voiceCommandSystem get() = voiceUI.voiceCommandSystem
        val dataVisualization get() = voiceUI.dataVisualization
        
        // Convenience methods
        fun setTheme(themeName: String) {
            voiceUI.setTheme(themeName)
        }
        
        fun showNotification(message: String, duration: Int = 2000) {
            voiceUI.hudSystem.showNotification(message, duration)
        }
        
        fun createWindow(windowConfig: Bundle): String {
            val title = windowConfig.getString("title", "Window")
            val width = windowConfig.getInt("width", 800)
            val height = windowConfig.getInt("height", 600)
            val x = windowConfig.getFloat("x", 0f)
            val y = windowConfig.getFloat("y", 0f)
            val z = windowConfig.getFloat("z", -2f)
            
            return voiceUI.windowManager.createSpatialWindow(
                title = title,
                width = width,
                height = height,
                x = x,
                y = y,
                z = z
            )
        }
        
        fun processGesture(gestureData: Bundle) {
            val type = gestureData.getString("type", "tap")
            val x = gestureData.getFloat("x", 0f)
            val y = gestureData.getFloat("y", 0f)
            
            voiceUI.gestureManager.processGesture(type, x, y)
        }
        
        fun registerVoiceCommand(command: String, action: String) {
            voiceUI.voiceCommandSystem.registerCommand(command, action)
        }
    }
    
    private val binder = VoiceUIBinder()
    
    override fun onCreate() {
        super.onCreate()
        voiceUI = VoiceUIModule.getInstance(this)
        
        scope.launch {
            val initialized = voiceUI.initialize()
            Log.d(TAG, "VoiceUI initialized: $initialized")
        }
    }
    
    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Service bound: ${intent.action}")
        return binder
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { handleIntent(it) }
        return START_STICKY
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            "com.augmentalis.voiceui.service.WINDOW_SERVICE" -> {
                Log.d(TAG, "Window service requested")
            }
            "com.augmentalis.voiceui.service.VOICE_SERVICE" -> {
                Log.d(TAG, "Voice service requested")
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.launch {
            voiceUI.shutdown()
        }
    }
}