/**
 * VOS4 Speech Engine Plugin Interface
 *
 * Allows third-party plugins to provide custom speech recognition engines
 * that integrate seamlessly with VOS4's multi-engine architecture.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-10-26
 */
package com.augmentalis.magiccode.plugins.vos4

import android.content.Context
import com.augmentalis.magiccode.plugins.core.PluginManifest
import kotlinx.coroutines.flow.Flow

/**
 * Plugin interface for speech recognition engines
 *
 * Implementations can:
 * - Provide custom speech recognition engines
 * - Support multiple languages and models
 * - Stream recognition results in real-time
 * - Provide confidence scores
 * - Support continuous and single-shot recognition
 *
 * ## Liskov Substitution Principle (LSP) Contract
 *
 * All implementations MUST adhere to the following behavioral contracts:
 *
 * ### Lifecycle Contracts
 * - initialize() MUST be called before any other operations
 * - isReady() MUST return false until initialize() succeeds
 * - startRecognition() MUST fail if isReady() returns false
 * - release() MUST cleanup all resources and make engine unusable
 * - Multiple initialize() calls without release() MUST be handled gracefully
 *
 * ### State Consistency
 * - Engine state transitions: UNINITIALIZED -> INITIALIZED -> LISTENING -> STOPPED
 * - stopRecognition() when not listening is no-op (NOT error)
 * - release() transitions to UNINITIALIZED state
 *
 * ### Exception Behavior
 * - initialize() returns false on failure (NOT throw)
 * - startRecognition() emits RecognitionResult.Error on failure
 * - Network errors emit RecognitionResult.Error (NOT throw)
 * - Permission errors emit RecognitionResult.Error with PERMISSION_DENIED code
 *
 * ### Thread Safety
 * - All methods MUST be thread-safe
 * - Flow emissions MUST be synchronized
 * - State changes MUST be atomic
 */
interface SpeechEnginePluginInterface {

    /**
     * Plugin manifest with metadata
     *
     * ## LSP Contract:
     * - MUST be immutable after construction
     * - MUST contain valid plugin metadata
     */
    val manifest: PluginManifest

    /**
     * Engine metadata
     *
     * @return SpeechEngineInfo with engine capabilities
     *
     * ## LSP Contract:
     * - MUST return same info on every call (immutable)
     * - MUST accurately reflect engine capabilities
     * - MUST be callable before initialization
     */
    fun provideEngineInfo(): SpeechEngineInfo

    /**
     * Initialize the speech engine
     *
     * @param context Android context
     * @param config Engine configuration
     * @return true if initialization succeeded, false otherwise
     *
     * ## LSP Contract:
     * - MUST return true only if engine is ready to recognize speech
     * - MUST return false on failure (NOT throw exception)
     * - MUST be idempotent (safe to call multiple times)
     * - MUST make isReady() return true only after successful initialization
     * - MUST handle missing permissions gracefully (return false)
     * - MUST handle missing models gracefully (return false)
     * - Network errors during init MUST return false
     */
    suspend fun initialize(context: Context, config: SpeechEngineConfig): Boolean

    /**
     * Check if engine is initialized and ready
     *
     * @return true if ready to recognize speech, false otherwise
     *
     * ## LSP Contract:
     * - MUST return false before initialize() succeeds
     * - MUST return true after successful initialize()
     * - MUST return false after release()
     * - MUST be thread-safe
     * - MUST NOT throw exceptions
     */
    fun isReady(): Boolean

    /**
     * Start speech recognition
     *
     * @param mode Recognition mode (continuous or single-shot)
     * @param language Target language code (e.g., "en-US")
     * @return Flow of recognition results
     *
     * ## LSP Contract:
     * - MUST emit RecognitionResult.Listening when recognition starts
     * - MUST emit RecognitionResult.Partial for intermediate results (if supported)
     * - MUST emit RecognitionResult.Final for final results
     * - MUST emit RecognitionResult.Error on failures (NOT throw)
     * - MUST emit RecognitionResult.Stopped when recognition ends
     * - Flow MUST NOT complete until stopRecognition() or error
     * - MUST emit Error(NOT_INITIALIZED) if isReady() returns false
     * - MUST emit Error(UNSUPPORTED_LANGUAGE) for unsupported languages
     * - MUST be safe to collect from any thread
     */
    fun startRecognition(mode: RecognitionMode, language: String): Flow<RecognitionResult>

    /**
     * Stop speech recognition
     *
     * ## LSP Contract:
     * - MUST be idempotent (safe to call when not listening)
     * - MUST cause recognition Flow to emit Stopped
     * - MUST NOT throw exceptions
     * - MUST be thread-safe
     * - MUST complete quickly (non-blocking)
     */
    suspend fun stopRecognition()

    /**
     * Set custom vocabulary for better recognition
     *
     * @param vocabulary List of words/phrases to prioritize
     *
     * ## LSP Contract:
     * - MUST handle empty list gracefully (no-op or reset to defaults)
     * - MUST be no-op if engine doesn't support custom vocabulary
     * - MUST NOT throw on unsupported feature
     * - Can be called while recognizing (updates dynamically)
     * - MUST be thread-safe
     */
    suspend fun setVocabulary(vocabulary: List<String>)

    /**
     * Get supported languages
     *
     * @return List of language codes this engine supports
     *
     * ## LSP Contract:
     * - MUST return non-empty list (engine must support at least one language)
     * - MUST be callable before initialization
     * - MUST return same list on every call (immutable)
     * - Language codes MUST follow BCP-47 (e.g., "en-US", "es-MX")
     * - MUST be thread-safe
     */
    fun getSupportedLanguages(): List<LanguageInfo>

    /**
     * Release engine resources
     *
     * ## LSP Contract:
     * - MUST stop any ongoing recognition
     * - MUST release all native resources
     * - MUST make isReady() return false
     * - MUST be idempotent (safe to call multiple times)
     * - MUST NOT throw exceptions
     * - After release(), initialize() can be called again
     * - MUST be thread-safe
     */
    suspend fun release()
}

/**
 * Speech engine metadata
 */
data class SpeechEngineInfo(
    val id: String,
    val name: String,
    val vendor: String,
    val version: String,
    val description: String,
    val requiresNetwork: Boolean = false,
    val requiresDownload: Boolean = false,
    val supportsStreaming: Boolean = true,
    val supportsPartialResults: Boolean = true,
    val features: List<EngineFeature> = emptyList()
)

/**
 * Engine features
 */
enum class EngineFeature {
    SPEAKER_ADAPTATION,     // Learn user's voice
    NOISE_CANCELLATION,    // Background noise filtering
    MULTI_LANGUAGE,        // Multiple languages simultaneously
    OFFLINE_MODE,          // Works without internet
    CUSTOM_MODELS,         // Support custom trained models
    WAKE_WORD,            // Wake word detection
    VOICE_ACTIVITY_DETECTION  // Detect when user is speaking
}

/**
 * Engine configuration
 */
data class SpeechEngineConfig(
    val modelPath: String? = null,
    val apiKey: String? = null,
    val sampleRate: Int = 16000,  // Hz
    val enableNoiseReduction: Boolean = true,
    val confidenceThreshold: Float = 0.5f,
    val maxSpeechTimeout: Long = 10000,  // milliseconds
    val customParameters: Map<String, Any>? = null
)

/**
 * Recognition modes
 */
enum class RecognitionMode {
    CONTINUOUS,      // Keep listening
    SINGLE_UTTERANCE // Stop after one phrase
}

/**
 * Recognition result
 */
sealed class RecognitionResult {
    data class Partial(
        val text: String,
        val confidence: Float,
        val timestamp: Long = System.currentTimeMillis()
    ) : RecognitionResult()

    data class Final(
        val text: String,
        val confidence: Float,
        val alternatives: List<Alternative> = emptyList(),
        val timestamp: Long = System.currentTimeMillis()
    ) : RecognitionResult()

    data class Error(
        val code: ErrorCode,
        val message: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : RecognitionResult()

    object Listening : RecognitionResult()
    object Stopped : RecognitionResult()
}

/**
 * Alternative recognition result
 */
data class Alternative(
    val text: String,
    val confidence: Float
)

/**
 * Error codes
 */
enum class ErrorCode {
    NETWORK_ERROR,
    AUDIO_ERROR,
    RECOGNITION_ERROR,
    TIMEOUT,
    PERMISSION_DENIED,
    NOT_INITIALIZED,
    MODEL_NOT_FOUND,
    UNSUPPORTED_LANGUAGE,
    UNKNOWN
}

/**
 * Language information
 */
data class LanguageInfo(
    val code: String,        // e.g., "en-US"
    val name: String,        // e.g., "English (United States)"
    val models: List<ModelInfo> = emptyList()
)

/**
 * Model information
 */
data class ModelInfo(
    val id: String,
    val name: String,
    val size: Long,  // bytes
    val downloaded: Boolean = false,
    val path: String? = null
)
