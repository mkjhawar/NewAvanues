/**
 * SpeechEnginePlugin.kt - Speech recognition engine plugin interface
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * Defines the contract for speech recognition plugins in the Universal Plugin system.
 * Implementations can provide offline, cloud-based, or hybrid recognition capabilities.
 */
package com.augmentalis.magiccode.plugins.universal.contracts.speech

import com.augmentalis.magiccode.plugins.universal.UniversalPlugin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Speech recognition engine plugin interface.
 *
 * Extends [UniversalPlugin] to provide speech-to-text functionality.
 * Implementations can use various recognition backends including:
 * - On-device models (Vosk, Whisper, etc.)
 * - Cloud services (Google Speech, Azure, AWS Transcribe)
 * - Hybrid approaches
 *
 * ## Design Goals
 * - **Accessibility-First**: Designed for hands-free voice control
 * - **Streaming**: Support for real-time partial results
 * - **Customizable**: Custom vocabulary and language support
 * - **Observable**: Reactive state via StateFlow
 *
 * ## Implementation Example
 * ```kotlin
 * class VoskSpeechEngine : SpeechEnginePlugin {
 *     override val pluginId = "com.augmentalis.speech.vosk"
 *     override val pluginName = "Vosk Speech Engine"
 *     override val version = "1.0.0"
 *
 *     override val engineCapabilities = setOf(
 *         SpeechCapability.OFFLINE,
 *         SpeechCapability.CONTINUOUS,
 *         SpeechCapability.STREAMING
 *     )
 *
 *     override val supportedLanguages = setOf("en-US", "de-DE", "es-ES")
 *
 *     private val _recognitionState = MutableStateFlow(RecognitionState.IDLE)
 *     override val recognitionState: StateFlow<RecognitionState> = _recognitionState
 *
 *     override fun startRecognition(config: RecognitionConfig): Flow<RecognitionResult> {
 *         return flow {
 *             _recognitionState.value = RecognitionState.LISTENING
 *             // ... recognition logic
 *         }
 *     }
 *
 *     // ... implement other methods
 * }
 * ```
 *
 * ## Capability Registration
 * Speech engine plugins should register the capability:
 * `com.augmentalis.capability.speech.recognition`
 *
 * @since 1.0.0
 * @see UniversalPlugin
 * @see SpeechCapability
 * @see RecognitionResult
 */
interface SpeechEnginePlugin : UniversalPlugin {

    // =========================================================================
    // Engine Properties
    // =========================================================================

    /**
     * Set of capabilities this speech engine supports.
     *
     * Used for feature discovery and selecting appropriate engines
     * based on application requirements.
     *
     * ## Common Capability Combinations
     * - **Offline voice control**: OFFLINE, CONTINUOUS, STREAMING
     * - **Cloud dictation**: STREAMING, PUNCTUATION, CUSTOM_VOCABULARY
     * - **Wake word activation**: WAKE_WORD, OFFLINE, CONTINUOUS
     *
     * @see SpeechCapability
     */
    val engineCapabilities: Set<SpeechCapability>

    /**
     * Set of languages supported by this engine.
     *
     * Languages are specified as BCP-47 language tags (e.g., "en-US", "de-DE").
     * Implementations should include all variations they support.
     *
     * ## Examples
     * ```kotlin
     * setOf("en-US", "en-GB", "en-AU") // English variants
     * setOf("de-DE", "de-AT", "de-CH") // German variants
     * ```
     */
    val supportedLanguages: Set<String>

    /**
     * Current recognition state as an observable StateFlow.
     *
     * Consumers can collect this flow to react to state changes.
     * State transitions follow the [RecognitionState] documentation.
     *
     * @see RecognitionState
     */
    val recognitionState: StateFlow<RecognitionState>

    // =========================================================================
    // Recognition Operations
    // =========================================================================

    /**
     * Start speech recognition and return results as a Flow.
     *
     * Begins listening for audio input and emits recognition results.
     * For continuous recognition, the flow continues until [stopRecognition]
     * is called. For single-utterance mode, the flow completes after
     * one final result.
     *
     * ## Partial Results
     * If [RecognitionConfig.partialResults] is true, interim results are
     * emitted with [RecognitionResult.isFinal] = false. These may change
     * as more audio is processed.
     *
     * ## Error Handling
     * Errors are signaled by:
     * 1. Setting [recognitionState] to [RecognitionState.ERROR]
     * 2. Throwing an exception in the flow (use `catch` operator)
     *
     * ## Thread Safety
     * This method is thread-safe and can be called from any coroutine context.
     * Results are emitted on the engine's internal dispatcher.
     *
     * @param config Recognition configuration specifying language, mode, etc.
     * @return Flow of recognition results (partial and final)
     * @see RecognitionConfig
     * @see RecognitionResult
     * @see stopRecognition
     */
    fun startRecognition(config: RecognitionConfig): Flow<RecognitionResult>

    /**
     * Stop active speech recognition.
     *
     * Stops listening for audio and completes any active recognition flow.
     * If recognition is not active, this method does nothing.
     *
     * After stopping:
     * - [recognitionState] transitions to [RecognitionState.IDLE]
     * - Any active recognition flow completes normally
     * - Audio resources are released
     *
     * ## Usage
     * ```kotlin
     * // Stop recognition after receiving a command
     * val job = launch {
     *     engine.startRecognition(config).collect { result ->
     *         if (result.isFinal && result.text.contains("stop listening")) {
     *             engine.stopRecognition()
     *         }
     *     }
     * }
     * ```
     *
     * @see startRecognition
     */
    fun stopRecognition()

    /**
     * Load a custom vocabulary for improved recognition.
     *
     * Adds domain-specific terms or phrases to the recognition model.
     * This can significantly improve accuracy for specialized vocabularies
     * (e.g., app-specific commands, technical terms, proper nouns).
     *
     * ## Persistence
     * Vocabularies may be persisted between sessions depending on the
     * engine implementation. Check engine documentation for details.
     *
     * ## Multiple Vocabularies
     * Multiple vocabularies can be loaded. How they interact depends
     * on the engine implementation (merged, prioritized, etc.).
     *
     * @param vocabulary The vocabulary to load
     * @return Result indicating success or failure
     * @throws UnsupportedOperationException if engine doesn't support custom vocabularies
     * @see CustomVocabulary
     */
    suspend fun loadVocabulary(vocabulary: CustomVocabulary): Result<Unit>

    /**
     * Calibrate the speech engine for current environment.
     *
     * Analyzes audio samples to optimize recognition for:
     * - Ambient noise levels
     * - Microphone characteristics
     * - Speaker voice profile
     *
     * ## Calibration Process
     * 1. Provide 3-5 audio samples of typical environment
     * 2. Engine analyzes and adjusts internal parameters
     * 3. Results indicate what adjustments were made
     *
     * ## Usage
     * ```kotlin
     * // Collect noise samples
     * val samples = listOf(
     *     captureAudioSample(duration = 2000), // 2 seconds
     *     captureAudioSample(duration = 2000),
     *     captureAudioSample(duration = 2000)
     * )
     *
     * val result = engine.calibrate(samples)
     * if (result.success) {
     *     println("Calibrated: ${result.adjustments}")
     * }
     * ```
     *
     * @param samples List of audio samples for analysis
     * @return CalibrationResult with success status and adjustments
     * @see AudioSample
     * @see CalibrationResult
     */
    suspend fun calibrate(samples: List<AudioSample>): CalibrationResult
}

// =============================================================================
// Extension Functions
// =============================================================================

/**
 * Check if this engine supports a specific capability.
 *
 * @param capability The capability to check
 * @return true if the engine supports this capability
 */
fun SpeechEnginePlugin.hasCapability(capability: SpeechCapability): Boolean {
    return capability in engineCapabilities
}

/**
 * Check if this engine supports offline recognition.
 *
 * @return true if offline recognition is supported
 */
fun SpeechEnginePlugin.supportsOffline(): Boolean {
    return hasCapability(SpeechCapability.OFFLINE)
}

/**
 * Check if this engine supports continuous recognition.
 *
 * @return true if continuous recognition is supported
 */
fun SpeechEnginePlugin.supportsContinuous(): Boolean {
    return hasCapability(SpeechCapability.CONTINUOUS)
}

/**
 * Check if this engine supports custom vocabularies.
 *
 * @return true if custom vocabulary is supported
 */
fun SpeechEnginePlugin.supportsCustomVocabulary(): Boolean {
    return hasCapability(SpeechCapability.CUSTOM_VOCABULARY)
}

/**
 * Check if this engine supports a specific language.
 *
 * @param language BCP-47 language tag
 * @return true if the language is supported
 */
fun SpeechEnginePlugin.supportsLanguage(language: String): Boolean {
    return language in supportedLanguages
}

/**
 * Check if this engine is currently listening.
 *
 * @return true if recognition is active
 */
fun SpeechEnginePlugin.isListening(): Boolean {
    return recognitionState.value == RecognitionState.LISTENING
}

/**
 * Check if this engine is idle and ready.
 *
 * @return true if engine is idle
 */
fun SpeechEnginePlugin.isIdle(): Boolean {
    return recognitionState.value == RecognitionState.IDLE
}

/**
 * Check if this engine is in error state.
 *
 * @return true if engine has an error
 */
fun SpeechEnginePlugin.hasError(): Boolean {
    return recognitionState.value == RecognitionState.ERROR
}

/**
 * Start recognition with default configuration.
 *
 * Convenience method that uses [RecognitionConfig.DEFAULT].
 *
 * @return Flow of recognition results
 */
fun SpeechEnginePlugin.startRecognition(): Flow<RecognitionResult> {
    return startRecognition(RecognitionConfig.DEFAULT)
}

/**
 * Start single-command recognition.
 *
 * Convenience method for recognizing a single utterance.
 *
 * @param language Optional language override
 * @return Flow of recognition results
 */
fun SpeechEnginePlugin.startSingleCommand(language: String = "en-US"): Flow<RecognitionResult> {
    return startRecognition(
        RecognitionConfig.SINGLE_COMMAND.copy(language = language)
    )
}
