/**
 * TTSEngine.kt - Text-to-Speech integration for Speech Recognition library
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-05
 * 
 * This class provides TTS integration within the SpeechRecognition library
 * by delegating to the existing AccessibilityManager TTS functionality
 * to avoid code duplication and maintain single-source-of-truth.
 */
package com.augmentalis.voiceos.speech.engines.tts

import android.content.Context
import android.speech.tts.Voice
import com.augmentalis.devicemanager.accessibility.TTSManager
import com.augmentalis.voiceos.speech.api.RecognitionResult
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * Text-to-Speech engine that integrates with SpeechRecognition
 * Delegates to AccessibilityManager to avoid duplication
 */
class TTSEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "TTSEngine"
        
        // Default TTS settings
        private const val DEFAULT_RATE = 1.0f
        private const val DEFAULT_PITCH = 1.0f
        private const val DEFAULT_VOLUME = 1.0f
        
        // Utterance ID prefix for speech recognition responses
        private const val UTTERANCE_PREFIX = "speech_response_"
    }
    
    // Delegate to TTSManager for actual TTS functionality
    private val ttsManager = TTSManager(context)
    
    // State flows
    private val _ttsEngineState = MutableStateFlow(TTSEngineState())
    val ttsEngineState: StateFlow<TTSEngineState> = _ttsEngineState.asStateFlow()
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    init {
        initialize()
    }
    
    // ========== DATA MODELS ==========
    
    data class TTSEngineState(
        val isReady: Boolean = false,
        val isSpeaking: Boolean = false,
        val currentLanguage: Locale? = null,
        val availableLanguages: List<Locale> = emptyList(),
        val availableVoices: List<Voice> = emptyList(),
        val settings: TTSEngineSettings = TTSEngineSettings()
    )
    
    data class TTSEngineSettings(
        val rate: Float = DEFAULT_RATE,
        val pitch: Float = DEFAULT_PITCH,
        val volume: Float = DEFAULT_VOLUME,
        val language: Locale = Locale.getDefault(),
        val voice: String? = null
    )
    
    data class SpeechRequest(
        val text: String,
        val priority: SpeechPriority = SpeechPriority.NORMAL,
        val queueMode: QueueMode = QueueMode.FLUSH,
        val utteranceId: String? = null,
        val language: Locale? = null,
        val rate: Float? = null,
        val pitch: Float? = null
    )
    
    enum class SpeechPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
    
    enum class QueueMode {
        FLUSH,    // Replace current speech
        ADD       // Queue after current speech
    }
    
    // ========== INITIALIZATION ==========
    
    private fun initialize() {
        // Monitor TTSManager TTS state
        scope.launch {
            ttsManager.ttsState.collect { ttsState: TTSManager.TTSState ->
                _ttsEngineState.update { engineState ->
                    engineState.copy(
                        isReady = ttsState.isInitialized,
                        isSpeaking = ttsState.isSpeaking,
                        currentLanguage = ttsState.currentLanguage,
                        availableLanguages = ttsState.availableLanguages,
                        availableVoices = ttsState.availableVoices
                    )
                }
            }
        }
    }
    
    // ========== TTS CORE FUNCTIONALITY ==========
    
    /**
     * Speak text using the configured TTS settings
     */
    suspend fun speak(request: SpeechRequest): Boolean {
        if (!isReady()) {
            return false
        }
        
        // Apply temporary settings if provided
        request.rate?.let { setSpeechRate(it) }
        request.pitch?.let { setSpeechPitch(it) }
        request.language?.let { setSpeechLanguage(it) }
        
        // Map our priority to TTSManager's priority
        val priority = when (request.priority) {
            SpeechPriority.LOW -> TTSManager.SpeechPriority.LOW
            SpeechPriority.NORMAL -> TTSManager.SpeechPriority.NORMAL
            SpeechPriority.HIGH -> TTSManager.SpeechPriority.HIGH
            SpeechPriority.URGENT -> TTSManager.SpeechPriority.URGENT
        }
        
        // Use speakWithPriority for prioritized speech
        if (request.priority != SpeechPriority.NORMAL) {
            return ttsManager.speakWithPriority(request.text, priority)
        }
        
        // Use regular speak for normal priority
        val queueMode = when (request.queueMode) {
            QueueMode.FLUSH -> TTSManager.TTS_QUEUE_FLUSH
            QueueMode.ADD -> TTSManager.TTS_QUEUE_ADD
        }
        
        return ttsManager.speak(
            text = request.text,
            queueMode = queueMode,
            utteranceId = request.utteranceId ?: generateUtteranceId()
        )
    }
    
    /**
     * Speak text immediately, interrupting current speech
     */
    fun speakNow(text: String): Boolean {
        return ttsManager.speak(text, TTSManager.TTS_QUEUE_FLUSH, generateUtteranceId())
    }
    
    /**
     * Queue text to be spoken after current speech
     */
    fun queueSpeech(text: String): Boolean {
        return ttsManager.speak(text, TTSManager.TTS_QUEUE_ADD, generateUtteranceId())
    }
    
    /**
     * Stop current speech
     */
    fun stopSpeaking() {
        ttsManager.stop()
    }
    
    /**
     * Pause current speech (stops on Android as pause isn't supported)
     */
    fun pauseSpeaking() {
        // Android TTS doesn't support pause, so we stop instead
        ttsManager.stop()
    }
    
    // ========== SPEECH RECOGNITION INTEGRATION ==========
    
    /**
     * Speak a recognition result with appropriate formatting
     */
    suspend fun speakRecognitionResult(result: RecognitionResult): Boolean {
        val text = formatRecognitionResult(result)
        return speak(
            SpeechRequest(
                text = text,
                utteranceId = "recognition_${result.timestamp}",
                priority = SpeechPriority.NORMAL
            )
        )
    }
    
    /**
     * Provide audio feedback for speech recognition events
     */
    suspend fun provideFeedback(event: SpeechEvent): Boolean {
        val text = when (event) {
            SpeechEvent.LISTENING_STARTED -> "Listening"
            SpeechEvent.LISTENING_STOPPED -> "Stopped listening"
            SpeechEvent.RECOGNITION_ERROR -> "Recognition error"
            SpeechEvent.NO_SPEECH_DETECTED -> "No speech detected"
            SpeechEvent.PARTIAL_RESULT -> "Processing speech"
        }
        
        return speak(
            SpeechRequest(
                text = text,
                priority = SpeechPriority.LOW,
                queueMode = QueueMode.ADD,
                utteranceId = "feedback_${event.name.lowercase()}"
            )
        )
    }
    
    /**
     * Announce command recognition
     */
    suspend fun announceCommand(command: String, confidence: Float): Boolean {
        val text = when {
            confidence >= 0.9f -> "Executing $command"
            confidence >= 0.7f -> "Command: $command"
            else -> "Did you say $command?"
        }
        
        return speak(
            SpeechRequest(
                text = text,
                priority = SpeechPriority.HIGH,
                utteranceId = "command_${command.hashCode()}"
            )
        )
    }
    
    /**
     * Speak translation result with language context
     */
    suspend fun speakTranslation(@Suppress("UNUSED_PARAMETER") originalText: String, translatedText: String, targetLanguage: String): Boolean {
        val targetLocale = Locale.forLanguageTag(targetLanguage)
        
        return speak(
            SpeechRequest(
                text = translatedText,
                language = targetLocale,
                utteranceId = "translation_${System.currentTimeMillis()}",
                priority = SpeechPriority.NORMAL
            )
        )
    }
    
    // ========== TTS SETTINGS ==========
    
    fun setSpeechRate(rate: Float) {
        ttsManager.setSpeechRate(rate)
        _ttsEngineState.update { state ->
            state.copy(settings = state.settings.copy(rate = rate))
        }
    }
    
    fun setSpeechPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
        _ttsEngineState.update { state ->
            state.copy(settings = state.settings.copy(pitch = pitch))
        }
    }
    
    fun setSpeechVolume(volume: Float) {
        // TTSManager handles volume through audio stream, not directly
        // Update our state to track the desired volume
        _ttsEngineState.update { state ->
            state.copy(settings = state.settings.copy(volume = volume))
        }
    }
    
    fun setSpeechLanguage(locale: Locale): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val success = ttsManager.setLanguage(locale)
            if (success) {
                _ttsEngineState.update { state ->
                    state.copy(
                        currentLanguage = locale,
                        settings = state.settings.copy(language = locale)
                    )
                }
            }
            success
        } else {
            false
        }
    }
    
    fun setSpeechVoice(voice: Voice): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            val success = ttsManager.setVoice(voice)
            if (success) {
                _ttsEngineState.update { state ->
                    state.copy(settings = state.settings.copy(voice = voice.name))
                }
            }
            success
        } else {
            false
        }
    }
    
    // ========== UTILITY METHODS ==========
    
    fun isReady(): Boolean {
        return ttsManager.ttsState.value.isInitialized
    }
    
    fun isSpeaking(): Boolean {
        return ttsManager.ttsState.value.isSpeaking
    }
    
    fun getAvailableLanguages(): List<Locale> {
        return _ttsEngineState.value.availableLanguages
    }
    
    fun getAvailableVoices(): List<Voice> {
        return _ttsEngineState.value.availableVoices
    }
    
    fun getCurrentSettings(): TTSEngineSettings {
        return _ttsEngineState.value.settings
    }
    
    private fun generateUtteranceId(): String {
        return "$UTTERANCE_PREFIX${System.currentTimeMillis()}"
    }
    
    private fun formatRecognitionResult(result: RecognitionResult): String {
        return when {
            result.isEmpty() -> "No speech recognized"
            result.confidence < 0.5f -> "Uncertain: ${result.text}"
            result.hasTranslation() -> "Translated: ${result.translation}"
            else -> result.text
        }
    }
    
    fun cleanup() {
        scope.cancel()
        // TTSManager cleanup
        ttsManager.release()
    }
    
    // ========== SPEECH EVENTS ==========
    
    enum class SpeechEvent {
        LISTENING_STARTED,
        LISTENING_STOPPED,
        RECOGNITION_ERROR,
        NO_SPEECH_DETECTED,
        PARTIAL_RESULT
    }
}