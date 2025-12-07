package com.augmentalis.devicemanager.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Main audio service facade that orchestrates all audio functionality
 */
class AudioService(private val context: Context) {
    
    companion object {
        private const val TAG = "AudioService"
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // Sub-components
    val routing = AudioRouting(context)
    val enhancement = AudioEnhancement()
    val effects = AudioEffects()
    val spatial = SpatialAudio(context)
    val capture = AudioCapture(context)
    
    // Audio focus
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentFocusType: Int = AudioManager.AUDIOFOCUS_NONE
    private val focusChangeListeners = mutableListOf<AudioFocusChangeListener>()
    
    // Audio focus state
    private val _audioFocusState = MutableStateFlow(AudioFocusState())
    val audioFocusState: StateFlow<AudioFocusState> = _audioFocusState.asStateFlow()
    
    /**
     * Audio focus state data class
     */
    data class AudioFocusState(
        val hasAudioFocus: Boolean = false,
        val focusType: Int = AudioManager.AUDIOFOCUS_NONE,
        val isDucked: Boolean = false,
        val isPaused: Boolean = false,
        val abandonReason: String? = null
    )
    
    /**
     * Audio focus change listener interface
     */
    interface AudioFocusChangeListener {
        fun onAudioFocusGained(focusType: Int)
        fun onAudioFocusLost()
        fun onAudioFocusLostTransient()
        fun onAudioFocusLostTransientCanDuck()
    }
    
    /**
     * Configure audio for voice-first interaction
     */
    fun configureForVoice(audioSessionId: Int) {
        // Set audio mode
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        
        // Enable voice enhancements
        enhancement.applyConfig(audioSessionId, EnhancementConfig(
            echoCancellation = true,
            noiseSuppression = true,
            automaticGainControl = true
        ))
        
        // Request audio focus
        requestAudioFocus(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            AudioAttributes.USAGE_VOICE_COMMUNICATION,
            AudioAttributes.CONTENT_TYPE_SPEECH
        )
        
        Log.d(TAG, "Configured for voice interaction")
    }
    
    /**
     * Configure audio for media playback
     */
    fun configureForMedia(audioSessionId: Int) {
        // Set audio mode
        audioManager.mode = AudioManager.MODE_NORMAL
        
        // Enable spatial audio if available
        spatial.enable(audioSessionId)
        
        // Apply media effects
        effects.applyConfig(audioSessionId, EffectConfig(
            bassBoostStrength = 300,
            equalizerPreset = EqualizerPreset.FLAT
        ))
        
        // Request audio focus
        requestAudioFocus(
            AudioManager.AUDIOFOCUS_GAIN,
            AudioAttributes.USAGE_MEDIA,
            AudioAttributes.CONTENT_TYPE_MUSIC
        )
        
        Log.d(TAG, "Configured for media playback")
    }
    
    /**
     * Configure audio for recording
     */
    fun configureForRecording(audioSessionId: Int) {
        // Set audio mode
        audioManager.mode = AudioManager.MODE_NORMAL
        
        // Enable recording enhancements
        enhancement.applyConfig(audioSessionId, EnhancementConfig(
            echoCancellation = false,
            noiseSuppression = true,
            automaticGainControl = true
        ))
        
        Log.d(TAG, "Configured for recording")
    }
    
    /**
     * Apply audio profile
     */
    fun applyProfile(profile: AudioProfile, audioSessionId: Int) {
        when (profile) {
            AudioProfile.VOICE_CALL,
            AudioProfile.VIDEO_CALL -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                routing.setSpeakerphone(profile == AudioProfile.VIDEO_CALL)
                enhancement.enableEchoCancellation(audioSessionId)
            }
            AudioProfile.VOICE_RECOGNITION -> {
                configureForVoice(audioSessionId)
            }
            AudioProfile.MEDIA_PLAYBACK -> {
                configureForMedia(audioSessionId)
            }
            AudioProfile.RECORDING -> {
                configureForRecording(audioSessionId)
            }
            AudioProfile.GAMING -> {
                audioManager.mode = AudioManager.MODE_NORMAL
                spatial.enable(audioSessionId)
                effects.configureBassBoost(audioSessionId, 500)
            }
            AudioProfile.DEFAULT -> {
                resetToDefaults()
            }
        }
        
        Log.d(TAG, "Applied profile: $profile")
    }
    
    /**
     * Get audio latency information
     */
    fun getLatencyInfo(): AudioLatency {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            val framesPerBuffer = audioManager.getProperty(
                AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER
            )?.toIntOrNull() ?: 0
            
            val sampleRate = audioManager.getProperty(
                AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE
            )?.toIntOrNull() ?: 44100
            
            val latencyMs = if (framesPerBuffer > 0 && sampleRate > 0) {
                (framesPerBuffer * 1000) / sampleRate
            } else 0
            
            AudioLatency(
                outputLatencyMs = latencyMs,
                outputFramesPerBuffer = framesPerBuffer,
                sampleRate = sampleRate,
                hasLowLatencySupport = context.packageManager.hasSystemFeature(
                    "android.hardware.audio.low_latency"
                ),
                hasProAudioSupport = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    context.packageManager.hasSystemFeature("android.hardware.audio.pro")
                } else false
            )
        } else {
            AudioLatency(0, 0, 44100, false, false)
        }
    }
    
    /**
     * Set volume for stream
     */
    fun setVolume(streamType: Int, volume: Int, showUi: Boolean = false) {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val safeVolume = volume.coerceIn(0, maxVolume)
            val flags = if (showUi) AudioManager.FLAG_SHOW_UI else 0
            audioManager.setStreamVolume(streamType, safeVolume, flags)
            Log.d(TAG, "Volume set: $safeVolume/$maxVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set volume", e)
        }
    }
    
    /**
     * Get current volume for stream
     */
    fun getVolume(streamType: Int): Int {
        return audioManager.getStreamVolume(streamType)
    }
    
    /**
     * Request audio focus with proper focus change handling
     */
    fun requestAudioFocus(
        focusGain: Int = AudioManager.AUDIOFOCUS_GAIN,
        usage: Int = AudioAttributes.USAGE_MEDIA,
        contentType: Int = AudioAttributes.CONTENT_TYPE_MUSIC
    ): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create focus change listener
            val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                handleFocusChange(focusChange)
            }
            
            val request = AudioFocusRequest.Builder(focusGain)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(usage)
                        .setContentType(contentType)
                        .build()
                )
                .setOnAudioFocusChangeListener(focusChangeListener)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .build()
            audioFocusRequest = request

            val result = audioManager.requestAudioFocus(request)
            val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            
            if (granted) {
                currentFocusType = focusGain
                _audioFocusState.value = _audioFocusState.value.copy(
                    hasAudioFocus = true,
                    focusType = focusGain,
                    isDucked = false,
                    isPaused = false,
                    abandonReason = null
                )
                Log.d(TAG, "Audio focus granted: $focusGain")
            } else {
                Log.w(TAG, "Audio focus request failed: $result")
            }
            
            granted
        } else {
            // Legacy API handling
            val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
                handleFocusChange(focusChange)
            }
            
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                focusChangeListener,
                AudioManager.STREAM_MUSIC,
                focusGain
            )
            
            val granted = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            
            if (granted) {
                currentFocusType = focusGain
                _audioFocusState.value = _audioFocusState.value.copy(
                    hasAudioFocus = true,
                    focusType = focusGain,
                    isDucked = false,
                    isPaused = false,
                    abandonReason = null
                )
                Log.d(TAG, "Audio focus granted (legacy): $focusGain")
            } else {
                Log.w(TAG, "Audio focus request failed (legacy): $result")
            }
            
            granted
        }
    }
    
    /**
     * Abandon audio focus
     */
    fun abandonAudioFocus(reason: String = "Manual abandonment") {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                val abandonResult = audioManager.abandonAudioFocusRequest(it)
                abandonResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } ?: true
        } else {
            @Suppress("DEPRECATION")
            val abandonResult = audioManager.abandonAudioFocus(null)
            abandonResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
        
        if (result) {
            currentFocusType = AudioManager.AUDIOFOCUS_NONE
            _audioFocusState.value = _audioFocusState.value.copy(
                hasAudioFocus = false,
                focusType = AudioManager.AUDIOFOCUS_NONE,
                isDucked = false,
                isPaused = false,
                abandonReason = reason
            )
            
            // Notify listeners
            focusChangeListeners.forEach { listener ->
                listener.onAudioFocusLost()
            }
            
            Log.d(TAG, "Audio focus abandoned: $reason")
        }
        
        audioFocusRequest = null
    }
    
    /**
     * Add audio focus change listener
     */
    fun addAudioFocusChangeListener(listener: AudioFocusChangeListener) {
        if (!focusChangeListeners.contains(listener)) {
            focusChangeListeners.add(listener)
            Log.d(TAG, "Added audio focus listener")
        }
    }
    
    /**
     * Remove audio focus change listener
     */
    fun removeAudioFocusChangeListener(listener: AudioFocusChangeListener) {
        focusChangeListeners.remove(listener)
        Log.d(TAG, "Removed audio focus listener")
    }
    
    /**
     * Handle audio focus changes
     */
    private fun handleFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                Log.d(TAG, "Audio focus gained")
                _audioFocusState.value = _audioFocusState.value.copy(
                    hasAudioFocus = true,
                    focusType = currentFocusType,
                    isDucked = false,
                    isPaused = false,
                    abandonReason = null
                )
                
                focusChangeListeners.forEach { listener ->
                    listener.onAudioFocusGained(currentFocusType)
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS -> {
                Log.d(TAG, "Audio focus lost permanently")
                _audioFocusState.value = _audioFocusState.value.copy(
                    hasAudioFocus = false,
                    focusType = AudioManager.AUDIOFOCUS_NONE,
                    isDucked = false,
                    isPaused = true,
                    abandonReason = "Focus lost permanently"
                )
                
                focusChangeListeners.forEach { listener ->
                    listener.onAudioFocusLost()
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                Log.d(TAG, "Audio focus lost transiently")
                _audioFocusState.value = _audioFocusState.value.copy(
                    hasAudioFocus = false,
                    focusType = AudioManager.AUDIOFOCUS_NONE,
                    isDucked = false,
                    isPaused = true,
                    abandonReason = "Focus lost transiently"
                )
                
                focusChangeListeners.forEach { listener ->
                    listener.onAudioFocusLostTransient()
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                Log.d(TAG, "Audio focus lost transiently (can duck)")
                _audioFocusState.value = _audioFocusState.value.copy(
                    hasAudioFocus = false,
                    focusType = AudioManager.AUDIOFOCUS_NONE,
                    isDucked = true,
                    isPaused = false,
                    abandonReason = "Focus lost - ducking"
                )
                
                focusChangeListeners.forEach { listener ->
                    listener.onAudioFocusLostTransientCanDuck()
                }
            }
        }
    }
    
    /**
     * Check if currently has audio focus
     */
    fun hasAudioFocus(): Boolean = _audioFocusState.value.hasAudioFocus
    
    /**
     * Get current focus type
     */
    fun getCurrentFocusType(): Int = _audioFocusState.value.focusType
    
    /**
     * Check if currently ducked
     */
    fun isDucked(): Boolean = _audioFocusState.value.isDucked
    
    /**
     * Check if currently paused due to focus loss
     */
    fun isPausedForFocus(): Boolean = _audioFocusState.value.isPaused
    
    /**
     * Reset to default settings
     */
    fun resetToDefaults() {
        audioManager.mode = AudioManager.MODE_NORMAL
        routing.setSpeakerphone(false)
        routing.stopBluetoothSco()
        enhancement.release()
        effects.disableAll()
        spatial.disable()
        abandonAudioFocus()
        Log.d(TAG, "Reset to defaults")
    }
    
    /**
     * Release all resources
     */
    fun release() {
        routing.release()
        enhancement.release()
        effects.release()
        spatial.release()
        capture.release()
        abandonAudioFocus()
        Log.d(TAG, "Audio service released")
    }
}
