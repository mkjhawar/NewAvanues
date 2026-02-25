/**
 * GoogleCloudConfig.kt - Configuration for Google Cloud STT v2 engine
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-21
 *
 * Configuration data class for Google Cloud Speech-to-Text v2 API.
 * Supports both VAD-batch mode (using WhisperVAD for chunking) and
 * streaming mode (continuous HTTP/2 streaming recognition).
 */
package com.augmentalis.speechrecognition.googlecloud

import com.augmentalis.speechrecognition.SpeechConfig

/**
 * Recognition mode for Google Cloud STT v2.
 */
enum class GoogleCloudMode {
    /** VAD-based batch: WhisperVAD detects speech chunks, each sent as a synchronous recognize request */
    VAD_BATCH,
    /** Streaming: continuous audio sent via HTTP/2 chunked transfer, partial + final results returned */
    STREAMING
}

/**
 * Authentication mode for Google Cloud STT v2.
 */
enum class GoogleCloudAuthMode {
    /** Firebase Auth — uses Firebase user's ID token as Bearer auth */
    FIREBASE_AUTH,
    /** API Key — appended as ?key= query parameter */
    API_KEY
}

/**
 * Complete configuration for the Google Cloud STT v2 engine.
 * Follows the same pattern as WhisperConfig — engine-specific config
 * extracted from the unified SpeechConfig.
 */
data class GoogleCloudConfig(
    /** GCP project ID (required) */
    val projectId: String,

    /** Recognizer resource name. "_" = default recognizer */
    val recognizerName: String = "_",

    /** GCP location for the recognizer */
    val location: String = "global",

    /** Recognition mode */
    val mode: GoogleCloudMode = GoogleCloudMode.VAD_BATCH,

    /** BCP-47 language code */
    val language: String = "en-US",

    /** Recognition model: "latest_short" for commands, "latest_long" for dictation */
    val model: String = "latest_short",

    /** Enable automatic punctuation in results */
    val enableAutoPunctuation: Boolean = true,

    /** Enable word-level time offsets in results */
    val enableWordTimeOffsets: Boolean = true,

    /** Enable per-word confidence scores */
    val enableWordConfidence: Boolean = true,

    /** Maximum alternative transcriptions to return */
    val maxAlternatives: Int = 3,

    /** Authentication mode */
    val authMode: GoogleCloudAuthMode = GoogleCloudAuthMode.API_KEY,

    /** API key (used when authMode = API_KEY) */
    val apiKey: String? = null,

    // VAD parameters (used in VAD_BATCH mode, mirrors WhisperConfig for WhisperVAD reuse)
    /** Silence duration (ms) before a speech chunk is finalized */
    val silenceThresholdMs: Long = 700,

    /** Minimum speech duration (ms) to consider as valid utterance */
    val minSpeechDurationMs: Long = 300,

    /** Maximum audio chunk duration (ms) before forced transcription */
    val maxChunkDurationMs: Long = 30_000,

    // Network parameters
    /** HTTP request timeout in milliseconds */
    val requestTimeoutMs: Long = 30_000,

    /** HTTP connection timeout in milliseconds */
    val connectTimeoutMs: Long = 10_000,

    /** Maximum retry attempts for transient failures */
    val maxRetries: Int = 3,

    /** Enable profanity filter */
    val profanityFilter: Boolean = false,

    /** Enable single utterance mode for streaming. When true, the server will
     *  detect the end of speech and finalize the result automatically.
     *  Best for command mode where the user speaks one phrase at a time. */
    val singleUtterance: Boolean = false
) {
    companion object {
        /**
         * Create a GoogleCloudConfig from the unified SpeechConfig.
         * Extracts GCP-specific fields and applies sensible defaults.
         */
        fun fromSpeechConfig(config: SpeechConfig): GoogleCloudConfig {
            val isStreaming = config.gcpRecognizerMode == "streaming"
            return GoogleCloudConfig(
                projectId = config.gcpProjectId ?: "",
                mode = if (isStreaming) GoogleCloudMode.STREAMING else GoogleCloudMode.VAD_BATCH,
                language = config.language,
                apiKey = config.cloudApiKey,
                authMode = if (config.cloudApiKey.isNullOrBlank()) {
                    GoogleCloudAuthMode.FIREBASE_AUTH
                } else {
                    GoogleCloudAuthMode.API_KEY
                },
                silenceThresholdMs = config.timeoutDuration.coerceIn(300, 5000),
                minSpeechDurationMs = 300,
                profanityFilter = config.enableProfanityFilter,
                singleUtterance = false // Caller should set true for COMMAND speech modes
            )
        }
    }

    /**
     * Build the REST API base URL for this configuration.
     */
    fun buildRecognizeUrl(): String {
        return "https://speech.googleapis.com/v2/projects/$projectId/locations/$location/recognizers/$recognizerName:recognize"
    }

    /**
     * Build the streaming REST API URL.
     */
    fun buildStreamingUrl(): String {
        return "https://speech.googleapis.com/v2/projects/$projectId/locations/$location/recognizers/$recognizerName:streamingRecognize"
    }

    /**
     * Validate this configuration.
     */
    fun validate(): Result<Unit> {
        return when {
            projectId.isBlank() -> Result.failure(
                IllegalArgumentException("Google Cloud STT requires a project ID")
            )
            language.isBlank() -> Result.failure(
                IllegalArgumentException("Language code cannot be blank")
            )
            authMode == GoogleCloudAuthMode.API_KEY && apiKey.isNullOrBlank() -> Result.failure(
                IllegalArgumentException("API_KEY auth mode requires an API key")
            )
            requestTimeoutMs < 1000 -> Result.failure(
                IllegalArgumentException("Request timeout must be at least 1000ms")
            )
            connectTimeoutMs < 1000 -> Result.failure(
                IllegalArgumentException("Connect timeout must be at least 1000ms")
            )
            maxRetries < 0 || maxRetries > 10 -> Result.failure(
                IllegalArgumentException("Max retries must be 0-10")
            )
            silenceThresholdMs < 100 || silenceThresholdMs > 5000 -> Result.failure(
                IllegalArgumentException("Silence threshold must be 100-5000ms")
            )
            else -> Result.success(Unit)
        }
    }
}
