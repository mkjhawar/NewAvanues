/*
 * VoiceRecognitionClient.kt - Client for calling VoiceRecognition service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * This client provides suspend functions for AVA to interact with voice
 * recognition services, supporting multiple recognizer backends and
 * streaming recognition results.
 */
package com.augmentalis.universalrpc.android.ava

import com.augmentalis.universalrpc.recognition.GetStateRequest
import com.augmentalis.universalrpc.recognition.RecognitionConfig
import com.augmentalis.universalrpc.recognition.RecognitionData
import com.augmentalis.universalrpc.recognition.RecognitionEvent
import com.augmentalis.universalrpc.recognition.RecognitionResponse
import com.augmentalis.universalrpc.recognition.RecognitionStatus
import com.augmentalis.universalrpc.recognition.StartRecognitionRequest
import com.augmentalis.universalrpc.recognition.StopRecognitionRequest
import com.augmentalis.universalrpc.recognition.VoiceRecognitionServiceGrpcKt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.UUID

/**
 * Voice recognizer type.
 */
enum class RecognizerType(val value: String) {
    /** Google Speech Recognition (default) */
    GOOGLE("google"),
    /** Vivoka (offline, embedded) */
    VIVOKA("vivoka"),
    /** OpenAI Whisper */
    WHISPER("whisper")
}

/**
 * Recognition state.
 */
enum class RecognitionState(val value: String) {
    LISTENING("listening"),
    PROCESSING("processing"),
    STOPPED("stopped")
}

/**
 * Builder for recognition configuration.
 */
class RecognitionConfigBuilder {
    var language: String = "en-US"
    var recognizerType: RecognizerType = RecognizerType.GOOGLE
    var continuous: Boolean = false
    var silenceThreshold: Float = 0.5f
    var maxDurationMs: Int = 60000

    fun build(): RecognitionConfig = RecognitionConfig(
        language = language,
        recognizer_type = recognizerType.value,
        continuous = continuous,
        silence_threshold = silenceThreshold,
        max_duration_ms = maxDurationMs
    )
}

/**
 * Client for interacting with VoiceRecognition service.
 *
 * Provides high-level suspend functions for voice recognition including:
 * - Start/stop recognition
 * - Recognition state queries
 * - Streaming recognition results
 * - Multiple recognizer backend support
 *
 * @param grpcClient The base gRPC client for connection management
 * @param dispatcher The coroutine dispatcher for async operations
 */
class VoiceRecognitionClient(
    private val grpcClient: AvaGrpcClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : Closeable {

    private var stub: VoiceRecognitionServiceGrpcKt.VoiceRecognitionServiceCoroutineStub? = null

    /**
     * Get or create the gRPC stub for VoiceRecognition service.
     */
    private suspend fun getStub(): VoiceRecognitionServiceGrpcKt.VoiceRecognitionServiceCoroutineStub {
        stub?.let { return it }

        val channel = grpcClient.getChannel()
        return VoiceRecognitionServiceGrpcKt.VoiceRecognitionServiceCoroutineStub(channel).also {
            stub = it
        }
    }

    // =========================================================================
    // Recognition Control
    // =========================================================================

    /**
     * Start voice recognition with default configuration.
     *
     * @param language Target language code (e.g., "en-US")
     * @param recognizerType The recognizer backend to use
     * @param continuous Whether to continuously listen
     * @return RecognitionResponse with success status
     */
    suspend fun start(
        language: String = "en-US",
        recognizerType: RecognizerType = RecognizerType.GOOGLE,
        continuous: Boolean = false
    ): RecognitionResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val config = RecognitionConfig(
                language = language,
                recognizer_type = recognizerType.value,
                continuous = continuous,
                silence_threshold = 0.5f,
                max_duration_ms = 60000
            )
            val request = StartRecognitionRequest(
                request_id = generateRequestId(),
                config = config
            )
            getStub().Start(request)
        }
    }

    /**
     * Start voice recognition with custom configuration.
     *
     * @param configure Configuration builder function
     * @return RecognitionResponse with success status
     */
    suspend fun start(configure: RecognitionConfigBuilder.() -> Unit): RecognitionResponse =
        withContext(dispatcher) {
            val config = RecognitionConfigBuilder().apply(configure).build()
            grpcClient.withRetry { _ ->
                val request = StartRecognitionRequest(
                    request_id = generateRequestId(),
                    config = config
                )
                getStub().Start(request)
            }
        }

    /**
     * Stop voice recognition.
     *
     * @return RecognitionResponse with success status
     */
    suspend fun stop(): RecognitionResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = StopRecognitionRequest(request_id = generateRequestId())
            getStub().Stop(request)
        }
    }

    // =========================================================================
    // State Queries
    // =========================================================================

    /**
     * Get current recognition state.
     *
     * @return RecognitionStatus with state and volume level
     */
    suspend fun getState(): RecognitionStatus = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = GetStateRequest(request_id = generateRequestId())
            getStub().GetState(request)
        }
    }

    /**
     * Check if recognition is currently active.
     *
     * @return true if listening or processing
     */
    suspend fun isActive(): Boolean = withContext(dispatcher) {
        try {
            val state = getState()
            state.state == RecognitionState.LISTENING.value ||
                    state.state == RecognitionState.PROCESSING.value
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if recognition is listening.
     *
     * @return true if listening
     */
    suspend fun isListening(): Boolean = withContext(dispatcher) {
        try {
            getState().state == RecognitionState.LISTENING.value
        } catch (e: Exception) {
            false
        }
    }

    // =========================================================================
    // Streaming Results
    // =========================================================================

    /**
     * Stream recognition results.
     *
     * This starts recognition and returns a Flow of events including:
     * - RecognitionData with text and confidence
     * - RecognitionError for error events
     * - RecognitionStatus for state changes
     *
     * @param language Target language code
     * @param recognizerType The recognizer backend to use
     * @param continuous Whether to continuously listen
     * @return Flow of RecognitionEvent
     */
    suspend fun streamResults(
        language: String = "en-US",
        recognizerType: RecognizerType = RecognizerType.GOOGLE,
        continuous: Boolean = true
    ): Flow<RecognitionEvent> {
        return grpcClient.withStreaming { _ ->
            val config = RecognitionConfig(
                language = language,
                recognizer_type = recognizerType.value,
                continuous = continuous,
                silence_threshold = 0.5f,
                max_duration_ms = 300000 // 5 minutes for streaming
            )
            val request = StartRecognitionRequest(
                request_id = generateRequestId(),
                config = config
            )
            getStub().StreamResults(request)
        }
    }

    /**
     * Stream recognition results with custom configuration.
     *
     * @param configure Configuration builder function
     * @return Flow of RecognitionEvent
     */
    suspend fun streamResults(
        configure: RecognitionConfigBuilder.() -> Unit
    ): Flow<RecognitionEvent> {
        val config = RecognitionConfigBuilder().apply(configure).build()
        return grpcClient.withStreaming { _ ->
            val request = StartRecognitionRequest(
                request_id = generateRequestId(),
                config = config
            )
            getStub().StreamResults(request)
        }
    }

    /**
     * Stream only final recognition results (text).
     *
     * Filters out partial results and errors, returning only final transcriptions.
     *
     * @param language Target language code
     * @param recognizerType The recognizer backend to use
     * @return Flow of String (recognized text)
     */
    suspend fun streamFinalText(
        language: String = "en-US",
        recognizerType: RecognizerType = RecognizerType.GOOGLE
    ): Flow<String> {
        return streamResults(language, recognizerType, continuous = true)
            .mapNotNull { event ->
                when {
                    event.result != null && event.result.is_final -> event.result.text
                    else -> null
                }
            }
    }

    /**
     * Stream all recognition text (partial and final).
     *
     * @param language Target language code
     * @param recognizerType The recognizer backend to use
     * @return Flow of RecognitionData
     */
    suspend fun streamAllText(
        language: String = "en-US",
        recognizerType: RecognizerType = RecognizerType.GOOGLE
    ): Flow<RecognitionData> {
        return streamResults(language, recognizerType, continuous = true)
            .mapNotNull { event -> event.result }
    }

    /**
     * Stream recognition status updates.
     *
     * @return Flow of RecognitionStatus
     */
    suspend fun streamStatus(): Flow<RecognitionStatus> {
        return streamResults()
            .mapNotNull { event -> event.status }
    }

    // =========================================================================
    // Convenience Methods
    // =========================================================================

    /**
     * Listen for a single utterance and return the result.
     *
     * Starts recognition, waits for a final result, then stops.
     *
     * @param language Target language code
     * @param recognizerType The recognizer backend to use
     * @param maxWaitMs Maximum time to wait for result
     * @return The recognized text, or null if timeout/error
     */
    suspend fun listenOnce(
        language: String = "en-US",
        recognizerType: RecognizerType = RecognizerType.GOOGLE,
        maxWaitMs: Long = 10000
    ): String? = withContext(dispatcher) {
        try {
            val response = start(language, recognizerType, continuous = false)
            if (!response.success) return@withContext null

            kotlinx.coroutines.withTimeout(maxWaitMs) {
                streamResults(language, recognizerType, continuous = false)
                    .mapNotNull { event ->
                        event.result?.takeIf { it.is_final }?.text
                    }
                    .filter { it.isNotBlank() }
                    .let { flow ->
                        var result: String? = null
                        flow.collect { text ->
                            result = text
                            return@collect
                        }
                        result
                    }
            }
        } catch (e: Exception) {
            null
        } finally {
            try {
                stop()
            } catch (e: Exception) {
                // Ignore stop errors
            }
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private fun generateRequestId(): String = UUID.randomUUID().toString()

    override fun close() {
        grpcClient.close()
    }

    companion object {
        /**
         * Create a client for local UDS connection.
         */
        fun forLocalConnection(
            socketPath: String = "/data/local/tmp/voiceos.sock"
        ): VoiceRecognitionClient {
            return VoiceRecognitionClient(AvaGrpcClient.forLocalConnection(socketPath))
        }

        /**
         * Create a client for remote TCP connection.
         */
        fun forRemoteConnection(
            host: String,
            port: Int = 50051,
            useTls: Boolean = false
        ): VoiceRecognitionClient {
            return VoiceRecognitionClient(AvaGrpcClient.forRemoteConnection(host, port, useTls))
        }
    }
}
