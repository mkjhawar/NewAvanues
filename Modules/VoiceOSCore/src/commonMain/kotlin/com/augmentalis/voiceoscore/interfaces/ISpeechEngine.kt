/**
 * ISpeechEngine.kt - Speech Engine Interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * KMP migration of VoiceOSCore ISpeechEngine interface.
 * Unified interface for all speech recognition engines across platforms.
 *
 * Implements Factory Pattern for extensibility:
 * - Open/Closed Principle: Add new engines without modifying existing code
 * - Single Responsibility: Each adapter handles one engine's integration
 * - Dependency Inversion: Managers depend on abstraction, not concrete engines
 */
package com.augmentalis.voiceoscore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Unified interface for all speech recognition engines.
 *
 * This abstraction allows SpeechEngineManager to work with any engine
 * implementation without knowing the underlying details.
 *
 * Lifecycle:
 * 1. Create via ISpeechEngineFactory
 * 2. Call initialize() with config
 * 3. Use startListening()/stopListening() for recognition
 * 4. Call updateCommands() when screen changes
 * 5. Call destroy() when done
 */
interface ISpeechEngine {

    /**
     * Current engine state as observable flow
     */
    val state: StateFlow<EngineState>

    /**
     * Recognition results as hot flow
     */
    val results: Flow<SpeechResult>

    /**
     * Error events as hot flow
     */
    val errors: Flow<SpeechError>

    /**
     * Initialize the speech engine with configuration.
     *
     * This is an asynchronous operation that may involve:
     * - Downloading language models
     * - Initializing SDK components
     * - Setting up audio pipelines
     * - Requesting permissions (platform-specific)
     *
     * @param config Speech configuration
     * @return Result indicating success or failure with error details
     */
    suspend fun initialize(config: SpeechConfig): Result<Unit>

    /**
     * Start listening for speech input.
     *
     * Begins audio capture and speech recognition. Results are
     * emitted via the [results] flow.
     *
     * PRECONDITION: Engine must be initialized
     *
     * @return Result indicating if listening started successfully
     */
    suspend fun startListening(): Result<Unit>

    /**
     * Stop listening for speech input.
     *
     * Stops audio capture and recognition processing.
     * Safe to call even if engine is not currently listening.
     */
    suspend fun stopListening()

    /**
     * Update dynamic command list at runtime.
     *
     * Replaces the current command vocabulary with a new set.
     * Used for context-sensitive voice commands based on current screen.
     *
     * NOTE: This operation may be expensive (100-500ms) for some engines
     * that require model recompilation. Call from background thread.
     *
     * @param commands List of voice commands to recognize
     * @return Result indicating success or failure
     */
    suspend fun updateCommands(commands: List<String>): Result<Unit>

    /**
     * Update engine configuration at runtime.
     *
     * Allows changing settings like language, mode, or timeouts without
     * full reinitialization. Not all engines support all configuration
     * changes at runtime.
     *
     * @param config New configuration data
     * @return Result indicating if update was applied
     */
    suspend fun updateConfiguration(config: SpeechConfig): Result<Unit>

    /**
     * Check if engine is currently recognizing speech.
     */
    fun isRecognizing(): Boolean

    /**
     * Check if engine is initialized and ready.
     */
    fun isInitialized(): Boolean

    /**
     * Get the engine type identifier.
     */
    fun getEngineType(): SpeechEngine

    /**
     * Get supported features for this engine.
     */
    fun getSupportedFeatures(): Set<EngineFeature>

    /**
     * Clean up engine resources.
     *
     * Releases all resources held by the engine (audio, models, SDK).
     * Engine cannot be used after destroy() is called.
     */
    suspend fun destroy()
}

/**
 * Engine state representation
 */
sealed class EngineState {
    data object Uninitialized : EngineState()
    data object Initializing : EngineState()
    data class Ready(val engineType: SpeechEngine) : EngineState()
    data object Listening : EngineState()
    data object Processing : EngineState()
    data class Error(val message: String, val recoverable: Boolean) : EngineState()
    data object Destroyed : EngineState()

    val isReady: Boolean get() = this is Ready
    val isListening: Boolean get() = this is Listening
    val isProcessing: Boolean get() = this is Processing
}

/**
 * Speech recognition result
 */
data class SpeechResult(
    /**
     * Recognized text
     */
    val text: String,

    /**
     * Confidence score (0.0 - 1.0)
     */
    val confidence: Float,

    /**
     * Whether this is a final result or interim
     */
    val isFinal: Boolean,

    /**
     * Timestamp of recognition
     */
    val timestamp: Long = currentTimeMillis(),

    /**
     * Alternative interpretations (if supported by engine)
     */
    val alternatives: List<Alternative> = emptyList()
) {
    data class Alternative(
        val text: String,
        val confidence: Float
    )
}

/**
 * Speech recognition error
 */
data class SpeechError(
    val code: ErrorCode,
    val message: String,
    val recoverable: Boolean,
    val timestamp: Long = currentTimeMillis()
) {
    enum class ErrorCode {
        NOT_INITIALIZED,
        AUDIO_ERROR,
        NETWORK_ERROR,
        PERMISSION_DENIED,
        NO_SPEECH_DETECTED,
        RECOGNITION_FAILED,
        MODEL_NOT_FOUND,
        ENGINE_BUSY,
        TIMEOUT,
        UNKNOWN
    }
}

/**
 * Engine feature flags
 */
enum class EngineFeature {
    OFFLINE_MODE,
    CONTINUOUS_RECOGNITION,
    WORD_TIMESTAMPS,
    SPEAKER_DIARIZATION,
    LANGUAGE_DETECTION,
    TRANSLATION,
    CUSTOM_VOCABULARY,
    WAKE_WORD,
    PUNCTUATION,
    PROFANITY_FILTER
}

/**
 * Platform-specific time functions
 */
expect fun currentTimeMillis(): Long
expect fun getCurrentTimeMillis(): Long
