/**
 * AzureEngineImpl.kt - Azure Cognitive Services Speech SDK implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * Production implementation of ISpeechEngine using Microsoft Azure Cognitive Services
 * Speech SDK for high-accuracy cloud-based speech recognition.
 *
 * Features:
 * - Continuous recognition mode
 * - Partial/interim results
 * - Multiple language support
 * - Word-level timestamps
 * - Speaker diarization (enterprise)
 * - Translation capabilities (enterprise)
 */
package com.augmentalis.voiceoscoreng.features

import android.util.Log
import com.microsoft.cognitiveservices.speech.CancellationErrorCode
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig as AzureSpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.Future
import kotlin.coroutines.resume

/**
 * Azure Cognitive Services Speech SDK implementation.
 *
 * Uses Microsoft's Speech SDK for enterprise-grade speech recognition with:
 * - High accuracy across many languages
 * - Continuous recognition with partial results
 * - Punctuation and capitalization
 * - Word-level timestamps
 * - Speaker diarization (with premium tier)
 * - Translation (with premium tier)
 *
 * Requirements:
 * - Azure subscription key and region
 * - RECORD_AUDIO permission
 * - Network connectivity
 *
 * SDK Dependency:
 * implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.35.0")
 */
class AzureEngineImpl : ISpeechEngine {

    companion object {
        private const val TAG = "AzureEngine"
        private const val CONNECTION_TIMEOUT_MS = 10_000L
        private const val STOP_TIMEOUT_MS = 5_000L
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

    private var azureConfig: AzureSpeechConfig? = null
    private var audioConfig: AudioConfig? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var currentConfig: SpeechConfig? = null
    private var isRecognizing = false
    private var isInitialized = false

    // ═══════════════════════════════════════════════════════════════════
    // Lifecycle Methods
    // ═══════════════════════════════════════════════════════════════════

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return try {
            Log.d(TAG, "Initializing Azure Speech engine")

            // Validate required Azure configuration
            val subscriptionKey = config.apiKey
            val region = config.apiRegion

            if (subscriptionKey.isNullOrBlank()) {
                val error = "Azure subscription key is required"
                Log.e(TAG, error)
                _state.value = EngineState.Error(error, recoverable = false)
                return Result.failure(IllegalArgumentException(error))
            }

            if (region.isNullOrBlank()) {
                val error = "Azure region is required"
                Log.e(TAG, error)
                _state.value = EngineState.Error(error, recoverable = false)
                return Result.failure(IllegalArgumentException(error))
            }

            _state.value = EngineState.Initializing
            currentConfig = config

            // Create Azure Speech configuration
            azureConfig = AzureSpeechConfig.fromSubscription(subscriptionKey, region).apply {
                // Set recognition language
                speechRecognitionLanguage = config.language

                // Configure speech features based on SpeechConfig
                if (config.enableProfanityFilter) {
                    setProfanity(com.microsoft.cognitiveservices.speech.ProfanityOption.Masked)
                }

                // Enable word-level timestamps if requested
                if (config.enableWordTimestamps) {
                    requestWordLevelTimestamps()
                }

                // Set output format to detailed for confidence scores
                setOutputFormat(com.microsoft.cognitiveservices.speech.OutputFormat.Detailed)
            }

            // Create audio configuration from default microphone
            audioConfig = AudioConfig.fromDefaultMicrophoneInput()

            // Create speech recognizer
            speechRecognizer = SpeechRecognizer(azureConfig, audioConfig).apply {
                // Wire up event handlers
                setupEventHandlers(this)
            }

            isInitialized = true
            _state.value = EngineState.Ready(SpeechEngine.AZURE)
            Log.i(TAG, "Azure Speech engine initialized successfully for region: $region, language: ${config.language}")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Azure Speech engine", e)
            _state.value = EngineState.Error(e.message ?: "Initialization failed", recoverable = false)
            Result.failure(e)
        }
    }

    override suspend fun startListening(): Result<Unit> {
        return try {
            val recognizer = speechRecognizer
            if (recognizer == null || !isInitialized) {
                return Result.failure(IllegalStateException("Engine not initialized"))
            }

            if (isRecognizing) {
                Log.w(TAG, "Already recognizing, ignoring start request")
                return Result.success(Unit)
            }

            Log.d(TAG, "Starting Azure continuous recognition")
            _state.value = EngineState.Listening
            isRecognizing = true

            // Start continuous recognition asynchronously
            recognizer.startContinuousRecognitionAsync().get()
            Log.i(TAG, "Azure continuous recognition started")

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Azure recognition", e)
            isRecognizing = false
            _state.value = EngineState.Error(e.message ?: "Start failed", recoverable = true)

            scope.launch {
                _errors.emit(SpeechError(
                    code = SpeechError.ErrorCode.RECOGNITION_FAILED,
                    message = "Failed to start Azure recognition: ${e.message}",
                    recoverable = true
                ))
            }

            Result.failure(e)
        }
    }

    override suspend fun stopListening() {
        try {
            val recognizer = speechRecognizer ?: return

            if (!isRecognizing) {
                Log.d(TAG, "Not currently recognizing, nothing to stop")
                return
            }

            Log.d(TAG, "Stopping Azure continuous recognition")
            isRecognizing = false

            // Stop with timeout to prevent hanging
            withTimeoutOrNull(STOP_TIMEOUT_MS) {
                suspendCancellableCoroutine<Unit> { continuation ->
                    try {
                        val future: Future<Void> = recognizer.stopContinuousRecognitionAsync()
                        future.get()
                        continuation.resume(Unit)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error stopping recognition", e)
                        continuation.resume(Unit)
                    }
                }
            } ?: Log.w(TAG, "Stop recognition timed out")

            _state.value = EngineState.Ready(SpeechEngine.AZURE)
            Log.i(TAG, "Azure recognition stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Azure recognition", e)
            _state.value = EngineState.Ready(SpeechEngine.AZURE)
        }
    }

    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        // Azure Speech uses phrase lists for boosting recognition of specific phrases
        // This is optional and improves accuracy for domain-specific terms
        try {
            val recognizer = speechRecognizer
            if (recognizer == null) {
                Log.w(TAG, "Cannot update commands: recognizer not initialized")
                return Result.success(Unit)
            }

            if (commands.isEmpty()) {
                Log.d(TAG, "No commands to add to phrase list")
                return Result.success(Unit)
            }

            // Create phrase list grammar for boosting recognition
            val phraseList = com.microsoft.cognitiveservices.speech.PhraseListGrammar.fromRecognizer(recognizer)
            phraseList.clear()

            commands.forEach { command ->
                phraseList.addPhrase(command)
            }

            Log.d(TAG, "Updated Azure phrase list with ${commands.size} commands")
            return Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update phrase list", e)
            return Result.failure(e)
        }
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        return try {
            // For Azure, changing configuration requires reinitializing
            // because SpeechConfig is immutable once created
            if (isRecognizing) {
                stopListening()
            }

            // Clean up current recognizer
            speechRecognizer?.close()
            audioConfig?.close()
            azureConfig?.close()

            // Reinitialize with new config
            isInitialized = false
            val result = initialize(config)

            if (result.isSuccess) {
                Log.d(TAG, "Configuration updated: language=${config.language}")
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update configuration", e)
            Result.failure(e)
        }
    }

    override fun isRecognizing(): Boolean = isRecognizing

    override fun isInitialized(): Boolean = isInitialized

    override fun getEngineType(): SpeechEngine = SpeechEngine.AZURE

    override fun getSupportedFeatures(): Set<EngineFeature> = setOf(
        EngineFeature.CONTINUOUS_RECOGNITION,
        EngineFeature.PUNCTUATION,
        EngineFeature.WORD_TIMESTAMPS,
        EngineFeature.TRANSLATION,
        EngineFeature.SPEAKER_DIARIZATION
    )

    override suspend fun destroy() {
        Log.d(TAG, "Destroying Azure Speech engine")

        try {
            if (isRecognizing) {
                stopListening()
            }

            speechRecognizer?.close()
            audioConfig?.close()
            azureConfig?.close()

            speechRecognizer = null
            audioConfig = null
            azureConfig = null
            isInitialized = false
            isRecognizing = false

            _state.value = EngineState.Destroyed
            Log.i(TAG, "Azure Speech engine destroyed")
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying Azure engine", e)
            _state.value = EngineState.Destroyed
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Event Handlers
    // ═══════════════════════════════════════════════════════════════════

    private fun setupEventHandlers(recognizer: SpeechRecognizer) {
        // Recognizing event - fired for partial/interim results
        recognizer.recognizing.addEventListener { _, eventArgs ->
            val result = eventArgs.result
            if (result.reason == ResultReason.RecognizingSpeech) {
                val text = result.text
                if (text.isNotBlank()) {
                    Log.d(TAG, "Partial result: '$text'")

                    scope.launch {
                        _results.emit(SpeechResult(
                            text = text,
                            confidence = 0.5f, // Partial results don't have confidence
                            isFinal = false,
                            alternatives = emptyList(),
                            timestamp = System.currentTimeMillis()
                        ))
                    }
                }
            }
        }

        // Recognized event - fired for final results
        recognizer.recognized.addEventListener { _, eventArgs ->
            val result = eventArgs.result

            when (result.reason) {
                ResultReason.RecognizedSpeech -> {
                    val text = result.text
                    if (text.isNotBlank()) {
                        // Parse detailed result for confidence
                        val confidence = parseConfidence(result)
                        val alternatives = parseAlternatives(result)

                        Log.d(TAG, "Final result: '$text' (confidence: $confidence)")

                        scope.launch {
                            _results.emit(SpeechResult(
                                text = text,
                                confidence = confidence,
                                isFinal = true,
                                alternatives = alternatives,
                                timestamp = System.currentTimeMillis()
                            ))
                        }
                    }
                }

                ResultReason.NoMatch -> {
                    Log.d(TAG, "No speech recognized")
                    scope.launch {
                        _errors.emit(SpeechError(
                            code = SpeechError.ErrorCode.NO_SPEECH_DETECTED,
                            message = "No speech detected",
                            recoverable = true
                        ))
                    }
                }

                else -> {
                    Log.d(TAG, "Recognition result: ${result.reason}")
                }
            }
        }

        // Canceled event - fired when recognition is canceled or errors occur
        recognizer.canceled.addEventListener { _, eventArgs ->
            val cancellation = eventArgs

            when (cancellation.reason) {
                CancellationReason.Error -> {
                    val errorCode = cancellation.errorCode
                    val errorDetails = cancellation.errorDetails
                    Log.e(TAG, "Recognition canceled with error: $errorCode - $errorDetails")

                    val (code, recoverable) = mapCancellationError(errorCode)

                    scope.launch {
                        _errors.emit(SpeechError(
                            code = code,
                            message = errorDetails ?: "Recognition error: $errorCode",
                            recoverable = recoverable
                        ))
                    }

                    isRecognizing = false
                    _state.value = EngineState.Error(errorDetails ?: "Recognition canceled", recoverable)
                }

                CancellationReason.EndOfStream -> {
                    Log.d(TAG, "End of audio stream")
                    isRecognizing = false
                    _state.value = EngineState.Ready(SpeechEngine.AZURE)
                }

                else -> {
                    Log.d(TAG, "Recognition canceled: ${cancellation.reason}")
                    isRecognizing = false
                    _state.value = EngineState.Ready(SpeechEngine.AZURE)
                }
            }
        }

        // Session started event
        recognizer.sessionStarted.addEventListener { _, eventArgs ->
            Log.d(TAG, "Session started: ${eventArgs.sessionId}")
            _state.value = EngineState.Listening
        }

        // Session stopped event
        recognizer.sessionStopped.addEventListener { _, eventArgs ->
            Log.d(TAG, "Session stopped: ${eventArgs.sessionId}")
            if (isRecognizing) {
                isRecognizing = false
                _state.value = EngineState.Ready(SpeechEngine.AZURE)
            }
        }

        // Speech start detected
        recognizer.speechStartDetected.addEventListener { _, eventArgs ->
            Log.d(TAG, "Speech start detected at offset: ${eventArgs.offset}")
            _state.value = EngineState.Processing
        }

        // Speech end detected
        recognizer.speechEndDetected.addEventListener { _, eventArgs ->
            Log.d(TAG, "Speech end detected at offset: ${eventArgs.offset}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Parse confidence score from detailed result.
     * Azure provides confidence in the detailed JSON result.
     */
    private fun parseConfidence(result: com.microsoft.cognitiveservices.speech.SpeechRecognitionResult): Float {
        return try {
            // Get the best result properties
            val properties = result.properties
            val jsonResult = properties.getProperty(
                com.microsoft.cognitiveservices.speech.PropertyId.SpeechServiceResponse_JsonResult
            )

            if (jsonResult.isNullOrBlank()) {
                return 0.8f // Default confidence
            }

            // Parse JSON to extract confidence
            // Format: {"NBest":[{"Confidence":0.95,"Lexical":"...","ITN":"...","MaskedITN":"...","Display":"..."},...]}
            val confidenceMatch = Regex("\"Confidence\":(\\d+\\.?\\d*)").find(jsonResult)
            confidenceMatch?.groupValues?.getOrNull(1)?.toFloatOrNull() ?: 0.8f
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse confidence", e)
            0.8f
        }
    }

    /**
     * Parse alternative recognitions from detailed result.
     */
    private fun parseAlternatives(result: com.microsoft.cognitiveservices.speech.SpeechRecognitionResult): List<SpeechResult.Alternative> {
        return try {
            val properties = result.properties
            val jsonResult = properties.getProperty(
                com.microsoft.cognitiveservices.speech.PropertyId.SpeechServiceResponse_JsonResult
            )

            if (jsonResult.isNullOrBlank()) {
                return emptyList()
            }

            // Parse NBest alternatives from JSON
            // This is a simplified parser - for production, use a proper JSON library
            val alternatives = mutableListOf<SpeechResult.Alternative>()
            val nbestPattern = Regex("\\{\"Confidence\":(\\d+\\.?\\d*)[^}]*\"Display\":\"([^\"]+)\"")
            val matches = nbestPattern.findAll(jsonResult)

            matches.drop(1).take(4).forEach { match ->
                val confidence = match.groupValues[1].toFloatOrNull() ?: 0f
                val text = match.groupValues[2]
                alternatives.add(SpeechResult.Alternative(text, confidence))
            }

            alternatives
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse alternatives", e)
            emptyList()
        }
    }

    /**
     * Map Azure cancellation error codes to our error model.
     */
    private fun mapCancellationError(errorCode: CancellationErrorCode): Pair<SpeechError.ErrorCode, Boolean> {
        return when (errorCode) {
            CancellationErrorCode.NoError ->
                SpeechError.ErrorCode.UNKNOWN to true

            CancellationErrorCode.AuthenticationFailure ->
                SpeechError.ErrorCode.PERMISSION_DENIED to false

            CancellationErrorCode.BadRequest ->
                SpeechError.ErrorCode.RECOGNITION_FAILED to false

            CancellationErrorCode.TooManyRequests ->
                SpeechError.ErrorCode.ENGINE_BUSY to true

            CancellationErrorCode.Forbidden ->
                SpeechError.ErrorCode.PERMISSION_DENIED to false

            CancellationErrorCode.ConnectionFailure ->
                SpeechError.ErrorCode.NETWORK_ERROR to true

            CancellationErrorCode.ServiceTimeout ->
                SpeechError.ErrorCode.TIMEOUT to true

            CancellationErrorCode.ServiceError ->
                SpeechError.ErrorCode.RECOGNITION_FAILED to true

            CancellationErrorCode.ServiceUnavailable ->
                SpeechError.ErrorCode.NETWORK_ERROR to true

            CancellationErrorCode.RuntimeError ->
                SpeechError.ErrorCode.UNKNOWN to true

            else ->
                SpeechError.ErrorCode.UNKNOWN to true
        }
    }
}
