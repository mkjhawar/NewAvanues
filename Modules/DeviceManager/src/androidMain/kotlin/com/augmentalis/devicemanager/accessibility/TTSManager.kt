// Author: Manoj Jhawar
// Purpose: Text-to-Speech management extracted from AccessibilityManager for better SRP

package com.augmentalis.devicemanager.accessibility

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import com.augmentalis.devicemanager.audio.AudioService

/**
 * TTSManager - Dedicated Text-to-Speech management
 * 
 * Handles all TTS functionality including:
 * - Voice synthesis initialization and management
 * - Language and voice selection
 * - Speech rate, pitch, and volume control
 * - Queue management for utterances
 * - Progress callbacks and error handling
 * 
 * COT Reflection:
 * - Single Responsibility: Only handles TTS, nothing else
 * - Clear naming: TTSManager clearly indicates Text-to-Speech
 * - Extracted from AccessibilityManager for better maintainability
 */
class TTSManager(
    private val context: Context,
    private val audioService: AudioService? = null
) : AudioService.AudioFocusChangeListener {
    
    companion object {
        private const val TAG = "TTSManager"
        
        // Preferences
        private const val TTS_PREFS = "tts_preferences"
        private const val PREF_TTS_RATE = "tts_rate"
        private const val PREF_TTS_PITCH = "tts_pitch"
        private const val PREF_TTS_VOLUME = "tts_volume"
        private const val PREF_TTS_LANGUAGE = "tts_language"
        private const val PREF_TTS_VOICE = "tts_voice"
        private const val PREF_TTS_ENGINE = "tts_engine"
        
        // TTS constants
        const val TTS_QUEUE_FLUSH = TextToSpeech.QUEUE_FLUSH
        const val TTS_QUEUE_ADD = TextToSpeech.QUEUE_ADD
        const val DEFAULT_TTS_RATE = 1.0f
        const val DEFAULT_TTS_PITCH = 1.0f
        const val DEFAULT_TTS_VOLUME = 1.0f
        
        // Speech rate limits
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f
        
        // Pitch limits
        const val MIN_PITCH = 0.5f
        const val MAX_PITCH = 2.0f
    }
    
    // System services
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val preferences: SharedPreferences = context.getSharedPreferences(TTS_PREFS, Context.MODE_PRIVATE)
    
    // TTS engine
    private var tts: TextToSpeech? = null
    private var ttsInitialized = false
    
    // State flows
    private val _ttsState = MutableStateFlow(TTSState())
    val ttsState: StateFlow<TTSState> = _ttsState.asStateFlow()
    
    private val _speechResult = MutableSharedFlow<SpeechResult>()
    val speechResult: SharedFlow<SpeechResult> = _speechResult.asSharedFlow()
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    
    // Speech queue
    private val speechQueue = mutableListOf<SpeechRequest>()
    private var isProcessingQueue = false
    
    // ========== DATA MODELS ==========
    
    data class TTSSettings(
        val rate: Float = DEFAULT_TTS_RATE,
        val pitch: Float = DEFAULT_TTS_PITCH,
        val volume: Float = DEFAULT_TTS_VOLUME,
        val language: Locale = Locale.getDefault(),
        val voice: String? = null,
        val engine: String? = null,
        val audioStream: Int = AudioManager.STREAM_ACCESSIBILITY
    )
    
    data class TTSState(
        val isInitialized: Boolean = false,
        val isInitializing: Boolean = false,
        val isSpeaking: Boolean = false,
        val currentLanguage: Locale? = null,
        val availableLanguages: List<Locale> = emptyList(),
        val availableVoices: List<Voice> = emptyList(),
        val currentVoice: Voice? = null,
        val settings: TTSSettings = TTSSettings(),
        val engineInfo: TTSEngineInfo? = null,
        val queuedUtterances: List<String> = emptyList()
    )
    
    data class TTSEngineInfo(
        val packageName: String,
        val label: String,
        val version: String?,
        val features: List<String>,
        val defaultEngine: Boolean
    )
    
    data class SpeechRequest(
        val text: String,
        val queueMode: Int = TTS_QUEUE_FLUSH,
        val params: Map<String, String>? = null,
        val utteranceId: String? = null,
        val priority: SpeechPriority = SpeechPriority.NORMAL
    )
    
    enum class SpeechPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
    
    data class SpeechResult(
        val utteranceId: String?,
        val success: Boolean,
        val error: String? = null,
        val duration: Long? = null,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    // ========== INITIALIZATION ==========
    
    init {
        initializeTTS()
        loadPreferences()
        // Register for audio focus changes if AudioService is available
        audioService?.addAudioFocusChangeListener(this)
    }
    
    private fun initializeTTS() {
        _ttsState.update { it.copy(isInitializing = true) }
        
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialized = true
                setupTTSEngine()
                _ttsState.update { 
                    it.copy(
                        isInitialized = true,
                        isInitializing = false
                    )
                }
                Log.d(TAG, "TTS initialized successfully")
            } else {
                _ttsState.update { 
                    it.copy(
                        isInitialized = false,
                        isInitializing = false
                    )
                }
                Log.e(TAG, "TTS initialization failed with status: $status")
            }
        }
    }
    
    private fun setupTTSEngine() {
        tts?.let { engine ->
            // Set up utterance listener
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _ttsState.update { it.copy(isSpeaking = true) }
                    Log.d(TAG, "Speech started: $utteranceId")
                }
                
                override fun onDone(utteranceId: String?) {
                    _ttsState.update { it.copy(isSpeaking = false) }
                    scope.launch {
                        _speechResult.emit(
                            SpeechResult(
                                utteranceId = utteranceId,
                                success = true
                            )
                        )
                    }
                    processNextInQueue()
                }
                
                @Deprecated("Deprecated in API level 21")
                override fun onError(utteranceId: String?) {
                    handleSpeechError(utteranceId, "Unknown error")
                }
                
                override fun onError(utteranceId: String?, errorCode: Int) {
                    val errorMessage = mapErrorCode(errorCode)
                    handleSpeechError(utteranceId, errorMessage)
                }
            })
            
            // Load available languages and voices
            updateAvailableLanguages()
            updateAvailableVoices()
            
            // Apply saved settings
            applyStoredSettings()
        }
    }
    
    private fun loadPreferences() {
        val settings = TTSSettings(
            rate = preferences.getFloat(PREF_TTS_RATE, DEFAULT_TTS_RATE),
            pitch = preferences.getFloat(PREF_TTS_PITCH, DEFAULT_TTS_PITCH),
            volume = preferences.getFloat(PREF_TTS_VOLUME, DEFAULT_TTS_VOLUME),
            language = getStoredLanguage(),
            voice = preferences.getString(PREF_TTS_VOICE, null),
            engine = preferences.getString(PREF_TTS_ENGINE, null)
        )
        
        _ttsState.update { it.copy(settings = settings) }
    }
    
    private fun getStoredLanguage(): Locale {
        val languageCode = preferences.getString(PREF_TTS_LANGUAGE, null)
        return if (languageCode != null) {
            Locale.forLanguageTag(languageCode)
        } else {
            Locale.getDefault()
        }
    }
    
    // ========== PUBLIC API ==========
    
    /**
     * Speak text with optional parameters
     */
    fun speak(text: String, queueMode: Int = TTS_QUEUE_FLUSH, utteranceId: String? = null): Boolean {
        if (!ttsInitialized) {
            Log.w(TAG, "TTS not initialized")
            return false
        }
        
        // Request audio focus before speaking
        requestAudioFocusForSpeech()
        
        val request = SpeechRequest(
            text = text,
            queueMode = queueMode,
            utteranceId = utteranceId
        )
        
        return if (queueMode == TTS_QUEUE_ADD) {
            addToQueue(request)
        } else {
            speakImmediate(request)
        }
    }
    
    /**
     * Speak with priority
     */
    fun speakWithPriority(text: String, priority: SpeechPriority): Boolean {
        val request = SpeechRequest(
            text = text,
            priority = priority,
            queueMode = if (priority == SpeechPriority.URGENT) TTS_QUEUE_FLUSH else TTS_QUEUE_ADD
        )
        
        return if (priority == SpeechPriority.URGENT) {
            speakImmediate(request)
        } else {
            addToQueue(request)
        }
    }
    
    /**
     * Stop speaking
     */
    fun stop(): Boolean {
        val result = tts?.stop() == TextToSpeech.SUCCESS
        // Abandon audio focus when stopping
        if (result) {
            audioService?.abandonAudioFocus("TTS stopped")
        }
        return result
    }
    
    /**
     * Set speech rate
     */
    fun setSpeechRate(rate: Float): Boolean {
        val clampedRate = rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        tts?.setSpeechRate(clampedRate)
        
        preferences.edit().putFloat(PREF_TTS_RATE, clampedRate).apply()
        _ttsState.update { it.copy(settings = it.settings.copy(rate = clampedRate)) }
        
        return true
    }
    
    /**
     * Set pitch
     */
    fun setPitch(pitch: Float): Boolean {
        val clampedPitch = pitch.coerceIn(MIN_PITCH, MAX_PITCH)
        tts?.setPitch(clampedPitch)
        
        preferences.edit().putFloat(PREF_TTS_PITCH, clampedPitch).apply()
        _ttsState.update { it.copy(settings = it.settings.copy(pitch = clampedPitch)) }
        
        return true
    }
    
    /**
     * Set language
     */
    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        
        return if (result == TextToSpeech.LANG_AVAILABLE || 
                   result == TextToSpeech.LANG_COUNTRY_AVAILABLE || 
                   result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
            preferences.edit().putString(PREF_TTS_LANGUAGE, locale.toLanguageTag()).apply()
            _ttsState.update { 
                it.copy(
                    currentLanguage = locale,
                    settings = it.settings.copy(language = locale)
                )
            }
            updateAvailableVoices()
            true
        } else {
            Log.w(TAG, "Language not available: $locale")
            false
        }
    }
    
    /**
     * Set voice
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setVoice(voice: Voice): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val result = tts?.setVoice(voice)
            if (result == TextToSpeech.SUCCESS) {
                preferences.edit().putString(PREF_TTS_VOICE, voice.name).apply()
                _ttsState.update { it.copy(currentVoice = voice) }
                true
            } else {
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Get available languages
     */
    fun getAvailableLanguages(): List<Locale> {
        return tts?.availableLanguages?.toList() ?: emptyList()
    }
    
    /**
     * Get available voices for current language
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun getAvailableVoices(): List<Voice> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.voices?.filter { 
                it.locale == _ttsState.value.currentLanguage 
            } ?: emptyList()
        } else {
            emptyList()
        }
    }
    
    /**
     * Check if language is available
     */
    fun isLanguageAvailable(locale: Locale): Boolean {
        val result = tts?.isLanguageAvailable(locale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        return result >= TextToSpeech.LANG_AVAILABLE
    }
    
    // ========== PRIVATE METHODS ==========
    
    private fun speakImmediate(request: SpeechRequest): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, request.utteranceId)
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ACCESSIBILITY)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, _ttsState.value.settings.volume)
            }
            tts?.speak(request.text, request.queueMode, params, request.utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = hashMapOf(
                TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to request.utteranceId
            )
            @Suppress("DEPRECATION")
            tts?.speak(request.text, request.queueMode, params)
        }
        return result == TextToSpeech.SUCCESS
    }
    
    private fun addToQueue(request: SpeechRequest): Boolean {
        speechQueue.add(request)
        _ttsState.update { 
            it.copy(queuedUtterances = speechQueue.map { req -> req.text })
        }
        
        if (!isProcessingQueue) {
            processNextInQueue()
        }
        
        return true
    }
    
    private fun processNextInQueue() {
        if (speechQueue.isEmpty()) {
            isProcessingQueue = false
            return
        }
        
        isProcessingQueue = true
        val request = speechQueue.removeAt(0)
        speakImmediate(request)
        
        _ttsState.update { 
            it.copy(queuedUtterances = speechQueue.map { it.text })
        }
    }
    
    private fun updateAvailableLanguages() {
        val languages = getAvailableLanguages()
        _ttsState.update { it.copy(availableLanguages = languages) }
    }
    
    private fun updateAvailableVoices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val voices = getAvailableVoices()
            _ttsState.update { it.copy(availableVoices = voices) }
        }
    }
    
    private fun applyStoredSettings() {
        val settings = _ttsState.value.settings
        setSpeechRate(settings.rate)
        setPitch(settings.pitch)
        setLanguage(settings.language)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && settings.voice != null) {
            val voice = _ttsState.value.availableVoices.find { it.name == settings.voice }
            voice?.let { setVoice(it) }
        }
    }
    
    private fun handleSpeechError(utteranceId: String?, errorMessage: String) {
        _ttsState.update { it.copy(isSpeaking = false) }
        scope.launch {
            _speechResult.emit(
                SpeechResult(
                    utteranceId = utteranceId,
                    success = false,
                    error = errorMessage
                )
            )
        }
        Log.e(TAG, "Speech error for utterance $utteranceId: $errorMessage")
        processNextInQueue()
    }
    
    private fun mapErrorCode(errorCode: Int): String {
        return when (errorCode) {
            TextToSpeech.ERROR_SYNTHESIS -> "Synthesis error"
            TextToSpeech.ERROR_SERVICE -> "Service error"
            TextToSpeech.ERROR_OUTPUT -> "Output error"
            TextToSpeech.ERROR_NETWORK -> "Network error"
            TextToSpeech.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            TextToSpeech.ERROR_INVALID_REQUEST -> "Invalid request"
            TextToSpeech.ERROR_NOT_INSTALLED_YET -> "Not installed yet"
            else -> "Unknown error: $errorCode"
        }
    }
    
    // ========== CLEANUP ==========
    
    // ========== AUDIO FOCUS HANDLING ==========
    
    /**
     * Request audio focus for speech
     */
    private fun requestAudioFocusForSpeech() {
        audioService?.requestAudioFocus(
            focusGain = AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
            usage = AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY,
            contentType = AudioAttributes.CONTENT_TYPE_SPEECH
        )
    }
    
    /**
     * AudioFocusChangeListener implementation
     */
    override fun onAudioFocusGained(focusType: Int) {
        Log.d(TAG, "Audio focus gained: $focusType")
        // Resume TTS if it was paused due to focus loss
        if (_ttsState.value.isSpeaking && speechQueue.isNotEmpty()) {
            processNextInQueue()
        }
    }
    
    override fun onAudioFocusLost() {
        Log.d(TAG, "Audio focus lost permanently - stopping TTS")
        stop()
    }
    
    override fun onAudioFocusLostTransient() {
        Log.d(TAG, "Audio focus lost transiently - pausing TTS")
        tts?.stop() // Pause current utterance
        _ttsState.update { it.copy(isSpeaking = false) }
    }
    
    override fun onAudioFocusLostTransientCanDuck() {
        Log.d(TAG, "Audio focus lost transiently (can duck) - lowering volume")
        // For TTS, we could lower the volume, but it's better to pause briefly
        // Since TTS is usually important accessibility information
        onAudioFocusLostTransient()
    }
    
    // ========== CLEANUP ==========
    
    fun release() {
        stop()
        speechQueue.clear()
        
        // Unregister from audio focus changes
        audioService?.removeAudioFocusChangeListener(this)
        audioService?.abandonAudioFocus("TTS released")
        
        tts?.shutdown()
        tts = null
        ttsInitialized = false
        scope.cancel()
    }
}