/**
 * VoiceStateManager.kt - Centralized voice state management for all speech engines
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-04
 * 
 * Thread-safe voice state management with persistence support.
 * Replaces duplicate state handling in VivokaState, WhisperEngine, AndroidSTTEngine, etc.
 */
package com.augmentalis.voiceos.speech.engines.common

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Centralized voice state management for all speech recognition engines.
 * Provides thread-safe state management, timeout handling, and persistence.
 */
class VoiceStateManager(
    private val context: Context? = null,
    private val engineName: String = "VoiceEngine"
) {
    
    companion object {
        private const val TAG = "VoiceStateManager"
        private const val PREFS_NAME = "voice_state_prefs"
        private const val DEFAULT_VOICE_TIMEOUT_MINUTES = 5
        private const val DEFAULT_DICTATION_TIMEOUT_MS = 3000L
        private const val STATE_CHANGE_DEBOUNCE_MS = 100L
        
        // Persistence keys
        private const val KEY_VOICE_ENABLED = "voice_enabled"
        private const val KEY_VOICE_SLEEPING = "voice_sleeping"
        private const val KEY_DICTATION_ACTIVE = "dictation_active"
        private const val KEY_LAST_COMMAND_TIME = "last_command_time"
    }
    
    // Thread-safe state variables
    @Volatile private var _isVoiceEnabled = AtomicBoolean(true)
    @Volatile private var _isVoiceSleeping = AtomicBoolean(false)
    @Volatile private var _isInitialized = AtomicBoolean(false)
    @Volatile private var _isDictationActive = AtomicBoolean(false)
    @Volatile private var _lastExecutedCommandTime = AtomicLong(0L)
    
    // Read-write lock for complex state operations
    private val stateLock = ReentrantReadWriteLock()
    
    // StateFlow for reactive observation
    private val _voiceState = MutableStateFlow(VoiceState())
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()
    
    // Handler for timeout operations
    private val handler = Handler(Looper.getMainLooper())
    private var timeoutRunnable: Runnable? = null
    private var dictationTimeoutRunnable: Runnable? = null
    
    // Shared preferences for state persistence
    private val prefs: SharedPreferences? = context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // State change tracking
    private var lastStateChangeTime = AtomicLong(0L)
    
    // Callbacks
    private var onVoiceSleepCallback: (() -> Unit)? = null
    private var onVoiceWakeCallback: (() -> Unit)? = null
    private var onDictationStartCallback: (() -> Unit)? = null
    private var onDictationEndCallback: (() -> Unit)? = null
    private var onStateChangeCallback: ((VoiceState) -> Unit)? = null
    
    /**
     * Data class representing the complete voice state
     */
    data class VoiceState(
        val isVoiceEnabled: Boolean = true,
        val isVoiceSleeping: Boolean = false,
        val isInitialized: Boolean = false,
        val isDictationActive: Boolean = false,
        val lastExecutedCommandTime: Long = 0L,
        val stateTimestamp: Long = System.currentTimeMillis()
    ) {
        /**
         * Check if voice system is ready to accept commands
         */
        fun isReady(): Boolean = isInitialized && isVoiceEnabled && !isVoiceSleeping
        
        /**
         * Check if voice system can process dictation
         */
        fun canDictate(): Boolean = isReady() && !isDictationActive
        
        /**
         * Get time since last command in milliseconds
         */
        fun getTimeSinceLastCommand(): Long = 
            if (lastExecutedCommandTime > 0) System.currentTimeMillis() - lastExecutedCommandTime else -1L
    }
    
    init {
        // Load persisted state if available
        loadPersistedState()
        
        // Initialize state flow
        updateStateFlow()
        
        Log.d(TAG, "[$engineName] VoiceStateManager initialized")
    }
    
    /**
     * Initialize the voice system
     */
    fun initialize(): Boolean {
        return stateLock.write {
            if (_isInitialized.get()) {
                Log.w(TAG, "[$engineName] Already initialized")
                return@write false
            }
            
            _isInitialized.set(true)
            updateStateFlow()
            persistState()
            
            Log.i(TAG, "[$engineName] Voice system initialized")
            true
        }
    }
    
    /**
     * Update command execution time and reset timeout
     */
    fun updateCommandExecutionTime() {
        val currentTime = System.currentTimeMillis()
        _lastExecutedCommandTime.set(currentTime)
        
        // Reset voice sleep timeout if voice is enabled
        if (_isVoiceEnabled.get() && !_isVoiceSleeping.get()) {
            resetVoiceTimeout()
        }
        
        updateStateFlow()
        persistState()
        
        Log.d(TAG, "[$engineName] Command execution time updated")
    }
    
    /**
     * Check if voice system should timeout based on inactivity
     */
    fun shouldTimeout(minutes: Int = DEFAULT_VOICE_TIMEOUT_MINUTES): Boolean {
        val lastCommandTime = _lastExecutedCommandTime.get()
        if (lastCommandTime <= 0) return false
        
        val timeoutMs = minutes * 60 * 1000L
        val timeSinceLastCommand = System.currentTimeMillis() - lastCommandTime
        
        return timeSinceLastCommand > timeoutMs
    }
    
    /**
     * Enter voice sleep mode
     */
    fun enterSleepMode(): Boolean {
        return stateLock.write {
            if (!_isVoiceEnabled.get() || _isVoiceSleeping.get()) {
                Log.w(TAG, "[$engineName] Cannot enter sleep mode: enabled=${_isVoiceEnabled.get()}, sleeping=${_isVoiceSleeping.get()}")
                return@write false
            }
            
            _isVoiceSleeping.set(true)
            cancelVoiceTimeout()
            
            updateStateFlow()
            persistState()
            
            // Trigger callback
            onVoiceSleepCallback?.invoke()
            
            Log.i(TAG, "[$engineName] Entered voice sleep mode")
            true
        }
    }
    
    /**
     * Exit voice sleep mode
     */
    fun exitSleepMode(): Boolean {
        return stateLock.write {
            if (!_isVoiceSleeping.get()) {
                Log.w(TAG, "[$engineName] Not currently sleeping")
                return@write false
            }
            
            _isVoiceSleeping.set(false)
            _lastExecutedCommandTime.set(System.currentTimeMillis())
            
            // Restart timeout if voice is enabled
            if (_isVoiceEnabled.get()) {
                resetVoiceTimeout()
            }
            
            updateStateFlow()
            persistState()
            
            // Trigger callback
            onVoiceWakeCallback?.invoke()
            
            Log.i(TAG, "[$engineName] Exited voice sleep mode")
            true
        }
    }
    
    /**
     * Enter dictation mode
     */
    fun enterDictationMode(): Boolean {
        return stateLock.write {
            if (!_isInitialized.get() || !_isVoiceEnabled.get() || _isVoiceSleeping.get()) {
                Log.w(TAG, "[$engineName] Cannot enter dictation mode: initialized=${_isInitialized.get()}, enabled=${_isVoiceEnabled.get()}, sleeping=${_isVoiceSleeping.get()}")
                return@write false
            }
            
            if (_isDictationActive.get()) {
                Log.w(TAG, "[$engineName] Already in dictation mode")
                return@write false
            }
            
            _isDictationActive.set(true)
            startDictationTimeout()
            
            updateStateFlow()
            persistState()
            
            // Trigger callback
            onDictationStartCallback?.invoke()
            
            Log.i(TAG, "[$engineName] Entered dictation mode")
            true
        }
    }
    
    /**
     * Exit dictation mode
     */
    fun exitDictationMode(): Boolean {
        return stateLock.write {
            if (!_isDictationActive.get()) {
                Log.w(TAG, "[$engineName] Not currently in dictation mode")
                return@write false
            }
            
            _isDictationActive.set(false)
            cancelDictationTimeout()
            
            updateStateFlow()
            persistState()
            
            // Trigger callback
            onDictationEndCallback?.invoke()
            
            Log.i(TAG, "[$engineName] Exited dictation mode")
            true
        }
    }
    
    /**
     * Set voice enabled/disabled state
     */
    fun setVoiceEnabled(enabled: Boolean): Boolean {
        return stateLock.write {
            if (_isVoiceEnabled.get() == enabled) {
                Log.d(TAG, "[$engineName] Voice already ${if (enabled) "enabled" else "disabled"}")
                return@write false
            }
            
            _isVoiceEnabled.set(enabled)
            
            if (!enabled) {
                // Disable voice system
                _isVoiceSleeping.set(false)
                _isDictationActive.set(false)
                cancelVoiceTimeout()
                cancelDictationTimeout()
            } else {
                // Enable voice system
                _lastExecutedCommandTime.set(System.currentTimeMillis())
                resetVoiceTimeout()
            }
            
            updateStateFlow()
            persistState()
            
            Log.i(TAG, "[$engineName] Voice ${if (enabled) "enabled" else "disabled"}")
            true
        }
    }
    
    /**
     * Reset dictation timeout
     */
    fun resetDictationTimeout(timeoutMs: Long = DEFAULT_DICTATION_TIMEOUT_MS) {
        if (_isDictationActive.get()) {
            startDictationTimeout(timeoutMs)
        }
    }
    
    /**
     * Get current voice state (thread-safe read)
     */
    fun getCurrentState(): VoiceState {
        return stateLock.read {
            VoiceState(
                isVoiceEnabled = _isVoiceEnabled.get(),
                isVoiceSleeping = _isVoiceSleeping.get(),
                isInitialized = _isInitialized.get(),
                isDictationActive = _isDictationActive.get(),
                lastExecutedCommandTime = _lastExecutedCommandTime.get(),
                stateTimestamp = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Individual state getters (thread-safe)
     */
    fun isVoiceEnabled(): Boolean = _isVoiceEnabled.get()
    fun isVoiceSleeping(): Boolean = _isVoiceSleeping.get()
    fun isInitialized(): Boolean = _isInitialized.get()
    fun isDictationActive(): Boolean = _isDictationActive.get()
    fun getLastExecutedCommandTime(): Long = _lastExecutedCommandTime.get()
    
    /**
     * Convenience methods
     */
    fun isReady(): Boolean = getCurrentState().isReady()
    fun canDictate(): Boolean = getCurrentState().canDictate()
    fun getTimeSinceLastCommand(): Long = getCurrentState().getTimeSinceLastCommand()
    
    /**
     * Set callbacks for state changes
     */
    fun setCallbacks(
        onVoiceSleep: (() -> Unit)? = null,
        onVoiceWake: (() -> Unit)? = null,
        onDictationStart: (() -> Unit)? = null,
        onDictationEnd: (() -> Unit)? = null,
        onStateChange: ((VoiceState) -> Unit)? = null
    ) {
        onVoiceSleepCallback = onVoiceSleep
        onVoiceWakeCallback = onVoiceWake
        onDictationStartCallback = onDictationStart
        onDictationEndCallback = onDictationEnd
        onStateChangeCallback = onStateChange
    }
    
    /**
     * Reset all state to defaults
     */
    fun reset() {
        stateLock.write {
            _isVoiceEnabled.set(true)
            _isVoiceSleeping.set(false)
            _isInitialized.set(false)
            _isDictationActive.set(false)
            _lastExecutedCommandTime.set(0L)
            
            cancelVoiceTimeout()
            cancelDictationTimeout()
            
            updateStateFlow()
            persistState()
            
            Log.i(TAG, "[$engineName] Voice state reset to defaults")
        }
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        stateLock.write {
            cancelVoiceTimeout()
            cancelDictationTimeout()
            
            // Clear callbacks
            onVoiceSleepCallback = null
            onVoiceWakeCallback = null
            onDictationStartCallback = null
            onDictationEndCallback = null
            onStateChangeCallback = null
            
            Log.i(TAG, "[$engineName] VoiceStateManager destroyed")
        }
    }
    
    // Private helper methods
    
    /**
     * Update the StateFlow with current state (debounced)
     */
    private fun updateStateFlow() {
        val currentTime = System.currentTimeMillis()
        val lastChange = lastStateChangeTime.get()
        
        // Debounce rapid state changes
        if (currentTime - lastChange < STATE_CHANGE_DEBOUNCE_MS) {
            return
        }
        
        lastStateChangeTime.set(currentTime)
        
        val newState = VoiceState(
            isVoiceEnabled = _isVoiceEnabled.get(),
            isVoiceSleeping = _isVoiceSleeping.get(),
            isInitialized = _isInitialized.get(),
            isDictationActive = _isDictationActive.get(),
            lastExecutedCommandTime = _lastExecutedCommandTime.get(),
            stateTimestamp = currentTime
        )
        
        _voiceState.value = newState
        onStateChangeCallback?.invoke(newState)
    }
    
    /**
     * Start/reset voice timeout timer
     */
    private fun resetVoiceTimeout(minutes: Int = DEFAULT_VOICE_TIMEOUT_MINUTES) {
        cancelVoiceTimeout()
        
        timeoutRunnable = Runnable {
            if (_isVoiceEnabled.get() && !_isVoiceSleeping.get()) {
                Log.i(TAG, "[$engineName] Voice timeout after ${minutes}m inactivity")
                enterSleepMode()
            }
        }
        
        handler.postDelayed(timeoutRunnable!!, minutes * 60 * 1000L)
    }
    
    /**
     * Cancel voice timeout timer
     */
    private fun cancelVoiceTimeout() {
        timeoutRunnable?.let {
            handler.removeCallbacks(it)
            timeoutRunnable = null
        }
    }
    
    /**
     * Start dictation timeout timer
     */
    private fun startDictationTimeout(timeoutMs: Long = DEFAULT_DICTATION_TIMEOUT_MS) {
        cancelDictationTimeout()
        
        dictationTimeoutRunnable = Runnable {
            if (_isDictationActive.get()) {
                Log.d(TAG, "[$engineName] Dictation timeout after ${timeoutMs}ms silence")
                exitDictationMode()
            }
        }
        
        handler.postDelayed(dictationTimeoutRunnable!!, timeoutMs)
    }
    
    /**
     * Cancel dictation timeout timer
     */
    private fun cancelDictationTimeout() {
        dictationTimeoutRunnable?.let {
            handler.removeCallbacks(it)
            dictationTimeoutRunnable = null
        }
    }
    
    /**
     * Load persisted state from SharedPreferences
     */
    private fun loadPersistedState() {
        prefs?.let { preferences ->
            _isVoiceEnabled.set(preferences.getBoolean(KEY_VOICE_ENABLED, true))
            _isVoiceSleeping.set(preferences.getBoolean(KEY_VOICE_SLEEPING, false))
            _isDictationActive.set(preferences.getBoolean(KEY_DICTATION_ACTIVE, false))
            _lastExecutedCommandTime.set(preferences.getLong(KEY_LAST_COMMAND_TIME, 0L))
            
            Log.d(TAG, "[$engineName] Loaded persisted state")
        }
    }
    
    /**
     * Persist current state to SharedPreferences
     */
    private fun persistState() {
        prefs?.edit()?.apply {
            putBoolean(KEY_VOICE_ENABLED, _isVoiceEnabled.get())
            putBoolean(KEY_VOICE_SLEEPING, _isVoiceSleeping.get())
            putBoolean(KEY_DICTATION_ACTIVE, _isDictationActive.get())
            putLong(KEY_LAST_COMMAND_TIME, _lastExecutedCommandTime.get())
            apply()
        }
    }
}