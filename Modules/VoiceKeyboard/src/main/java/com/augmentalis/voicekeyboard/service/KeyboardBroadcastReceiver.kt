/**
 * KeyboardBroadcastReceiver.kt - Handles incoming broadcast commands for the keyboard
 * 
 * Author: Manoj Jhawar
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.voicekeyboard.utils.KeyboardConstants

/**
 * Receives broadcast commands from VoiceAccessibility and other components
 * Routes commands to the active keyboard service instance
 */
class KeyboardBroadcastReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "KeyboardBroadcastReceiver"
        
        // Static reference to active keyboard service
        private var activeKeyboardService: VoiceKeyboardService? = null
        
        fun setActiveService(service: VoiceKeyboardService?) {
            activeKeyboardService = service
        }
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        Log.d(TAG, "Received broadcast: $action")
        
        // Forward to active keyboard service if available
        activeKeyboardService?.let { service ->
            when (action) {
                KeyboardConstants.ACTION_VOICE_KEY_CODE -> {
                    val keyCode = intent.getIntExtra("keyCode", 0)
                    service.handleVoiceKeyCode(keyCode)
                }
                
                KeyboardConstants.ACTION_VOICE_KEY_COMMAND -> {
                    val command = intent.getStringExtra(KeyboardConstants.KEY_COMMAND)
                    command?.let { service.handleVoiceCommandString(it) }
                }
                
                KeyboardConstants.ACTION_CLOSE_COMMAND -> {
                    service.handleCloseCommand()
                }
                
                KeyboardConstants.ACTION_VOICE_SWITCH_KEYBOARD -> {
                    service.handleSwitchKeyboard()
                }
                
                KeyboardConstants.ACTION_VOICE_COMMAND_SHOW_INPUT -> {
                    service.handleShowInput()
                }
                
                KeyboardConstants.ACTION_FREE_SPEECH_COMMAND -> {
                    service.handleFreeSpeechCommand()
                }
                
                KeyboardConstants.ACTION_LAUNCH_DICTATION -> {
                    service.handleLaunchDictation()
                }
                
                else -> {
                    Log.w(TAG, "Unknown action: $action")
                }
            }
        } ?: Log.w(TAG, "No active keyboard service to handle broadcast")
    }
}