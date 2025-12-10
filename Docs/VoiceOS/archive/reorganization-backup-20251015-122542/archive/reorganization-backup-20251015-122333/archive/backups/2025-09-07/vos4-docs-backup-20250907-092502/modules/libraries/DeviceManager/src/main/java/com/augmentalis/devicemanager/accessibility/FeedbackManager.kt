// Author: Manoj Jhawar
// Purpose: Comprehensive feedback management for haptic, audio, and visual feedback systems

package com.augmentalis.devicemanager.accessibility

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * COT: This FeedbackManager follows the Single Responsibility Principle by handling ONLY feedback 
 * operations (haptic, audio, visual). It does NOT handle TTS or translation - those are separate
 * concerns managed by AccessibilityManager and TTSManager respectively.
 * 
 * Design Decision: Separated feedback into three distinct domains (haptic, audio, visual) each
 * with their own settings and state management. This allows granular control and easier testing.
 * 
 * StateFlow Choice: Using StateFlow for reactive updates allows UI components to observe feedback
 * state changes without tight coupling. This is particularly important for accessibility features
 * where feedback state may need to be monitored by multiple components.
 */
class FeedbackManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FeedbackManager"
        
        // Preferences key
        private const val FEEDBACK_PREFS = "feedback_preferences"
        
        // Haptic feedback preferences
        private const val PREF_HAPTIC_ENABLED = "haptic_enabled"
        private const val PREF_HAPTIC_INTENSITY = "haptic_intensity"
        private const val PREF_HAPTIC_DURATION = "haptic_duration"
        private const val PREF_HAPTIC_PATTERN = "haptic_pattern"
        
        // Audio feedback preferences  
        private const val PREF_AUDIO_ENABLED = "audio_enabled"
        private const val PREF_AUDIO_VOLUME = "audio_volume"
        private const val PREF_AUDIO_TONE_TYPE = "audio_tone_type"
        private const val PREF_AUDIO_DURATION = "audio_duration"
        
        // Visual feedback preferences
        private const val PREF_VISUAL_ENABLED = "visual_enabled"
        private const val PREF_SCREEN_FLASH_ENABLED = "screen_flash_enabled"
        private const val PREF_ANIMATION_ENABLED = "animation_enabled"
        private const val PREF_ANIMATION_DURATION = "animation_duration"
        private const val PREF_FLASH_INTENSITY = "flash_intensity"
        
        // Default values
        private const val DEFAULT_HAPTIC_INTENSITY = 0.5f
        private const val DEFAULT_HAPTIC_DURATION = 100L
        private const val DEFAULT_AUDIO_VOLUME = 0.7f
        private const val DEFAULT_AUDIO_DURATION = 200L
        private const val DEFAULT_ANIMATION_DURATION = 300L
        private const val DEFAULT_FLASH_INTENSITY = 0.8f
        
        // Haptic patterns
        private val PATTERN_SINGLE = longArrayOf(0, 100)
        private val PATTERN_DOUBLE = longArrayOf(0, 100, 50, 100)
        private val PATTERN_TRIPLE = longArrayOf(0, 100, 50, 100, 50, 100)
        private val PATTERN_LONG = longArrayOf(0, 300)
        
        // Audio tone types
        private const val TONE_SUCCESS = ToneGenerator.TONE_PROP_BEEP
        private const val TONE_ERROR = ToneGenerator.TONE_PROP_NACK
        private const val TONE_WARNING = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD
        private const val TONE_INFO = ToneGenerator.TONE_PROP_ACK
    }
    
    // System services
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var toneGenerator: ToneGenerator? = null
    
    // Preferences
    private val preferences: SharedPreferences = context.getSharedPreferences(FEEDBACK_PREFS, Context.MODE_PRIVATE)
    
    // State flows
    private val _feedbackState = MutableStateFlow(FeedbackState())
    val feedbackState: StateFlow<FeedbackState> = _feedbackState.asStateFlow()
    
    /**
     * COT: Comprehensive state data class that encapsulates all feedback settings and current state.
     * Grouped by feedback type for clarity and maintainability. Each feedback type has both
     * enabled status and specific settings to allow fine-grained control.
     */
    data class FeedbackState(
        val haptic: HapticFeedbackState = HapticFeedbackState(),
        val audio: AudioFeedbackState = AudioFeedbackState(),
        val visual: VisualFeedbackState = VisualFeedbackState(),
        val systemFeedbackEnabled: Boolean = true
    )
    
    data class HapticFeedbackState(
        val enabled: Boolean = true,
        val intensity: Float = DEFAULT_HAPTIC_INTENSITY,
        val duration: Long = DEFAULT_HAPTIC_DURATION,
        val pattern: HapticPattern = HapticPattern.SINGLE,
        val isVibrating: Boolean = false,
        val isAvailable: Boolean = true
    )
    
    data class AudioFeedbackState(
        val enabled: Boolean = true,
        val volume: Float = DEFAULT_AUDIO_VOLUME,
        val toneType: AudioToneType = AudioToneType.SUCCESS,
        val duration: Long = DEFAULT_AUDIO_DURATION,
        val isPlaying: Boolean = false,
        val isAvailable: Boolean = true
    )
    
    data class VisualFeedbackState(
        val enabled: Boolean = true,
        val screenFlashEnabled: Boolean = true,
        val animationEnabled: Boolean = true,
        val animationDuration: Long = DEFAULT_ANIMATION_DURATION,
        val flashIntensity: Float = DEFAULT_FLASH_INTENSITY,
        val isAnimating: Boolean = false
    )
    
    enum class HapticPattern(val pattern: LongArray) {
        SINGLE(PATTERN_SINGLE),
        DOUBLE(PATTERN_DOUBLE),
        TRIPLE(PATTERN_TRIPLE),
        LONG(PATTERN_LONG)
    }
    
    enum class AudioToneType(val tone: Int) {
        SUCCESS(TONE_SUCCESS),
        ERROR(TONE_ERROR),
        WARNING(TONE_WARNING),
        INFO(TONE_INFO)
    }
    
    enum class FeedbackType {
        SUCCESS,
        ERROR,
        WARNING,
        INFO,
        SELECTION,
        NAVIGATION
    }
    
    init {
        initializeFeedbackSystems()
        loadPreferences()
    }
    
    /**
     * COT: Initialization separated from constructor to allow for proper error handling
     * and logging. Each feedback system (haptic, audio, visual) is initialized independently
     * to handle cases where one system might fail while others succeed.
     */
    private fun initializeFeedbackSystems() {
        try {
            // Initialize haptic feedback
            val hapticAvailable = vibrator?.hasVibrator() == true
            
            // Initialize audio feedback
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 50)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize ToneGenerator", e)
                toneGenerator = null
            }
            
            val audioAvailable = toneGenerator != null
            
            // Update state with system capabilities
            updateState { currentState ->
                currentState.copy(
                    haptic = currentState.haptic.copy(isAvailable = hapticAvailable),
                    audio = currentState.audio.copy(isAvailable = audioAvailable)
                )
            }
            
            Log.d(TAG, "FeedbackManager initialized - Haptic: $hapticAvailable, Audio: $audioAvailable")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing feedback systems", e)
        }
    }
    
    /**
     * COT: Preference loading follows the pattern established in AccessibilityManager.
     * Each preference type is loaded separately to handle missing or corrupted values gracefully.
     * Default values are used as fallbacks to ensure system remains functional.
     */
    private fun loadPreferences() {
        try {
            val hapticEnabled = preferences.getBoolean(PREF_HAPTIC_ENABLED, true)
            val hapticIntensity = preferences.getFloat(PREF_HAPTIC_INTENSITY, DEFAULT_HAPTIC_INTENSITY)
            val hapticDuration = preferences.getLong(PREF_HAPTIC_DURATION, DEFAULT_HAPTIC_DURATION)
            val hapticPatternName = preferences.getString(PREF_HAPTIC_PATTERN, HapticPattern.SINGLE.name)
            val hapticPattern = try {
                HapticPattern.valueOf(hapticPatternName ?: HapticPattern.SINGLE.name)
            } catch (e: IllegalArgumentException) {
                HapticPattern.SINGLE
            }
            
            val audioEnabled = preferences.getBoolean(PREF_AUDIO_ENABLED, true)
            val audioVolume = preferences.getFloat(PREF_AUDIO_VOLUME, DEFAULT_AUDIO_VOLUME)
            val audioToneName = preferences.getString(PREF_AUDIO_TONE_TYPE, AudioToneType.SUCCESS.name)
            val audioToneType = try {
                AudioToneType.valueOf(audioToneName ?: AudioToneType.SUCCESS.name)
            } catch (e: IllegalArgumentException) {
                AudioToneType.SUCCESS
            }
            val audioDuration = preferences.getLong(PREF_AUDIO_DURATION, DEFAULT_AUDIO_DURATION)
            
            val visualEnabled = preferences.getBoolean(PREF_VISUAL_ENABLED, true)
            val screenFlashEnabled = preferences.getBoolean(PREF_SCREEN_FLASH_ENABLED, true)
            val animationEnabled = preferences.getBoolean(PREF_ANIMATION_ENABLED, true)
            val animationDuration = preferences.getLong(PREF_ANIMATION_DURATION, DEFAULT_ANIMATION_DURATION)
            val flashIntensity = preferences.getFloat(PREF_FLASH_INTENSITY, DEFAULT_FLASH_INTENSITY)
            
            updateState { currentState ->
                currentState.copy(
                    haptic = currentState.haptic.copy(
                        enabled = hapticEnabled,
                        intensity = hapticIntensity,
                        duration = hapticDuration,
                        pattern = hapticPattern
                    ),
                    audio = currentState.audio.copy(
                        enabled = audioEnabled,
                        volume = audioVolume,
                        toneType = audioToneType,
                        duration = audioDuration
                    ),
                    visual = currentState.visual.copy(
                        enabled = visualEnabled,
                        screenFlashEnabled = screenFlashEnabled,
                        animationEnabled = animationEnabled,
                        animationDuration = animationDuration,
                        flashIntensity = flashIntensity
                    )
                )
            }
            
            Log.d(TAG, "Preferences loaded successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading preferences, using defaults", e)
        }
    }
    
    // HAPTIC FEEDBACK METHODS
    
    /**
     * COT: Haptic feedback methods provide different levels of feedback for different UI actions.
     * Each method corresponds to a specific user interaction pattern. The implementation handles
     * API level differences (VibrationEffect vs deprecated vibrate methods) transparently.
     */
    
    fun provideHapticFeedback(type: FeedbackType) {
        val state = _feedbackState.value
        if (!state.haptic.enabled || !state.haptic.isAvailable) return
        
        val pattern = when (type) {
            FeedbackType.SUCCESS -> HapticPattern.SINGLE
            FeedbackType.ERROR -> HapticPattern.TRIPLE
            FeedbackType.WARNING -> HapticPattern.DOUBLE
            FeedbackType.INFO -> HapticPattern.SINGLE
            FeedbackType.SELECTION -> HapticPattern.SINGLE
            FeedbackType.NAVIGATION -> HapticPattern.DOUBLE
        }
        
        vibrate(pattern)
    }
    
    fun vibrateWithPattern(pattern: HapticPattern) {
        vibrate(pattern)
    }
    
    fun vibrateWithDuration(duration: Long) {
        val state = _feedbackState.value
        if (!state.haptic.enabled || !state.haptic.isAvailable) return
        
        try {
            updateState { it.copy(haptic = it.haptic.copy(isVibrating = true)) }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intensity = (state.haptic.intensity * 255).toInt().coerceIn(1, 255)
                val effect = VibrationEffect.createOneShot(duration, intensity)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(duration)
            }
            
            // Reset vibrating state after duration
            CoroutineScope(Dispatchers.Main).launch {
                delay(duration)
                updateState { it.copy(haptic = it.haptic.copy(isVibrating = false)) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error providing haptic feedback", e)
            updateState { it.copy(haptic = it.haptic.copy(isVibrating = false)) }
        }
    }
    
    private fun vibrate(pattern: HapticPattern) {
        val state = _feedbackState.value
        if (!state.haptic.enabled || !state.haptic.isAvailable) return
        
        try {
            updateState { it.copy(haptic = it.haptic.copy(isVibrating = true)) }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val intensity = (state.haptic.intensity * 255).toInt().coerceIn(1, 255)
                val effect = VibrationEffect.createWaveform(pattern.pattern, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern.pattern, -1)
            }
            
            // Calculate total pattern duration
            val totalDuration = pattern.pattern.sum()
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(totalDuration)
                updateState { it.copy(haptic = it.haptic.copy(isVibrating = false)) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error providing haptic feedback with pattern", e)
            updateState { it.copy(haptic = it.haptic.copy(isVibrating = false)) }
        }
    }
    
    // AUDIO FEEDBACK METHODS
    
    /**
     * COT: Audio feedback uses ToneGenerator for system-level audio feedback tones.
     * Different tones are mapped to different feedback types to provide audio cues
     * that match user expectations (success = positive tone, error = negative tone).
     */
    
    fun provideAudioFeedback(type: FeedbackType) {
        val state = _feedbackState.value
        if (!state.audio.enabled || !state.audio.isAvailable) return
        
        val toneType = when (type) {
            FeedbackType.SUCCESS -> AudioToneType.SUCCESS
            FeedbackType.ERROR -> AudioToneType.ERROR
            FeedbackType.WARNING -> AudioToneType.WARNING
            FeedbackType.INFO -> AudioToneType.INFO
            FeedbackType.SELECTION -> AudioToneType.INFO
            FeedbackType.NAVIGATION -> AudioToneType.SUCCESS
        }
        
        playTone(toneType)
    }
    
    fun playTone(toneType: AudioToneType) {
        val state = _feedbackState.value
        if (!state.audio.enabled || !state.audio.isAvailable) return
        
        try {
            updateState { it.copy(audio = it.audio.copy(isPlaying = true)) }
            
            val volume = (state.audio.volume * 100).toInt().coerceIn(0, 100)
            toneGenerator?.startTone(toneType.tone, state.audio.duration.toInt())
            
            // Reset playing state after duration
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(state.audio.duration)
                updateState { it.copy(audio = it.audio.copy(isPlaying = false)) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error providing audio feedback", e)
            updateState { it.copy(audio = it.audio.copy(isPlaying = false)) }
        }
    }
    
    fun playCustomTone(tone: Int, duration: Long) {
        val state = _feedbackState.value
        if (!state.audio.enabled || !state.audio.isAvailable) return
        
        try {
            updateState { it.copy(audio = it.audio.copy(isPlaying = true)) }
            
            toneGenerator?.startTone(tone, duration.toInt())
            
            CoroutineScope(Dispatchers.Main).launch {
                delay(duration)
                updateState { it.copy(audio = it.audio.copy(isPlaying = false)) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error playing custom tone", e)
            updateState { it.copy(audio = it.audio.copy(isPlaying = false)) }
        }
    }
    
    // VISUAL FEEDBACK METHODS
    
    /**
     * COT: Visual feedback methods provide callbacks for UI components to implement
     * visual feedback effects. The manager coordinates timing and state but delegates
     * actual visual implementation to UI components since feedback system should not
     * directly manipulate UI elements (separation of concerns).
     */
    
    fun provideVisualFeedback(type: FeedbackType, callback: VisualFeedbackCallback) {
        val state = _feedbackState.value
        if (!state.visual.enabled) return
        
        val config = when (type) {
            FeedbackType.SUCCESS -> VisualFeedbackConfig(
                flashEnabled = state.visual.screenFlashEnabled,
                animationEnabled = state.visual.animationEnabled,
                duration = state.visual.animationDuration,
                intensity = state.visual.flashIntensity,
                color = 0xFF4CAF50.toInt() // Green for success
            )
            FeedbackType.ERROR -> VisualFeedbackConfig(
                flashEnabled = state.visual.screenFlashEnabled,
                animationEnabled = state.visual.animationEnabled,
                duration = state.visual.animationDuration,
                intensity = state.visual.flashIntensity,
                color = 0xFFF44336.toInt() // Red for error
            )
            FeedbackType.WARNING -> VisualFeedbackConfig(
                flashEnabled = state.visual.screenFlashEnabled,
                animationEnabled = state.visual.animationEnabled,
                duration = state.visual.animationDuration,
                intensity = state.visual.flashIntensity,
                color = 0xFFFF9800.toInt() // Orange for warning
            )
            FeedbackType.INFO -> VisualFeedbackConfig(
                flashEnabled = state.visual.screenFlashEnabled,
                animationEnabled = state.visual.animationEnabled,
                duration = state.visual.animationDuration,
                intensity = state.visual.flashIntensity,
                color = 0xFF2196F3.toInt() // Blue for info
            )
            FeedbackType.SELECTION -> VisualFeedbackConfig(
                flashEnabled = false,
                animationEnabled = state.visual.animationEnabled,
                duration = state.visual.animationDuration / 2,
                intensity = state.visual.flashIntensity * 0.5f,
                color = 0xFF9C27B0.toInt() // Purple for selection
            )
            FeedbackType.NAVIGATION -> VisualFeedbackConfig(
                flashEnabled = false,
                animationEnabled = state.visual.animationEnabled,
                duration = state.visual.animationDuration / 3,
                intensity = state.visual.flashIntensity * 0.3f,
                color = 0xFF607D8B.toInt() // Blue-grey for navigation
            )
        }
        
        executeVisualFeedback(config, callback)
    }
    
    private fun executeVisualFeedback(config: VisualFeedbackConfig, callback: VisualFeedbackCallback) {
        try {
            updateState { it.copy(visual = it.visual.copy(isAnimating = true)) }
            
            callback.onVisualFeedbackStart(config)
            
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
                kotlinx.coroutines.delay(config.duration)
                callback.onVisualFeedbackEnd(config)
                updateState { it.copy(visual = it.visual.copy(isAnimating = false)) }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error providing visual feedback", e)
            updateState { it.copy(visual = it.visual.copy(isAnimating = false)) }
        }
    }
    
    data class VisualFeedbackConfig(
        val flashEnabled: Boolean,
        val animationEnabled: Boolean,
        val duration: Long,
        val intensity: Float,
        val color: Int
    )
    
    interface VisualFeedbackCallback {
        fun onVisualFeedbackStart(config: VisualFeedbackConfig)
        fun onVisualFeedbackEnd(config: VisualFeedbackConfig)
    }
    
    // COMBINED FEEDBACK METHODS
    
    /**
     * COT: Combined feedback methods allow triggering multiple feedback types simultaneously.
     * This is useful for important system events where multiple modalities should be used
     * to ensure user awareness (accessibility requirement).
     */
    
    fun provideFeedback(
        type: FeedbackType,
        includeHaptic: Boolean = true,
        includeAudio: Boolean = true,
        includeVisual: Boolean = false,
        visualCallback: VisualFeedbackCallback? = null
    ) {
        if (includeHaptic) {
            provideHapticFeedback(type)
        }
        
        if (includeAudio) {
            provideAudioFeedback(type)
        }
        
        if (includeVisual && visualCallback != null) {
            provideVisualFeedback(type, visualCallback)
        }
    }
    
    // SETTINGS METHODS
    
    /**
     * COT: Settings methods provide granular control over feedback preferences.
     * Each setting is persisted immediately to SharedPreferences to ensure
     * user preferences are maintained across app sessions.
     */
    
    fun setHapticEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(PREF_HAPTIC_ENABLED, enabled).apply()
        updateState { it.copy(haptic = it.haptic.copy(enabled = enabled)) }
    }
    
    fun setHapticIntensity(intensity: Float) {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        preferences.edit().putFloat(PREF_HAPTIC_INTENSITY, clampedIntensity).apply()
        updateState { it.copy(haptic = it.haptic.copy(intensity = clampedIntensity)) }
    }
    
    fun setHapticDuration(duration: Long) {
        val clampedDuration = duration.coerceIn(50L, 1000L)
        preferences.edit().putLong(PREF_HAPTIC_DURATION, clampedDuration).apply()
        updateState { it.copy(haptic = it.haptic.copy(duration = clampedDuration)) }
    }
    
    fun setHapticPattern(pattern: HapticPattern) {
        preferences.edit().putString(PREF_HAPTIC_PATTERN, pattern.name).apply()
        updateState { it.copy(haptic = it.haptic.copy(pattern = pattern)) }
    }
    
    fun setAudioEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(PREF_AUDIO_ENABLED, enabled).apply()
        updateState { it.copy(audio = it.audio.copy(enabled = enabled)) }
    }
    
    fun setAudioVolume(volume: Float) {
        val clampedVolume = volume.coerceIn(0f, 1f)
        preferences.edit().putFloat(PREF_AUDIO_VOLUME, clampedVolume).apply()
        updateState { it.copy(audio = it.audio.copy(volume = clampedVolume)) }
    }
    
    fun setAudioToneType(toneType: AudioToneType) {
        preferences.edit().putString(PREF_AUDIO_TONE_TYPE, toneType.name).apply()
        updateState { it.copy(audio = it.audio.copy(toneType = toneType)) }
    }
    
    fun setAudioDuration(duration: Long) {
        val clampedDuration = duration.coerceIn(100L, 2000L)
        preferences.edit().putLong(PREF_AUDIO_DURATION, clampedDuration).apply()
        updateState { it.copy(audio = it.audio.copy(duration = clampedDuration)) }
    }
    
    fun setVisualEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(PREF_VISUAL_ENABLED, enabled).apply()
        updateState { it.copy(visual = it.visual.copy(enabled = enabled)) }
    }
    
    fun setScreenFlashEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(PREF_SCREEN_FLASH_ENABLED, enabled).apply()
        updateState { it.copy(visual = it.visual.copy(screenFlashEnabled = enabled)) }
    }
    
    fun setAnimationEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(PREF_ANIMATION_ENABLED, enabled).apply()
        updateState { it.copy(visual = it.visual.copy(animationEnabled = enabled)) }
    }
    
    fun setAnimationDuration(duration: Long) {
        val clampedDuration = duration.coerceIn(100L, 1000L)
        preferences.edit().putLong(PREF_ANIMATION_DURATION, clampedDuration).apply()
        updateState { it.copy(visual = it.visual.copy(animationDuration = clampedDuration)) }
    }
    
    fun setFlashIntensity(intensity: Float) {
        val clampedIntensity = intensity.coerceIn(0f, 1f)
        preferences.edit().putFloat(PREF_FLASH_INTENSITY, clampedIntensity).apply()
        updateState { it.copy(visual = it.visual.copy(flashIntensity = clampedIntensity)) }
    }
    
    fun setSystemFeedbackEnabled(enabled: Boolean) {
        updateState { it.copy(systemFeedbackEnabled = enabled) }
    }
    
    // UTILITY METHODS
    
    /**
     * COT: Utility methods provide convenient access to current settings and capabilities.
     * These methods abstract the StateFlow complexity for simple queries.
     */
    
    fun isHapticAvailable(): Boolean = _feedbackState.value.haptic.isAvailable
    fun isAudioAvailable(): Boolean = _feedbackState.value.audio.isAvailable
    fun isHapticEnabled(): Boolean = _feedbackState.value.haptic.enabled
    fun isAudioEnabled(): Boolean = _feedbackState.value.audio.enabled
    fun isVisualEnabled(): Boolean = _feedbackState.value.visual.enabled
    
    fun getCurrentHapticSettings(): HapticFeedbackState = _feedbackState.value.haptic
    fun getCurrentAudioSettings(): AudioFeedbackState = _feedbackState.value.audio
    fun getCurrentVisualSettings(): VisualFeedbackState = _feedbackState.value.visual
    
    private fun updateState(update: (FeedbackState) -> FeedbackState) {
        _feedbackState.value = update(_feedbackState.value)
    }
    
    /**
     * COT: Cleanup method ensures proper resource disposal when manager is no longer needed.
     * ToneGenerator must be released to avoid audio resource leaks.
     */
    fun cleanup() {
        try {
            toneGenerator?.release()
            toneGenerator = null
            Log.d(TAG, "FeedbackManager cleanup completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}