/**
 * AudioStateManager.kt - Unified audio and recognition state management
 * 
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-28
 * 
 * Consolidated state management, mode switching, and silence detection
 * Reduces ~200 lines of duplicated code across engines to ~50 lines
 */
package com.augmentalis.voiceos.speech.engines.common

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Centralized audio and recognition state management for all engines.
 * Handles mode switching, silence detection, and voice sleep functionality.
 */
class AudioStateManager(private val engineName: String) {
    
    companion object {
        private const val TAG = "AudioStateManager"
        private const val DEFAULT_SILENCE_TIMEOUT = 2000L // 2 seconds
        private const val DEFAULT_VOICE_TIMEOUT = 300000L // 5 minutes
        private const val COMMAND_COOLDOWN = 500L // 500ms between commands
    }
    
    // Core state flags (thread-safe)
    private val _isListening = AtomicBoolean(false)
    private val _isDictationActive = AtomicBoolean(false)
    private val _isVoiceEnabled = AtomicBoolean(true)
    private val _isVoiceSleeping = AtomicBoolean(false)
    private val _isPaused = AtomicBoolean(false)
    
    // State flows for observers
    private val _recognitionMode = MutableStateFlow(RecognitionMode.COMMAND)
    val recognitionMode: StateFlow<RecognitionMode> = _recognitionMode.asStateFlow()
    
    private val _audioState = MutableStateFlow(AudioState.IDLE)
    val audioState: StateFlow<AudioState> = _audioState.asStateFlow()
    
    // Timing tracking
    private val lastCommandTime = AtomicLong(0)
    private val lastActivityTime = AtomicLong(System.currentTimeMillis())
    private val sessionStartTime = AtomicLong(0)
    
    // Handler for silence detection
    private val handler = Handler(Looper.getMainLooper())
    private var silenceRunnable: Runnable? = null
    private var voiceSleepRunnable: Runnable? = null
    
    // Callbacks
    private var onSilenceDetected: (() -> Unit)? = null
    private var onVoiceSleep: (() -> Unit)? = null
    private var onModeChanged: ((RecognitionMode) -> Unit)? = null
    
    enum class RecognitionMode {
        COMMAND,        // Command recognition mode
        DICTATION,      // Free speech/dictation mode
        HYBRID,         // Both command and dictation
        WAKE_WORD,      // Wake word detection only
        CONTINUOUS      // Continuous recognition
    }
    
    enum class AudioState {
        IDLE,           // Not listening
        LISTENING,      // Actively listening
        PROCESSING,     // Processing audio
        PAUSED,         // Temporarily paused
        SLEEPING,       // Voice sleep mode
        ERROR           // Error state
    }
    
    /**
     * Start listening
     */
    fun startListening(): Boolean {
        if (_isListening.get()) {
            Log.w(TAG, "[$engineName] Already listening")
            return false
        }
        
        _isListening.set(true)
        _isPaused.set(false)
        sessionStartTime.set(System.currentTimeMillis())
        lastActivityTime.set(System.currentTimeMillis())
        
        _audioState.value = AudioState.LISTENING
        
        // Start voice sleep timer if enabled
        if (_isVoiceEnabled.get()) {
            startVoiceSleepTimer()
        }
        
        Log.i(TAG, "[$engineName] Started listening in ${_recognitionMode.value} mode")
        return true
    }
    
    /**
     * Stop listening
     */
    fun stopListening(): Boolean {
        if (!_isListening.get()) {
            Log.w(TAG, "[$engineName] Not currently listening")
            return false
        }
        
        _isListening.set(false)
        _isDictationActive.set(false)
        cancelSilenceDetection()
        cancelVoiceSleepTimer()
        
        _audioState.value = AudioState.IDLE
        
        val sessionDuration = System.currentTimeMillis() - sessionStartTime.get()
        Log.i(TAG, "[$engineName] Stopped listening after ${sessionDuration / 1000}s")
        return true
    }
    
    /**
     * Pause listening temporarily
     */
    fun pauseListening() {
        if (_isListening.get() && !_isPaused.get()) {
            _isPaused.set(true)
            _audioState.value = AudioState.PAUSED
            cancelSilenceDetection()
            Log.d(TAG, "[$engineName] Paused listening")
        }
    }
    
    /**
     * Resume listening
     */
    fun resumeListening() {
        if (_isListening.get() && _isPaused.get()) {
            _isPaused.set(false)
            _audioState.value = AudioState.LISTENING
            lastActivityTime.set(System.currentTimeMillis())
            Log.d(TAG, "[$engineName] Resumed listening")
        }
    }

    /**
     * Start recording (alias for startListening for audio processing components)
     */
    fun startRecording(): Boolean {
        return startListening()
    }

    /**
     * Stop recording (alias for stopListening for audio processing components)
     */
    fun stopRecording(): Boolean {
        return stopListening()
    }
    
    /**
     * Switch recognition mode
     */
    fun switchMode(newMode: RecognitionMode): Boolean {
        val oldMode = _recognitionMode.value
        
        if (oldMode == newMode) {
            Log.d(TAG, "[$engineName] Already in $newMode mode")
            return false
        }
        
        // Handle mode-specific transitions
        when (newMode) {
            RecognitionMode.DICTATION -> {
                _isDictationActive.set(true)
                startSilenceDetection()
            }
            RecognitionMode.COMMAND -> {
                _isDictationActive.set(false)
                cancelSilenceDetection()
            }
            RecognitionMode.HYBRID -> {
                // Both command and dictation active
                _isDictationActive.set(true)
            }
            RecognitionMode.WAKE_WORD -> {
                _isDictationActive.set(false)
                cancelSilenceDetection()
            }
            RecognitionMode.CONTINUOUS -> {
                _isDictationActive.set(true)
            }
        }
        
        _recognitionMode.value = newMode
        onModeChanged?.invoke(newMode)
        
        Log.i(TAG, "[$engineName] Switched mode: $oldMode -> $newMode")
        return true
    }
    
    /**
     * Start silence detection for dictation mode
     */
    fun startSilenceDetection(timeout: Long = DEFAULT_SILENCE_TIMEOUT) {
        cancelSilenceDetection()
        
        val runnable = Runnable {
            if (_isDictationActive.get()) {
                Log.d(TAG, "[$engineName] Silence detected after ${timeout}ms")
                onSilenceDetected?.invoke()
                _isDictationActive.set(false)
                switchMode(RecognitionMode.COMMAND)
            }
        }
        silenceRunnable = runnable

        handler.postDelayed(runnable, timeout)
        Log.d(TAG, "[$engineName] Started silence detection (${timeout}ms)")
    }
    
    /**
     * Cancel silence detection
     */
    fun cancelSilenceDetection() {
        silenceRunnable?.let {
            handler.removeCallbacks(it)
            silenceRunnable = null
            Log.d(TAG, "[$engineName] Cancelled silence detection")
        }
    }
    
    /**
     * Reset silence detection timer
     */
    fun resetSilenceTimer(timeout: Long = DEFAULT_SILENCE_TIMEOUT) {
        if (_isDictationActive.get()) {
            startSilenceDetection(timeout)
        }
    }
    
    /**
     * Record command execution
     */
    fun recordCommandExecution(command: String): Boolean {
        val currentTime = System.currentTimeMillis()
        val timeSinceLastCommand = currentTime - lastCommandTime.get()
        
        // Check cooldown period
        if (timeSinceLastCommand < COMMAND_COOLDOWN) {
            Log.w(TAG, "[$engineName] Command ignored (cooldown): $command")
            return false
        }
        
        lastCommandTime.set(currentTime)
        lastActivityTime.set(currentTime)
        
        // Reset voice sleep timer on activity
        if (_isVoiceEnabled.get() && !_isVoiceSleeping.get()) {
            startVoiceSleepTimer()
        }
        
        Log.d(TAG, "[$engineName] Command executed: $command")
        return true
    }
    
    /**
     * Start voice sleep timer
     */
    private fun startVoiceSleepTimer(timeout: Long = DEFAULT_VOICE_TIMEOUT) {
        cancelVoiceSleepTimer()
        
        val runnable = Runnable {
            if (_isVoiceEnabled.get() && !_isVoiceSleeping.get()) {
                _isVoiceSleeping.set(true)
                _audioState.value = AudioState.SLEEPING
                Log.i(TAG, "[$engineName] Entering voice sleep after ${timeout / 1000}s inactivity")
                onVoiceSleep?.invoke()
            }
        }
        voiceSleepRunnable = runnable

        handler.postDelayed(runnable, timeout)
    }
    
    /**
     * Cancel voice sleep timer
     */
    private fun cancelVoiceSleepTimer() {
        voiceSleepRunnable?.let {
            handler.removeCallbacks(it)
            voiceSleepRunnable = null
        }
    }
    
    /**
     * Wake from voice sleep
     */
    fun wakeFromSleep() {
        if (_isVoiceSleeping.get()) {
            _isVoiceSleeping.set(false)
            lastActivityTime.set(System.currentTimeMillis())
            
            if (_isListening.get()) {
                _audioState.value = AudioState.LISTENING
                startVoiceSleepTimer()
            }
            
            Log.i(TAG, "[$engineName] Woke from voice sleep")
        }
    }
    
    /**
     * Set voice enabled/disabled
     */
    fun setVoiceEnabled(enabled: Boolean) {
        _isVoiceEnabled.set(enabled)
        
        if (!enabled) {
            cancelVoiceSleepTimer()
            _isVoiceSleeping.set(false)
        } else if (_isListening.get()) {
            startVoiceSleepTimer()
        }
        
        Log.i(TAG, "[$engineName] Voice ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Register callbacks
     */
    fun setCallbacks(
        onSilence: (() -> Unit)? = null,
        onSleep: (() -> Unit)? = null,
        onMode: ((RecognitionMode) -> Unit)? = null
    ) {
        onSilenceDetected = onSilence
        onVoiceSleep = onSleep
        onModeChanged = onMode
    }
    
    /**
     * Get current states
     */
    fun isListening() = _isListening.get()
    fun isDictationActive() = _isDictationActive.get()
    fun isVoiceEnabled() = _isVoiceEnabled.get()
    fun isVoiceSleeping() = _isVoiceSleeping.get()
    fun isPaused() = _isPaused.get()
    
    /**
     * Get timing information
     */
    fun getLastCommandTime() = lastCommandTime.get()
    fun getLastActivityTime() = lastActivityTime.get()
    fun getSessionDuration() = if (sessionStartTime.get() > 0) {
        System.currentTimeMillis() - sessionStartTime.get()
    } else 0L
    
    /**
     * Reset all states
     */
    fun reset() {
        stopListening()
        _isVoiceEnabled.set(true)
        _isVoiceSleeping.set(false)
        _recognitionMode.value = RecognitionMode.COMMAND
        _audioState.value = AudioState.IDLE
        lastCommandTime.set(0)
        lastActivityTime.set(System.currentTimeMillis())
        sessionStartTime.set(0)
        Log.d(TAG, "[$engineName] State reset")
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        stopListening()
        cancelSilenceDetection()
        cancelVoiceSleepTimer()
        onSilenceDetected = null
        onVoiceSleep = null
        onModeChanged = null
        Log.i(TAG, "[$engineName] AudioStateManager destroyed")
    }
}