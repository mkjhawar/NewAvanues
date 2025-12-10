/**
 * DictationHandler.kt - Manages dictation functionality for the keyboard
 * 
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-07
 */
package com.augmentalis.voicekeyboard.voice

import android.content.Context
import android.os.CountDownTimer
import android.util.Log
import com.augmentalis.voicekeyboard.utils.IMEUtil
import com.augmentalis.voicekeyboard.utils.KeyboardConstants
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Handles dictation functionality including start/stop commands, timeout management,
 * and communication with VoiceAccessibility service
 * 
 * SOLID Principles:
 * - Single Responsibility: Only manages dictation state and flow
 * - Open/Closed: Extensible through callbacks and state flows
 * - Dependency Inversion: Depends on abstractions (Context, callbacks)
 */
class DictationHandler(
    private val context: Context,
    private val onDictationResult: (String) -> Unit,
    private val onDictationStateChanged: (Boolean) -> Unit
) {
    
    companion object {
        private const val TAG = "DictationHandler"
        private const val DEFAULT_TIMEOUT_MS = 5000L // 5 seconds default
    }
    
    // Dictation state management
    private val _isDictationActive = MutableStateFlow(false)
    val isDictationActive: StateFlow<Boolean> = _isDictationActive
    
    private val _dictationText = MutableStateFlow("")
    val dictationText: StateFlow<String> = _dictationText
    
    // Coroutine scope for async operations
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Timer for dictation timeout
    private var dictationTimer: CountDownTimer? = null
    
    // Configuration
    private var dictationTimeoutMs: Long = DEFAULT_TIMEOUT_MS
    private var startCommand: String = KeyboardConstants.DEFAULT_DICTATION_START_COMMAND
    private var stopCommand: String = KeyboardConstants.DEFAULT_DICTATION_STOP_COMMAND
    
    // Track keyboard visibility for dictation eligibility
    private var isKeyboardVisible = false
    
    /**
     * Initialize the dictation handler
     */
    fun initialize() {
        Log.d(TAG, "Initializing dictation handler")
        loadDictationPreferences()
    }
    
    /**
     * Update keyboard visibility status
     * Dictation only works when keyboard is visible
     */
    fun updateKeyboardVisibility(isVisible: Boolean) {
        isKeyboardVisible = isVisible
        
        // If keyboard becomes hidden while dictating, stop dictation
        if (!isVisible && _isDictationActive.value) {
            stopDictation()
        }
    }
    
    /**
     * Handle dictation start request
     * Returns true if dictation was started successfully
     */
    fun handleDictationStart(): Boolean {
        if (!isKeyboardVisible) {
            Log.w(TAG, "Cannot start dictation - keyboard not visible")
            return false
        }
        
        if (_isDictationActive.value) {
            Log.d(TAG, "Dictation already active")
            return true
        }
        
        Log.d(TAG, "Starting dictation")
        
        // Update state
        _isDictationActive.value = true
        _dictationText.value = ""
        
        // Notify keyboard service
        onDictationStateChanged(true)
        
        // Send status to VoiceAccessibility
        IMEUtil.sendDictationStatus(context, true)
        
        // Start timeout timer
        startDictationTimer()
        
        return true
    }
    
    /**
     * Handle dictation end request
     * Returns true if dictation was stopped successfully
     */
    fun handleDictationEnd(): Boolean {
        if (!_isDictationActive.value) {
            Log.d(TAG, "Dictation not active")
            return false
        }
        
        Log.d(TAG, "Stopping dictation")
        
        // Cancel timer
        cancelDictationTimer()
        
        // Process any pending text
        if (_dictationText.value.isNotEmpty()) {
            onDictationResult(_dictationText.value)
        }
        
        // Update state
        _isDictationActive.value = false
        _dictationText.value = ""
        
        // Notify keyboard service
        onDictationStateChanged(false)
        
        // Send status to VoiceAccessibility
        IMEUtil.sendDictationStatus(context, false)
        
        return true
    }
    
    /**
     * Stop dictation (alias for handleDictationEnd)
     */
    fun stopDictation() = handleDictationEnd()
    
    /**
     * Process voice command for dictation control
     */
    fun processVoiceCommand(command: String): Boolean {
        val normalizedCommand = command.lowercase().trim()
        
        return when {
            normalizedCommand == startCommand.lowercase() -> {
                handleDictationStart()
            }
            normalizedCommand == stopCommand.lowercase() -> {
                handleDictationEnd()
            }
            _isDictationActive.value -> {
                // If dictating, append the text
                appendDictationText(command)
                true
            }
            else -> false
        }
    }
    
    /**
     * Append text during dictation
     */
    fun appendDictationText(text: String) {
        if (!_isDictationActive.value) {
            Log.w(TAG, "Cannot append text - dictation not active")
            return
        }
        
        // Reset timer on new input
        restartDictationTimer()
        
        // Append text with proper spacing
        val currentText = _dictationText.value
        _dictationText.value = if (currentText.isEmpty()) {
            text
        } else {
            "$currentText $text"
        }
        
        // Send intermediate result to keyboard
        onDictationResult(_dictationText.value)
    }
    
    /**
     * Handle dictation timeout
     */
    private fun onDictationTimeout() {
        Log.d(TAG, "Dictation timeout reached")
        
        if (_isDictationActive.value) {
            // Auto-stop dictation on timeout
            handleDictationEnd()
        }
    }
    
    /**
     * Start dictation timeout timer
     */
    private fun startDictationTimer() {
        cancelDictationTimer()
        
        dictationTimer = object : CountDownTimer(dictationTimeoutMs, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsRemaining = millisUntilFinished / 1000
                Log.v(TAG, "Dictation timeout in $secondsRemaining seconds")
            }
            
            override fun onFinish() {
                onDictationTimeout()
            }
        }.start()
    }
    
    /**
     * Restart dictation timer (called when new input received)
     */
    private fun restartDictationTimer() {
        Log.v(TAG, "Restarting dictation timer")
        startDictationTimer()
    }
    
    /**
     * Cancel dictation timer
     */
    private fun cancelDictationTimer() {
        dictationTimer?.cancel()
        dictationTimer = null
    }
    
    /**
     * Load dictation preferences from storage
     */
    private fun loadDictationPreferences() {
        // TODO: Load from SharedPreferences or database
        // For now, use defaults
        dictationTimeoutMs = KeyboardConstants.DEFAULT_DICTATION_TIMEOUT * 1000L
        startCommand = KeyboardConstants.DEFAULT_DICTATION_START_COMMAND
        stopCommand = KeyboardConstants.DEFAULT_DICTATION_STOP_COMMAND
    }
    
    /**
     * Update dictation configuration
     */
    fun updateConfiguration(
        timeoutSeconds: Int = KeyboardConstants.DEFAULT_DICTATION_TIMEOUT,
        startCmd: String = KeyboardConstants.DEFAULT_DICTATION_START_COMMAND,
        stopCmd: String = KeyboardConstants.DEFAULT_DICTATION_STOP_COMMAND
    ) {
        dictationTimeoutMs = timeoutSeconds * 1000L
        startCommand = startCmd
        stopCommand = stopCmd
        
        Log.d(TAG, "Updated configuration - timeout: ${timeoutSeconds}s, start: $startCmd, stop: $stopCmd")
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        Log.d(TAG, "Destroying dictation handler")
        
        // Stop any active dictation
        if (_isDictationActive.value) {
            handleDictationEnd()
        }
        
        // Cancel timer
        cancelDictationTimer()
        
        // Cancel coroutines
        scope.cancel()
    }
}