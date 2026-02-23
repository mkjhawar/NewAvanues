/**
 * GoogleCloudEngine.kt - Google Cloud STT v2 speech recognition engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Main engine orchestrator for Google Cloud Speech-to-Text v2.
 * Mirrors the WhisperEngine pattern exactly:
 *
 * Lifecycle: UNINITIALIZED -> LOADING_MODEL -> READY -> LISTENING -> PROCESSING -> READY
 *
 * Two recognition modes:
 * - VAD_BATCH: WhisperVAD detects speech chunks -> GoogleCloudApiClient.recognize(chunk)
 * - STREAMING: Continuous audio -> GoogleCloudStreamingClient -> partial/final results
 *
 * Reuses: WhisperAudio (mic capture), WhisperVAD (batch chunking), CommandCache
 */
package com.augmentalis.speechrecognition.googlecloud

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.CommandCache
import com.augmentalis.speechrecognition.RecognitionResult
import com.augmentalis.speechrecognition.SpeechError
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.speechrecognition.VoiceStateManager
import com.augmentalis.speechrecognition.whisper.OnSpeechChunkReady
import com.augmentalis.speechrecognition.whisper.WhisperAudio
import com.augmentalis.speechrecognition.whisper.WhisperEngineState
import com.augmentalis.speechrecognition.whisper.WhisperVAD
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

/**
 * Google Cloud STT v2 speech recognition engine.
 *
 * Provides high-accuracy cloud recognition as a premium option,
 * with both VAD-based batch and continuous streaming modes.
 * Uses the same audio capture pipeline as WhisperEngine (16kHz mono PCM).
 */
class GoogleCloudEngine(
    private val context: Context
) {
    companion object {
        private const val TAG = "GoogleCloudEngine"
        private const val ENGINE_NAME = "GoogleCloud"
        private const val ENGINE_VERSION = "1.0.0"

        /** Polling interval for checking audio buffer during VAD listen loop */
        private const val LISTEN_POLL_MS = 100L

        /** Polling interval for streaming mode audio chunks */
        private const val STREAM_CHUNK_MS = 100L
    }

    // State — reuses WhisperEngineState enum (same lifecycle)
    private val engineState = AtomicReference(WhisperEngineState.UNINITIALIZED)
    val state: WhisperEngineState get() = engineState.get()

    // Configuration
    private var config = GoogleCloudConfig(projectId = "")
    private var speechMode: SpeechMode = SpeechMode.DYNAMIC_COMMAND

    // Components — reuses WhisperAudio and WhisperVAD
    private val audio = WhisperAudio()
    private var vad: WhisperVAD? = null
    private val commandCache = CommandCache()
    private var voiceStateManager: VoiceStateManager? = null

    // API clients
    private var apiClient: GoogleCloudApiClient? = null
    private var streamingClient: GoogleCloudStreamingClient? = null

    // Coroutine management
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var listenJob: Job? = null

    // Result flows
    private val _resultFlow = MutableSharedFlow<RecognitionResult>(replay = 1)
    val resultFlow: SharedFlow<RecognitionResult> = _resultFlow.asSharedFlow()

    private val _errorFlow = MutableSharedFlow<SpeechError>(replay = 1)
    val errorFlow: SharedFlow<SpeechError> = _errorFlow.asSharedFlow()

    /**
     * Initialize the engine with Google Cloud-specific configuration.
     *
     * @param gcConfig Google Cloud configuration
     * @return true if initialization succeeded
     */
    suspend fun initialize(gcConfig: GoogleCloudConfig): Boolean {
        if (engineState.get() == WhisperEngineState.READY) {
            Log.w(TAG, "Already initialized")
            return true
        }

        gcConfig.validate().getOrElse { e ->
            Log.e(TAG, "Invalid config: ${e.message}")
            scope.launch {
                _errorFlow.emit(SpeechError(
                    code = SpeechError.ERROR_CLIENT,
                    message = "Invalid Google Cloud config: ${e.message}",
                    isRecoverable = false,
                    suggestedAction = SpeechError.Action.CHECK_CONFIGURATION
                ))
            }
            return false
        }

        config = gcConfig
        engineState.set(WhisperEngineState.LOADING_MODEL) // "Loading" = setting up clients

        return try {
            // Step 1: Initialize audio capture (same as WhisperEngine)
            if (!audio.initialize()) {
                throw IllegalStateException("Failed to initialize audio capture")
            }

            audio.onError = { error ->
                scope.launch {
                    _errorFlow.emit(SpeechError.audioError(error))
                }
            }

            // Step 2: Initialize VAD (for VAD_BATCH mode)
            vad = WhisperVAD(
                speechThreshold = 0f, // auto-calibrate
                silenceTimeoutMs = config.silenceThresholdMs,
                minSpeechDurationMs = config.minSpeechDurationMs,
                maxSpeechDurationMs = config.maxChunkDurationMs,
                paddingMs = 150
            )

            // Step 3: Create API clients based on mode
            apiClient = GoogleCloudApiClient(config)

            if (config.mode == GoogleCloudMode.STREAMING) {
                streamingClient = GoogleCloudStreamingClient(config, scope)

                // Collect streaming results and bridge to engine result flow
                scope.launch {
                    streamingClient?.resultFlow?.collect { result ->
                        _resultFlow.emit(result)
                        if (result.isFinal) {
                            voiceStateManager?.updateCommandExecutionTime()
                        }
                    }
                }

                // Collect streaming errors
                scope.launch {
                    streamingClient?.errorFlow?.collect { error ->
                        _errorFlow.emit(error)
                    }
                }
            }

            engineState.set(WhisperEngineState.READY)
            Log.i(TAG, "Engine initialized: mode=${config.mode}, lang=${config.language}, " +
                    "auth=${config.authMode}, model=${config.model}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
            engineState.set(WhisperEngineState.ERROR)
            scope.launch {
                _errorFlow.emit(SpeechError(
                    code = SpeechError.ERROR_UNKNOWN,
                    message = "Google Cloud init failed: ${e.message}",
                    isRecoverable = true,
                    suggestedAction = SpeechError.Action.REINITIALIZE
                ))
            }
            false
        }
    }

    /**
     * Start listening for speech.
     * Starts audio capture and begins the appropriate listen loop based on mode.
     */
    fun startListening(mode: SpeechMode = speechMode): Boolean {
        val currentState = engineState.get()
        if (currentState != WhisperEngineState.READY && currentState != WhisperEngineState.PAUSED) {
            Log.w(TAG, "Cannot start listening from state: $currentState")
            return false
        }

        speechMode = mode

        if (!audio.isRecording()) {
            if (!audio.start()) {
                Log.e(TAG, "Failed to start audio capture")
                return false
            }
        }

        engineState.set(WhisperEngineState.LISTENING)

        listenJob?.cancel()
        listenJob = scope.launch {
            when (config.mode) {
                GoogleCloudMode.VAD_BATCH -> vadListenLoop()
                GoogleCloudMode.STREAMING -> streamingListenLoop()
            }
        }

        Log.i(TAG, "Listening started, speechMode=$mode, cloudMode=${config.mode}")
        return true
    }

    /**
     * Stop listening. Audio capture stops and any pending recognition completes.
     */
    fun stopListening() {
        listenJob?.cancel()
        listenJob = null

        // In VAD_BATCH mode, flush remaining audio through the API
        if (config.mode == GoogleCloudMode.VAD_BATCH) {
            val remainingAudio = audio.drainBuffer()
            if (remainingAudio.size > config.minSpeechDurationMs * WhisperAudio.SAMPLE_RATE / 1000) {
                scope.launch {
                    recognizeChunk(remainingAudio)
                }
            }
        }

        // In STREAMING mode, stop the streaming client
        if (config.mode == GoogleCloudMode.STREAMING) {
            streamingClient?.stopStreaming()
        }

        audio.stop()

        if (engineState.get() == WhisperEngineState.LISTENING ||
            engineState.get() == WhisperEngineState.PROCESSING) {
            engineState.set(WhisperEngineState.READY)
        }

        Log.i(TAG, "Listening stopped")
    }

    /**
     * Pause listening — keeps clients alive but stops audio capture.
     */
    fun pause() {
        if (engineState.get() == WhisperEngineState.LISTENING) {
            listenJob?.cancel()
            listenJob = null
            streamingClient?.stopStreaming()
            audio.stop()
            engineState.set(WhisperEngineState.PAUSED)
            Log.i(TAG, "Paused")
        }
    }

    /**
     * Resume from paused state.
     */
    fun resume(): Boolean {
        if (engineState.get() != WhisperEngineState.PAUSED) return false
        return startListening(speechMode)
    }

    /**
     * Set commands for command-mode recognition.
     */
    fun setCommands(staticCommands: List<String>, dynamicCommands: List<String> = emptyList()) {
        commandCache.setStaticCommands(staticCommands)
        commandCache.setDynamicCommands(dynamicCommands)
        Log.d(TAG, "Commands updated: ${staticCommands.size} static, ${dynamicCommands.size} dynamic")
    }

    /**
     * Set the speech recognition mode.
     */
    fun setMode(mode: SpeechMode) {
        speechMode = mode
        Log.d(TAG, "Mode set to: $mode")
    }

    /**
     * Attach a VoiceStateManager for state integration.
     */
    fun setVoiceStateManager(manager: VoiceStateManager) {
        voiceStateManager = manager
    }

    /**
     * Check if currently listening.
     */
    fun isListening(): Boolean = engineState.get() == WhisperEngineState.LISTENING

    /**
     * Check if engine is ready.
     */
    fun isReady(): Boolean = engineState.get() == WhisperEngineState.READY

    fun getEngineName(): String = ENGINE_NAME
    fun getEngineVersion(): String = ENGINE_VERSION
    fun requiresNetwork(): Boolean = true

    /**
     * Destroy the engine and release all resources.
     */
    fun destroy() {
        Log.d(TAG, "Destroying GoogleCloudEngine")

        listenJob?.cancel()
        listenJob = null

        audio.release()

        streamingClient?.destroy()
        streamingClient = null

        apiClient?.destroy()
        apiClient = null

        commandCache.clear()
        engineState.set(WhisperEngineState.DESTROYED)
        scope.cancel()

        Log.d(TAG, "GoogleCloudEngine destroyed")
    }

    // --- Private listen loops ---

    /**
     * VAD-based batch listen loop.
     * Mirrors WhisperEngine.listenLoop() — feeds audio through WhisperVAD,
     * which detects speech chunks. Each chunk is sent to the REST API.
     */
    private suspend fun vadListenLoop() {
        Log.d(TAG, "VAD batch listen loop started")

        val activeVad = vad ?: run {
            Log.e(TAG, "VAD not initialized")
            return
        }

        // Wire VAD chunk callback to cloud recognition
        activeVad.onSpeechChunkReady = OnSpeechChunkReady { audioData, durationMs ->
            scope.launch {
                engineState.set(WhisperEngineState.PROCESSING)
                recognizeChunk(audioData)
                if (engineState.get() == WhisperEngineState.PROCESSING) {
                    engineState.set(WhisperEngineState.LISTENING)
                }
            }
        }

        activeVad.reset()
        var lastPartialEmitMs = 0L

        while (scope.isActive && engineState.get() == WhisperEngineState.LISTENING) {
            delay(LISTEN_POLL_MS)

            val samples = audio.drainBuffer()
            if (samples.isNotEmpty()) {
                val nowMs = System.currentTimeMillis()
                activeVad.processAudio(samples, nowMs)

                // Emit partial "listening" indicator when VAD detects speech
                if (activeVad.getState() == com.augmentalis.speechrecognition.whisper.VADState.SPEECH &&
                    nowMs - lastPartialEmitMs > 500
                ) {
                    lastPartialEmitMs = nowMs
                    _resultFlow.emit(RecognitionResult(
                        text = "",
                        confidence = 0f,
                        isPartial = true,
                        isFinal = false,
                        engine = ENGINE_NAME,
                        mode = speechMode.name
                    ))
                }
            }
        }

        activeVad.flush()
        Log.d(TAG, "VAD batch listen loop ended")
    }

    /**
     * Streaming listen loop.
     * Starts the streaming client, then continuously drains the audio buffer
     * and sends chunks to the streaming session.
     */
    private suspend fun streamingListenLoop() {
        Log.d(TAG, "Streaming listen loop started")

        val client = streamingClient ?: run {
            Log.e(TAG, "Streaming client not initialized")
            return
        }

        client.startStreaming(speechMode.name, phraseHints = commandCache.getAllCommands())

        while (scope.isActive && engineState.get() == WhisperEngineState.LISTENING) {
            delay(STREAM_CHUNK_MS)

            val samples = audio.drainBuffer()
            if (samples.isNotEmpty()) {
                client.sendAudioChunk(samples)
            }
        }

        client.stopStreaming()
        Log.d(TAG, "Streaming listen loop ended")
    }

    /**
     * Send a speech chunk to the batch API and emit the result.
     */
    private suspend fun recognizeChunk(audioData: FloatArray) {
        val client = apiClient ?: run {
            Log.w(TAG, "API client not initialized")
            return
        }

        try {
            val audioDurationMs = (audioData.size * 1000L) / WhisperAudio.SAMPLE_RATE
            Log.d(TAG, "Recognizing chunk: ${audioData.size} samples, ~${audioDurationMs}ms")

            val response = client.recognize(audioData, speechMode.name, phraseHints = commandCache.getAllCommands())

            when (response) {
                is RecognizeResponse.Success -> {
                    val result = response.result
                    if (!result.isEmpty()) {
                        Log.i(TAG, "Recognized: '${result.text}' (conf=${result.confidence})")
                        _resultFlow.emit(result)
                        voiceStateManager?.updateCommandExecutionTime()
                    } else {
                        Log.d(TAG, "Empty recognition result for ${audioDurationMs}ms chunk")
                    }
                }
                is RecognizeResponse.Error -> {
                    Log.w(TAG, "Recognition error: ${response.error.message}")
                    _errorFlow.emit(response.error)
                }
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Recognition error", e)
            _errorFlow.emit(SpeechError(
                code = SpeechError.ERROR_UNKNOWN,
                message = "Recognition failed: ${e.message}",
                isRecoverable = true,
                suggestedAction = SpeechError.Action.RETRY
            ))
        }
    }
}
