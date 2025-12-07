/**
 * TTSIntegration.kt - TTS integration interface for Speech Recognition
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-09-05
 * 
 * Provides a unified interface for TTS functionality within the Speech Recognition library.
 * This interface allows speech recognition engines to provide audio feedback without
 * direct coupling to the TTS implementation.
 */
package com.augmentalis.voiceos.speech.api

import android.content.Context
import android.speech.tts.Voice
import com.augmentalis.voiceos.speech.engines.tts.TTSEngine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.*

/**
 * TTS Integration interface for speech recognition engines
 */
interface TTSIntegration {
    
    /**
     * Initialize TTS integration
     */
    fun initialize(context: Context): Boolean
    
    /**
     * Check if TTS is ready for use
     */
    fun isReady(): Boolean
    
    /**
     * Check if TTS is currently speaking
     */
    fun isSpeaking(): Boolean
    
    /**
     * Speak text immediately
     */
    suspend fun speak(text: String, utteranceId: String? = null): Boolean
    
    /**
     * Speak text with priority
     */
    suspend fun speakWithPriority(text: String, priority: SpeechPriority = SpeechPriority.NORMAL): Boolean
    
    /**
     * Provide audio feedback for recognition events
     */
    suspend fun provideFeedback(event: SpeechFeedbackEvent): Boolean
    
    /**
     * Speak recognition result with formatting
     */
    suspend fun speakResult(result: RecognitionResult): Boolean
    
    /**
     * Stop current speech
     */
    fun stopSpeaking()
    
    /**
     * Configure TTS settings
     */
    fun configure(settings: TTSSettings)
    
    /**
     * Get current TTS state
     */
    fun getState(): StateFlow<TTSState>
    
    /**
     * Clean up resources
     */
    fun cleanup()
}

/**
 * Speech feedback events that can be announced
 */
enum class SpeechFeedbackEvent {
    LISTENING_STARTED,
    LISTENING_STOPPED,
    PROCESSING,
    RECOGNITION_SUCCESS,
    RECOGNITION_ERROR,
    NO_SPEECH_DETECTED,
    TIMEOUT,
    COMMAND_RECOGNIZED,
    COMMAND_EXECUTED,
    DICTATION_STARTED,
    DICTATION_STOPPED
}

/**
 * Speech priority levels
 */
enum class SpeechPriority {
    LOW,
    NORMAL,
    HIGH,
    URGENT
}

/**
 * TTS configuration settings
 */
data class TTSSettings(
    val rate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val volume: Float = 1.0f,
    val language: Locale = Locale.getDefault(),
    val voice: String? = null,
    val enableFeedback: Boolean = true,
    val feedbackLevel: FeedbackLevel = FeedbackLevel.NORMAL
)

/**
 * TTS feedback levels
 */
enum class FeedbackLevel {
    SILENT,      // No audio feedback
    MINIMAL,     // Only errors and confirmations
    NORMAL,      // Standard feedback
    VERBOSE      // Detailed feedback
}

/**
 * TTS state information
 */
data class TTSState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val currentLanguage: Locale? = null,
    val availableLanguages: List<Locale> = emptyList(),
    val availableVoices: List<Voice> = emptyList(),
    val settings: TTSSettings = TTSSettings()
)

/**
 * Default implementation of TTSIntegration using TTSEngine
 */
class DefaultTTSIntegration : TTSIntegration {
    
    private var ttsEngine: TTSEngine? = null
    private var currentSettings = TTSSettings()
    
    override fun initialize(context: Context): Boolean {
        return try {
            ttsEngine = TTSEngine(context)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    override fun isReady(): Boolean {
        return ttsEngine?.isReady() == true
    }
    
    override fun isSpeaking(): Boolean {
        return ttsEngine?.isSpeaking() == true
    }
    
    override suspend fun speak(text: String, utteranceId: String?): Boolean {
        return ttsEngine?.speak(
            TTSEngine.SpeechRequest(
                text = text,
                utteranceId = utteranceId
            )
        ) ?: false
    }
    
    override suspend fun speakWithPriority(text: String, priority: SpeechPriority): Boolean {
        return ttsEngine?.speak(
            TTSEngine.SpeechRequest(
                text = text,
                priority = when (priority) {
                    SpeechPriority.LOW -> TTSEngine.SpeechPriority.LOW
                    SpeechPriority.NORMAL -> TTSEngine.SpeechPriority.NORMAL
                    SpeechPriority.HIGH -> TTSEngine.SpeechPriority.HIGH
                    SpeechPriority.URGENT -> TTSEngine.SpeechPriority.URGENT
                }
            )
        ) ?: false
    }
    
    override suspend fun provideFeedback(event: SpeechFeedbackEvent): Boolean {
        if (!currentSettings.enableFeedback) return true
        
        val text = when (currentSettings.feedbackLevel) {
            FeedbackLevel.SILENT -> return true
            FeedbackLevel.MINIMAL -> getMinimalFeedback(event)
            FeedbackLevel.NORMAL -> getNormalFeedback(event)
            FeedbackLevel.VERBOSE -> getVerboseFeedback(event)
        } ?: return true
        
        return ttsEngine?.speak(
            TTSEngine.SpeechRequest(
                text = text,
                priority = TTSEngine.SpeechPriority.LOW,
                queueMode = TTSEngine.QueueMode.ADD
            )
        ) ?: false
    }
    
    override suspend fun speakResult(result: RecognitionResult): Boolean {
        return ttsEngine?.speakRecognitionResult(result) ?: false
    }
    
    override fun stopSpeaking() {
        ttsEngine?.stopSpeaking()
    }
    
    override fun configure(settings: TTSSettings) {
        currentSettings = settings
        ttsEngine?.let { engine ->
            engine.setSpeechRate(settings.rate)
            engine.setSpeechPitch(settings.pitch)
            engine.setSpeechVolume(settings.volume)
            engine.setSpeechLanguage(settings.language)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                settings.voice?.let { voiceName ->
                    engine.getAvailableVoices().find { it.name == voiceName }?.let { voice ->
                        engine.setSpeechVoice(voice)
                    }
                }
            }
        }
    }
    
    override fun getState(): StateFlow<TTSState> {
        return ttsEngine?.ttsEngineState?.let { engineState ->
            engineState.map { state ->
                TTSState(
                    isReady = state.isReady,
                    isSpeaking = state.isSpeaking,
                    currentLanguage = state.currentLanguage,
                    availableLanguages = state.availableLanguages,
                    availableVoices = state.availableVoices,
                    settings = currentSettings
                )
            }.stateIn(
                scope = CoroutineScope(Dispatchers.Main + SupervisorJob()),
                started = SharingStarted.Lazily,
                initialValue = TTSState()
            )
        } ?: MutableStateFlow(TTSState())
    }
    
    override fun cleanup() {
        ttsEngine?.cleanup()
        ttsEngine = null
    }
    
    private fun getMinimalFeedback(event: SpeechFeedbackEvent): String? {
        return when (event) {
            SpeechFeedbackEvent.RECOGNITION_ERROR -> "Error"
            SpeechFeedbackEvent.COMMAND_EXECUTED -> "Done"
            else -> null
        }
    }
    
    private fun getNormalFeedback(event: SpeechFeedbackEvent): String? {
        return when (event) {
            SpeechFeedbackEvent.LISTENING_STARTED -> "Listening"
            SpeechFeedbackEvent.RECOGNITION_ERROR -> "Recognition error"
            SpeechFeedbackEvent.NO_SPEECH_DETECTED -> "No speech detected"
            SpeechFeedbackEvent.COMMAND_RECOGNIZED -> "Command recognized"
            SpeechFeedbackEvent.COMMAND_EXECUTED -> "Command executed"
            SpeechFeedbackEvent.DICTATION_STARTED -> "Dictation started"
            SpeechFeedbackEvent.DICTATION_STOPPED -> "Dictation stopped"
            else -> null
        }
    }
    
    private fun getVerboseFeedback(event: SpeechFeedbackEvent): String? {
        return when (event) {
            SpeechFeedbackEvent.LISTENING_STARTED -> "Voice recognition started, speak now"
            SpeechFeedbackEvent.LISTENING_STOPPED -> "Voice recognition stopped"
            SpeechFeedbackEvent.PROCESSING -> "Processing your speech"
            SpeechFeedbackEvent.RECOGNITION_SUCCESS -> "Speech recognized successfully"
            SpeechFeedbackEvent.RECOGNITION_ERROR -> "Speech recognition error occurred"
            SpeechFeedbackEvent.NO_SPEECH_DETECTED -> "No speech detected, please try again"
            SpeechFeedbackEvent.TIMEOUT -> "Recognition timeout, please try again"
            SpeechFeedbackEvent.COMMAND_RECOGNIZED -> "Voice command recognized"
            SpeechFeedbackEvent.COMMAND_EXECUTED -> "Voice command executed successfully"
            SpeechFeedbackEvent.DICTATION_STARTED -> "Dictation mode activated"
            SpeechFeedbackEvent.DICTATION_STOPPED -> "Dictation mode deactivated"
        }
    }
}

/**
 * Factory for creating TTS integration instances
 */
object TTSIntegrationFactory {
    
    /**
     * Create default TTS integration
     */
    fun createDefault(): TTSIntegration {
        return DefaultTTSIntegration()
    }
    
    /**
     * Create TTS integration with custom settings
     */
    fun createWithSettings(settings: TTSSettings): TTSIntegration {
        val integration = DefaultTTSIntegration()
        integration.configure(settings)
        return integration
    }
    
    /**
     * Create silent TTS integration (no audio feedback)
     */
    fun createSilent(): TTSIntegration {
        return createWithSettings(
            TTSSettings(
                enableFeedback = false,
                feedbackLevel = FeedbackLevel.SILENT
            )
        )
    }
    
    /**
     * Create minimal feedback TTS integration
     */
    fun createMinimal(): TTSIntegration {
        return createWithSettings(
            TTSSettings(
                enableFeedback = true,
                feedbackLevel = FeedbackLevel.MINIMAL
            )
        )
    }
    
    /**
     * Create verbose TTS integration
     */
    fun createVerbose(): TTSIntegration {
        return createWithSettings(
            TTSSettings(
                enableFeedback = true,
                feedbackLevel = FeedbackLevel.VERBOSE
            )
        )
    }
}