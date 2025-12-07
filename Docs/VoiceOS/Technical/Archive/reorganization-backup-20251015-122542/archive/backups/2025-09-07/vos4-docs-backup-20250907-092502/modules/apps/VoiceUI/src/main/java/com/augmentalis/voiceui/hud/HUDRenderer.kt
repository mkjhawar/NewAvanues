package com.augmentalis.voiceui.hud

import android.content.Context
import android.util.Log

/**
 * HUDRenderer - Minimal HUD renderer for VoiceOS compatibility
 * Following SRP and zero-overhead principles
 */
class HUDRenderer(private val context: Context) {
    companion object {
        private const val TAG = "HUDRenderer"
    }
    
    fun render() {
        Log.d(TAG, "HUD rendering")
    }
    
    fun shutdown() {
        Log.d(TAG, "HUD renderer shutdown")
    }
}