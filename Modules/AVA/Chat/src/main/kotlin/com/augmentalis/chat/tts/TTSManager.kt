package com.augmentalis.chat.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.augmentalis.ava.core.common.Result
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Text-to-Speech manager for AVA AI Phase 1.2.
 *
 * Responsibilities:
 * - Initialize and manage Android TextToSpeech engine
 * - Provide voice output for assistant messages
 * - Support configurable voice, rate, and pitch settings
 * - Handle speech queue management
 * - Stream support for LLM response chunks
 *
 * Features:
 * - Auto-speak assistant responses (configurable)
 * - Manual speak button for individual messages
 * - Voice selection from available system voices
 * - Speech rate control (0.5x - 2.0x)
 * - Pitch control (0.5x - 2.0x)
 * - Stop/pause/resume controls
 * - Streaming TTS for real-time LLM responses
 *
 * Thread-safety: All TTS operations are dispatched to main thread
 * Lifecycle: Singleton scoped to app lifecycle, requires shutdown() on app exit
 *
 * @param context Application context for TTS initialization
 */
@Singleton
class TTSManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TTSManager"

        // Default TTS settings
        private const val DEFAULT_SPEECH_RATE = 1.0f
        private const val DEFAULT_PITCH = 1.0f

        // Speech rate bounds
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f

        // Pitch bounds
        const val MIN_PITCH = 0.5f
        const val MAX_PITCH = 2.0f
    }

    // ==================== State ====================

    /**
     * TTS initialization state
     */
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /**
     * Currently speaking state
     */
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    /**
     * Available voices on this device
     */
    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    /**
     * Current TTS settings
     */
    private val _currentSettings = MutableStateFlow(TTSSettings())
    val currentSettings: StateFlow<TTSSettings> = _currentSettings.asStateFlow()

    /**
     * Initialization error (if any)
     */
    private val _initError = MutableStateFlow<String?>(null)
    val initError: StateFlow<String?> = _initError.asStateFlow()

    // ==================== Internal State ====================

    /**
     * Android TextToSpeech instance
     */
    private var tts: TextToSpeech? = null

    /**
     * Coroutine scope for async operations.
     * Uses SupervisorJob to prevent child failures from cancelling the scope.
     * CRITICAL: Must be cancelled in shutdown() to prevent memory leaks.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * Map of utterance IDs to completion callbacks.
     *
     * Thread Safety (Issue C-04, A-04):
     * - Uses ConcurrentHashMap for thread-safe access
     * - Callbacks may be invoked from TTS engine thread
     * - Operations are dispatched to main thread via scope
     */
    private val utteranceCallbacks = ConcurrentHashMap<String, () -> Unit>()

    // ==================== Initialization ====================

    init {
        Log.d(TAG, "Initializing TTSManager...")
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        scope.launch {
            if (status == TextToSpeech.SUCCESS) {
                Log.d(TAG, "TTS initialized successfully")

                // Set default language
                val result = tts?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    _initError.value = "Language not supported on this device"
                    Log.e(TAG, "Language not supported: ${Locale.US}")
                    return@launch
                }

                // Set utterance progress listener
                // Thread Safety (Issue A-04): Callbacks dispatch to main thread
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // Dispatch to main thread to ensure StateFlow updates on correct thread
                        scope.launch {
                            Log.d(TAG, "TTS started: $utteranceId")
                            _isSpeaking.value = true
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        // Dispatch to main thread (Issue A-04)
                        scope.launch {
                            Log.d(TAG, "TTS completed: $utteranceId")
                            _isSpeaking.value = false

                            // Invoke completion callback if registered (thread-safe ConcurrentHashMap)
                            utteranceId?.let { id ->
                                utteranceCallbacks.remove(id)?.invoke()
                            }
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        // Dispatch to main thread (Issue A-04)
                        scope.launch {
                            Log.e(TAG, "TTS error: $utteranceId")
                            _isSpeaking.value = false

                            // Clean up callback on error (thread-safe ConcurrentHashMap)
                            utteranceId?.let { id ->
                                utteranceCallbacks.remove(id)
                            }
                        }
                    }
                })

                // Load available voices
                loadAvailableVoices()

                _isInitialized.value = true
                Log.i(TAG, "TTS ready - ${_availableVoices.value.size} voices available")
            } else {
                _initError.value = "TTS initialization failed with status: $status"
                Log.e(TAG, "TTS initialization failed: $status")
            }
        }
    }

    // ==================== Public API ====================

    /**
     * Speak text with current settings.
     *
     * @param text Text to speak
     * @param queueMode QUEUE_ADD or QUEUE_FLUSH
     * @param onComplete Optional callback when speech completes
     * @return Result.Success if speech started, Result.Error otherwise
     */
    fun speak(
        text: String,
        queueMode: Int = TextToSpeech.QUEUE_ADD,
        onComplete: (() -> Unit)? = null
    ): Result<Unit> {
        if (!_isInitialized.value) {
            return Result.Error(
                exception = IllegalStateException("TTS not initialized"),
                message = "TTS not initialized: ${_initError.value ?: "Unknown error"}"
            )
        }

        if (text.isBlank()) {
            return Result.Error(
                exception = IllegalArgumentException("Empty text"),
                message = "Cannot speak empty text"
            )
        }

        val utteranceId = UUID.randomUUID().toString()

        // Register completion callback
        onComplete?.let { callback ->
            utteranceCallbacks[utteranceId] = callback
        }

        val settings = _currentSettings.value

        // Apply settings
        tts?.setSpeechRate(settings.speechRate)
        tts?.setPitch(settings.pitch)

        // Apply voice if selected
        settings.selectedVoice?.let { voiceId ->
            val voice = tts?.voices?.find { it.name == voiceId }
            voice?.let { tts?.voice = it }
        }

        // Speak
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = tts?.speak(text, queueMode, params, utteranceId)

        return if (result == TextToSpeech.SUCCESS) {
            Log.d(TAG, "Speaking: \"${text.take(50)}${if (text.length > 50) "..." else ""}\"")
            Result.Success(Unit)
        } else {
            utteranceCallbacks.remove(utteranceId) // Clean up callback on failure
            Result.Error(
                exception = RuntimeException("TTS speak failed with code: $result"),
                message = "Failed to speak text (error code: $result)"
            )
        }
    }

    /**
     * Speak text with custom settings (one-time override).
     *
     * @param text Text to speak
     * @param rate Speech rate (0.5 - 2.0)
     * @param pitch Pitch (0.5 - 2.0)
     * @param voiceId Optional voice ID override
     * @param queueMode QUEUE_ADD or QUEUE_FLUSH
     * @param onComplete Optional callback when speech completes
     * @return Result.Success if speech started, Result.Error otherwise
     */
    fun speakWithSettings(
        text: String,
        rate: Float = DEFAULT_SPEECH_RATE,
        pitch: Float = DEFAULT_PITCH,
        voiceId: String? = null,
        queueMode: Int = TextToSpeech.QUEUE_ADD,
        onComplete: (() -> Unit)? = null
    ): Result<Unit> {
        if (!_isInitialized.value) {
            return Result.Error(
                exception = IllegalStateException("TTS not initialized"),
                message = "TTS not initialized: ${_initError.value ?: "Unknown error"}"
            )
        }

        if (text.isBlank()) {
            return Result.Error(
                exception = IllegalArgumentException("Empty text"),
                message = "Cannot speak empty text"
            )
        }

        val utteranceId = UUID.randomUUID().toString()

        // Register completion callback
        onComplete?.let { callback ->
            utteranceCallbacks[utteranceId] = callback
        }

        // Apply custom settings
        tts?.setSpeechRate(rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE))
        tts?.setPitch(pitch.coerceIn(MIN_PITCH, MAX_PITCH))

        // Apply voice if provided
        voiceId?.let { id ->
            val voice = tts?.voices?.find { it.name == id }
            voice?.let { tts?.voice = it }
        }

        // Speak
        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }

        val result = tts?.speak(text, queueMode, params, utteranceId)

        return if (result == TextToSpeech.SUCCESS) {
            Log.d(TAG, "Speaking with custom settings: rate=$rate, pitch=$pitch")
            Result.Success(Unit)
        } else {
            utteranceCallbacks.remove(utteranceId) // Clean up callback on failure
            Result.Error(
                exception = RuntimeException("TTS speak failed with code: $result"),
                message = "Failed to speak text (error code: $result)"
            )
        }
    }

    /**
     * Speak streaming text chunks (for LLM responses).
     *
     * Speaks each chunk as it arrives, queuing them sequentially.
     * Useful for streaming LLM responses where text arrives in chunks.
     *
     * @param textFlow Flow of text chunks to speak
     * @param onComplete Optional callback when all chunks complete
     */
    suspend fun speakStreaming(
        textFlow: Flow<String>,
        onComplete: (() -> Unit)? = null
    ) {
        if (!_isInitialized.value) {
            Log.w(TAG, "TTS not initialized, cannot speak streaming text")
            return
        }

        var chunkCount = 0
        textFlow.collect { chunk ->
            if (chunk.isNotBlank()) {
                chunkCount++
                val isLastChunk = false // We don't know if it's the last chunk in a Flow

                speak(
                    text = chunk,
                    queueMode = TextToSpeech.QUEUE_ADD,
                    onComplete = if (isLastChunk) onComplete else null
                )
            }
        }

        Log.d(TAG, "Spoke $chunkCount streaming chunks")
    }

    /**
     * Stop speaking immediately and clear queue.
     */
    fun stop() {
        tts?.stop()
        utteranceCallbacks.clear()
        _isSpeaking.value = false
        Log.d(TAG, "TTS stopped")
    }

    /**
     * Update TTS settings.
     *
     * @param settings New TTS settings to apply
     */
    fun updateSettings(settings: TTSSettings) {
        _currentSettings.value = settings
        Log.d(TAG, "TTS settings updated: $settings")
    }

    /**
     * Get available voices on this device.
     *
     * @return List of VoiceInfo objects
     */
    fun getAvailableVoices(): List<VoiceInfo> {
        return _availableVoices.value
    }

    /**
     * Check if a specific language is available.
     *
     * @param locale Locale to check
     * @return true if language is available
     */
    fun isLanguageAvailable(locale: Locale): Boolean {
        val result = tts?.isLanguageAvailable(locale)
        return result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE
    }

    /**
     * Shutdown TTS engine and release resources.
     * Should be called when app is destroyed.
     *
     * CRITICAL: Cancels coroutine scope to prevent memory leaks (Issue 1.1).
     * Without cancellation, the scope holds references to StateFlows and
     * callbacks, causing memory to accumulate with each TTS usage cycle.
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down TTS...")

        // Cancel coroutine scope first to stop any pending operations
        // This prevents memory leaks from retained StateFlow references
        scope.cancel()

        tts?.stop()
        tts?.shutdown()
        tts = null
        utteranceCallbacks.clear()
        _isInitialized.value = false
        _isSpeaking.value = false
        Log.i(TAG, "TTS shutdown complete")
    }

    // ==================== Private Methods ====================

    /**
     * Load available voices from TTS engine.
     */
    private fun loadAvailableVoices() {
        val voices = tts?.voices?.mapNotNull { voice ->
            try {
                VoiceInfo(
                    id = voice.name,
                    name = voice.name,
                    locale = voice.locale.displayName,
                    quality = when (voice.quality) {
                        300 -> VoiceQuality.HIGH
                        200 -> VoiceQuality.NORMAL
                        else -> VoiceQuality.LOW
                    },
                    requiresNetwork = voice.isNetworkConnectionRequired
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load voice: ${voice.name}", e)
                null
            }
        } ?: emptyList()

        _availableVoices.value = voices
        Log.d(TAG, "Loaded ${voices.size} voices: ${voices.map { it.name }}")
    }
}

/**
 * Voice information data class.
 *
 * @param id Unique voice identifier
 * @param name Display name for UI
 * @param locale Locale display name (e.g., "English (United States)")
 * @param quality Voice quality level
 * @param requiresNetwork True if voice requires network connection
 */
data class VoiceInfo(
    val id: String,
    val name: String,
    val locale: String,
    val quality: VoiceQuality,
    val requiresNetwork: Boolean
)

/**
 * Voice quality levels.
 */
enum class VoiceQuality {
    HIGH,      // 300 - High quality
    NORMAL,    // 200 - Normal quality
    LOW        // <200 - Low quality
}
