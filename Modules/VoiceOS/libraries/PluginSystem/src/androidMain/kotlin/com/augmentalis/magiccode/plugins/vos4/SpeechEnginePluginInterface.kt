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
 */
interface SpeechEnginePluginInterface {

    /**
     * Plugin manifest with metadata
     */
    val manifest: PluginManifest

    /**
     * Engine metadata
     */
    fun provideEngineInfo(): SpeechEngineInfo

    /**
     * Initialize the speech engine
     *
     * @param context Android context
     * @param config Engine configuration
     * @return true if initialization succeeded
     */
    suspend fun initialize(context: Context, config: SpeechEngineConfig): Boolean

    /**
     * Check if engine is initialized and ready
     */
    fun isReady(): Boolean

    /**
     * Start speech recognition
     *
     * @param mode Recognition mode (continuous or single-shot)
     * @param language Target language code (e.g., "en-US")
     * @return Flow of recognition results
     */
    fun startRecognition(mode: RecognitionMode, language: String): Flow<RecognitionResult>

    /**
     * Stop speech recognition
     */
    suspend fun stopRecognition()

    /**
     * Set custom vocabulary for better recognition
     *
     * @param vocabulary List of words/phrases to prioritize
     */
    suspend fun setVocabulary(vocabulary: List<String>)

    /**
     * Get supported languages
     *
     * @return List of language codes this engine supports
     */
    fun getSupportedLanguages(): List<LanguageInfo>

    /**
     * Release engine resources
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
