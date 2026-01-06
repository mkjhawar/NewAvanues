/**
 * AndroidSTTEngineImpl.kt - Real Android SpeechRecognizer implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Production implementation of ISpeechEngine using Android's native SpeechRecognizer.
 * Uses flows for reactive result delivery.
 */
package com.augmentalis.voiceoscoreng.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Production Android STT engine using native SpeechRecognizer.
 *
 * Features:
 * - Uses Android's built-in speech recognition (Google)
 * - Reactive via Kotlin Flows
 * - Continuous recognition mode with auto-restart
 * - Partial results support
 *
 * Requirements:
 * - RECORD_AUDIO permission
 * - Network connectivity (uses Google servers)
 * - Google Play Services (most devices)
 *
 * @param context Android application context
 */
class AndroidSTTEngineImpl(private val context: Context) : ISpeechEngine {

    companion object {
        private const val TAG = "AndroidSTTEngine"
        private const val RESTART_DELAY_MS = 500L
    }

    // ═══════════════════════════════════════════════════════════════════
    // Flows (ISpeechEngine interface)
    // ═══════════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _results = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 16)
    override val results: SharedFlow<SpeechResult> = _results.asSharedFlow()

    private val _errors = MutableSharedFlow<SpeechError>(extraBufferCapacity = 8)
    override val errors: SharedFlow<SpeechError> = _errors.asSharedFlow()

    // ═══════════════════════════════════════════════════════════════════
    // Internal State
    // ═══════════════════════════════════════════════════════════════════

    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionIntent: Intent? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentConfig: SpeechConfig? = null
    private var isListeningInternal = false
    private var continuousMode = false
    private var shouldRestart = false

    // ═══════════════════════════════════════════════════════════════════
    // Lifecycle Methods
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return try {
            Log.d(TAG, "Initializing Android STT engine")

            // Check availability
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                val error = "Speech recognition not available on this device"
                Log.e(TAG, error)
                _state.value = EngineState.Error(error, recoverable = false)
                return Result.failure(IllegalStateException(error))
            }

            currentConfig = config

            // Create SpeechRecognizer on main thread
            mainHandler.post {
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                    speechRecognizer?.setRecognitionListener(createRecognitionListener())

                    // Create recognition intent
                    recognitionIntent = createRecognitionIntent(config)

                    _state.value = EngineState.Ready(SpeechEngine.ANDROID_STT)
                    Log.i(TAG, "Android STT engine initialized successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create SpeechRecognizer", e)
                    _state.value = EngineState.Error(e.message ?: "Initialization failed", recoverable = false)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Android STT", e)
            _state.value = EngineState.Error(e.message ?: "Unknown error", recoverable = false)
            Result.failure(e)
        }
    }

    override suspend fun startListening(): Result<Unit> {
        return try {
            if (_state.value !is EngineState.Ready && _state.value !is EngineState.Listening) {
                return Result.failure(IllegalStateException("Engine not ready: ${_state.value}"))
            }

            Log.d(TAG, "Starting Android STT listening")
            shouldRestart = true
            continuousMode = currentConfig?.mode?.supportsContinuous() == true

            mainHandler.post {
                try {
                    speechRecognizer?.startListening(recognitionIntent)
                    isListeningInternal = true
                    _state.value = EngineState.Listening
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start listening", e)
                    isListeningInternal = false
                    _state.value = EngineState.Error(e.message ?: "Start failed", recoverable = true)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting STT", e)
            Result.failure(e)
        }
    }

    override suspend fun stopListening() {
        Log.d(TAG, "Stopping Android STT listening")
        shouldRestart = false
        isListeningInternal = false

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                _state.value = EngineState.Ready(SpeechEngine.ANDROID_STT)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping STT", e)
            }
        }
    }

    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        // Android STT doesn't support dynamic command vocabularies
        // Commands are matched at a higher level (CommandMatcher)
        Log.d(TAG, "updateCommands: Android STT uses free-form recognition, ${commands.size} commands stored for matching")
        return Result.success(Unit)
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        currentConfig = config
        recognitionIntent = createRecognitionIntent(config)
        Log.d(TAG, "Configuration updated: language=${config.language}, mode=${config.mode}")
        return Result.success(Unit)
    }

    override fun isRecognizing(): Boolean = isListeningInternal

    override fun isInitialized(): Boolean = speechRecognizer != null

    override fun getEngineType(): SpeechEngine = SpeechEngine.ANDROID_STT

    override fun getSupportedFeatures(): Set<EngineFeature> = setOf(
        EngineFeature.CONTINUOUS_RECOGNITION,
        EngineFeature.PUNCTUATION
    )

    override suspend fun destroy() {
        Log.d(TAG, "Destroying Android STT engine")
        shouldRestart = false
        isListeningInternal = false

        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
                recognitionIntent = null
                _state.value = EngineState.Destroyed
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying STT", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Recognition Listener
    // ═══════════════════════════════════════════════════════════════════

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
                _state.value = EngineState.Listening
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech detected")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level - can emit for visualization
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Raw audio buffer
            }

            override fun onEndOfSpeech() {
                Log.d(TAG, "End of speech")
            }

            override fun onError(error: Int) {
                val errorInfo = mapError(error)
                Log.e(TAG, "Recognition error: ${errorInfo.first} (code: $error)")

                scope.launch {
                    _errors.emit(SpeechError(
                        code = errorInfo.second,
                        message = errorInfo.first,
                        recoverable = errorInfo.third
                    ))
                }

                isListeningInternal = false
                _state.value = EngineState.Ready(SpeechEngine.ANDROID_STT)

                // Auto-restart in continuous mode for recoverable errors
                if (continuousMode && shouldRestart && errorInfo.third) {
                    mainHandler.postDelayed({
                        if (shouldRestart) {
                            Log.d(TAG, "Auto-restarting after recoverable error")
                            speechRecognizer?.startListening(recognitionIntent)
                            isListeningInternal = true
                            _state.value = EngineState.Listening
                        }
                    }, RESTART_DELAY_MS)
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidences = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)

                if (matches != null && matches.isNotEmpty()) {
                    val text = matches[0]
                    val confidence = confidences?.getOrNull(0) ?: 0.8f

                    Log.d(TAG, "Final result: '$text' (confidence: $confidence)")

                    scope.launch {
                        _results.emit(SpeechResult(
                            text = text,
                            confidence = confidence,
                            isFinal = true,
                            alternatives = matches.drop(1).mapIndexed { idx, alt ->
                                SpeechResult.Alternative(alt, confidences?.getOrNull(idx + 1) ?: 0f)
                            },
                            timestamp = System.currentTimeMillis()
                        ))
                    }
                }

                isListeningInternal = false
                _state.value = EngineState.Ready(SpeechEngine.ANDROID_STT)

                // Auto-restart in continuous mode
                if (continuousMode && shouldRestart) {
                    mainHandler.postDelayed({
                        if (shouldRestart) {
                            Log.d(TAG, "Auto-restarting for continuous mode")
                            speechRecognizer?.startListening(recognitionIntent)
                            isListeningInternal = true
                            _state.value = EngineState.Listening
                        }
                    }, RESTART_DELAY_MS)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null && matches.isNotEmpty()) {
                    val text = matches[0]
                    Log.d(TAG, "Partial result: '$text'")

                    scope.launch {
                        _results.emit(SpeechResult(
                            text = text,
                            confidence = 0.5f,
                            isFinal = false,
                            alternatives = emptyList(),
                            timestamp = System.currentTimeMillis()
                        ))
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                Log.d(TAG, "Recognition event: $eventType")
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    private fun createRecognitionIntent(config: SpeechConfig): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, config.language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)

            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, config.silenceTimeout)

            // Prefer offline recognition if available (Android 23+)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, config.preferOffline)
        }
    }

    /**
     * Map Android error codes to our error model
     * Returns: (message, code, isRecoverable)
     */
    private fun mapError(errorCode: Int): Triple<String, SpeechError.ErrorCode, Boolean> {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO ->
                Triple("Audio recording error", SpeechError.ErrorCode.AUDIO_ERROR, true)
            SpeechRecognizer.ERROR_CLIENT ->
                Triple("Client side error", SpeechError.ErrorCode.UNKNOWN, true)
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                Triple("Insufficient permissions", SpeechError.ErrorCode.PERMISSION_DENIED, false)
            SpeechRecognizer.ERROR_NETWORK ->
                Triple("Network error", SpeechError.ErrorCode.NETWORK_ERROR, true)
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                Triple("Network timeout", SpeechError.ErrorCode.TIMEOUT, true)
            SpeechRecognizer.ERROR_NO_MATCH ->
                Triple("No speech detected", SpeechError.ErrorCode.NO_SPEECH_DETECTED, true)
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                Triple("Recognizer busy", SpeechError.ErrorCode.ENGINE_BUSY, true)
            SpeechRecognizer.ERROR_SERVER ->
                Triple("Server error", SpeechError.ErrorCode.RECOGNITION_FAILED, true)
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                Triple("No speech input", SpeechError.ErrorCode.TIMEOUT, true)
            else ->
                Triple("Unknown error ($errorCode)", SpeechError.ErrorCode.UNKNOWN, true)
        }
    }
}
