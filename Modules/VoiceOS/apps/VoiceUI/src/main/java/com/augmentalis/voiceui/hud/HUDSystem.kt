package com.augmentalis.voiceui.hud

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * HUDSystem - Minimal HUD system for VoiceOS compatibility
 * Following SRP and zero-overhead principles
 */
class HUDSystem(private val context: Context) {
    companion object {
        private const val TAG = "HUDSystem"
    }
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isVisible = false
    private var currentFPS = 60.0f
    
    fun setVisible(visible: Boolean) {
        isVisible = visible
        Log.d(TAG, "HUD visibility: $visible")
    }
    
    fun toggleVisibility() {
        setVisible(!isVisible)
    }
    
    fun isVisible(): Boolean = isVisible
    
    fun getCurrentFPS(): Float = currentFPS
    
    fun showNotification(message: String) {
        Log.d(TAG, "HUD notification: $message")
    }
    
    fun removeElement(elementId: String) {
        Log.d(TAG, "Removing HUD element: $elementId")
    }
    
    fun shutdown() {
        Log.d(TAG, "HUD system shutdown")
    }
}