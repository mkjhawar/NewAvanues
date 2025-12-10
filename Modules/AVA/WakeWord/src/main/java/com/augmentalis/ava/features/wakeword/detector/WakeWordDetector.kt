// filename: Universal/AVA/Features/WakeWord/src/main/java/com/augmentalis/ava/features/wakeword/detector/WakeWordDetector.kt
// created: 2025-11-22
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.features.wakeword.detector

import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import android.content.Context
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.features.llm.domain.ProviderType
import com.augmentalis.ava.features.llm.security.ApiKeyManager
import com.augmentalis.ava.features.wakeword.WakeWordKeyword
import com.augmentalis.ava.features.wakeword.WakeWordSettings
import com.augmentalis.ava.features.wakeword.WakeWordState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wake Word Detector using Porcupine engine
 *
 * Provides hands-free activation for AVA AI via "Hey AVA" or "OK AVA" wake words.
 *
 * Features:
 * - Porcupine wake word engine integration
 * - On-device processing (privacy-first)
 * - Low CPU and battery usage
 * - Configurable sensitivity
 * - Multiple wake word options
 *
 * Usage:
 * ```
 * val detector = WakeWordDetector(context, apiKeyManager)
 * detector.initialize(settings) { keyword ->
 *     // Wake word detected!
 *     startVoiceInput()
 * }
 * detector.start()
 * ```
 *
 * @author Manoj Jhawar
 */
@Singleton
class WakeWordDetector @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiKeyManager: ApiKeyManager
) {
    companion object {
        private const val TAG = "WakeWordDetector"

        // Custom wake word model files (if available in assets/)
        private const val HEY_AVA_MODEL = "hey-ava_android.ppn"
        private const val OK_AVA_MODEL = "ok-ava_android.ppn"
    }

    private var porcupineManager: PorcupineManager? = null
    private var onDetectionCallback: ((WakeWordKeyword) -> Unit)? = null

    private val _state = MutableStateFlow(WakeWordState.UNINITIALIZED)
    val state: StateFlow<WakeWordState> = _state.asStateFlow()

    private val _detectionCount = MutableStateFlow(0)
    val detectionCount: StateFlow<Int> = _detectionCount.asStateFlow()

    /**
     * Initialize wake word detector
     *
     * Loads Porcupine engine with specified settings and wake word.
     *
     * @param settings Wake word configuration
     * @param onDetected Callback when wake word is detected
     * @return Result.Success if initialized, Result.Error otherwise
     */
    suspend fun initialize(
        settings: WakeWordSettings,
        onDetected: (WakeWordKeyword) -> Unit
    ): Result<Unit> {
        return try {
            _state.value = WakeWordState.INITIALIZING
            onDetectionCallback = onDetected

            // Get Porcupine API key (uses ProviderType enum for consistency)
            // Note: We'll add PORCUPINE to ProviderType enum, but for now use a workaround
            val accessKey = getPorcupineAccessKey()
                ?: return Result.Error(
                    exception = IllegalStateException("Porcupine access key not found"),
                    message = "Porcupine access key not configured. Please add it in settings or set AVA_PORCUPINE_API_KEY environment variable."
                )

            // Build Porcupine manager
            porcupineManager = buildPorcupineManager(
                accessKey = accessKey,
                keyword = settings.keyword,
                sensitivity = settings.sensitivity
            )

            _state.value = WakeWordState.STOPPED
            Timber.i("Wake word detector initialized successfully with keyword: ${settings.keyword.displayName}")
            Result.Success(Unit)

        } catch (e: PorcupineException) {
            _state.value = WakeWordState.ERROR
            Timber.e(e, "Failed to initialize Porcupine wake word detector")
            Result.Error(
                exception = e,
                message = "Failed to initialize wake word detector: ${e.message}"
            )
        } catch (e: Exception) {
            _state.value = WakeWordState.ERROR
            Timber.e(e, "Failed to initialize wake word detector")
            Result.Error(
                exception = e,
                message = "Failed to initialize wake word detector: ${e.message}"
            )
        }
    }

    /**
     * Start wake word detection
     *
     * Begins listening for wake word. Must call initialize() first.
     *
     * @return Result.Success if started, Result.Error otherwise
     */
    suspend fun start(): Result<Unit> {
        return try {
            if (porcupineManager == null) {
                return Result.Error(
                    exception = IllegalStateException("Not initialized"),
                    message = "Wake word detector not initialized. Call initialize() first."
                )
            }

            porcupineManager?.start()
            _state.value = WakeWordState.LISTENING
            Timber.i("Wake word detection started")
            Result.Success(Unit)

        } catch (e: PorcupineException) {
            _state.value = WakeWordState.ERROR
            Timber.e(e, "Failed to start wake word detection")
            Result.Error(
                exception = e,
                message = "Failed to start wake word detection: ${e.message}"
            )
        }
    }

    /**
     * Stop wake word detection
     *
     * Stops listening for wake word but keeps detector initialized.
     *
     * @return Result.Success if stopped, Result.Error otherwise
     */
    suspend fun stop(): Result<Unit> {
        return try {
            porcupineManager?.stop()
            _state.value = WakeWordState.STOPPED
            Timber.i("Wake word detection stopped")
            Result.Success(Unit)

        } catch (e: PorcupineException) {
            Timber.e(e, "Failed to stop wake word detection")
            Result.Error(
                exception = e,
                message = "Failed to stop wake word detection: ${e.message}"
            )
        }
    }

    /**
     * Pause wake word detection
     *
     * Temporarily pauses detection (e.g., when screen is off for battery optimization).
     *
     * @param reason Reason for pausing (for logging)
     */
    suspend fun pause(reason: String) {
        if (_state.value == WakeWordState.LISTENING) {
            porcupineManager?.stop()
            _state.value = WakeWordState.PAUSED
            Timber.i("Wake word detection paused: $reason")
        }
    }

    /**
     * Resume wake word detection
     *
     * Resumes detection after pause.
     */
    suspend fun resume() {
        if (_state.value == WakeWordState.PAUSED) {
            porcupineManager?.start()
            _state.value = WakeWordState.LISTENING
            Timber.i("Wake word detection resumed")
        }
    }

    /**
     * Clean up resources
     *
     * Stops detection and releases Porcupine engine.
     * Call this when wake word detection is no longer needed.
     */
    suspend fun cleanup() {
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            porcupineManager = null
            _state.value = WakeWordState.UNINITIALIZED
            Timber.i("Wake word detector cleaned up")
        } catch (e: Exception) {
            Timber.e(e, "Error cleaning up wake word detector")
        }
    }

    /**
     * Update sensitivity
     *
     * Changes detection sensitivity without reinitialization.
     * Note: Requires rebuilding PorcupineManager, so this is relatively expensive.
     *
     * @param sensitivity New sensitivity (0.0 - 1.0)
     */
    suspend fun updateSensitivity(sensitivity: Float): Result<Unit> {
        // Note: Porcupine requires rebuilding to change sensitivity
        // This is a limitation of the current API
        return Result.Error(
            exception = UnsupportedOperationException("Changing sensitivity requires reinitialization"),
            message = "To change sensitivity, please stop and reinitialize the detector with new settings."
        )
    }

    /**
     * Check if detector is currently listening
     */
    fun isListening(): Boolean {
        return _state.value == WakeWordState.LISTENING
    }

    /**
     * Get Porcupine access key from ApiKeyManager or environment
     *
     * Priority:
     * 1. Environment variable: AVA_PORCUPINE_API_KEY
     * 2. Encrypted SharedPreferences (future: add PORCUPINE to ProviderType)
     *
     * @return Access key or null if not found
     */
    private suspend fun getPorcupineAccessKey(): String? {
        // Check environment variable first
        val envKey = System.getenv("AVA_PORCUPINE_API_KEY")
        if (envKey != null && envKey.isNotBlank()) {
            Timber.d("Using Porcupine access key from environment variable")
            return envKey
        }

        // TODO: Add PORCUPINE to ProviderType enum and use ApiKeyManager
        // For now, return null if env var not set
        Timber.w("Porcupine access key not found in environment. Set AVA_PORCUPINE_API_KEY.")
        return null
    }

    /**
     * Build PorcupineManager with keyword and sensitivity
     *
     * @param accessKey Porcupine access key
     * @param keyword Wake word keyword
     * @param sensitivity Detection sensitivity (0.0 - 1.0)
     * @return PorcupineManager instance
     */
    private fun buildPorcupineManager(
        accessKey: String,
        keyword: WakeWordKeyword,
        sensitivity: Float
    ): PorcupineManager {
        // Create callback for wake word detection
        val callback = PorcupineManagerCallback { keywordIndex ->
            // Wake word detected!
            _detectionCount.value += 1
            Timber.i("Wake word detected: ${keyword.displayName} (index: $keywordIndex, count: ${_detectionCount.value})")

            // Invoke callback
            onDetectionCallback?.invoke(keyword)
        }

        // Determine which keyword to use
        // Porcupine supports built-in keywords and custom models
        return when (keyword) {
            WakeWordKeyword.HEY_AVA -> {
                // Try custom model first, fall back to Jarvis (similar)
                if (hasCustomModel(HEY_AVA_MODEL)) {
                    buildWithCustomModel(accessKey, HEY_AVA_MODEL, sensitivity, callback)
                } else {
                    Timber.w("Custom 'Hey AVA' model not found, using built-in 'Jarvis' keyword")
                    buildWithBuiltinKeyword(accessKey, Porcupine.BuiltInKeyword.JARVIS, sensitivity, callback)
                }
            }
            WakeWordKeyword.OK_AVA -> {
                // Try custom model first, fall back to Alexa (similar)
                if (hasCustomModel(OK_AVA_MODEL)) {
                    buildWithCustomModel(accessKey, OK_AVA_MODEL, sensitivity, callback)
                } else {
                    Timber.w("Custom 'OK AVA' model not found, using built-in 'Alexa' keyword")
                    buildWithBuiltinKeyword(accessKey, Porcupine.BuiltInKeyword.ALEXA, sensitivity, callback)
                }
            }
            WakeWordKeyword.JARVIS -> {
                buildWithBuiltinKeyword(accessKey, Porcupine.BuiltInKeyword.JARVIS, sensitivity, callback)
            }
            WakeWordKeyword.ALEXA -> {
                buildWithBuiltinKeyword(accessKey, Porcupine.BuiltInKeyword.ALEXA, sensitivity, callback)
            }
            WakeWordKeyword.COMPUTER -> {
                buildWithBuiltinKeyword(accessKey, Porcupine.BuiltInKeyword.COMPUTER, sensitivity, callback)
            }
        }
    }

    /**
     * Build PorcupineManager with built-in keyword
     */
    private fun buildWithBuiltinKeyword(
        accessKey: String,
        keyword: Porcupine.BuiltInKeyword,
        sensitivity: Float,
        callback: PorcupineManagerCallback
    ): PorcupineManager {
        return PorcupineManager.Builder()
            .setAccessKey(accessKey)
            .setKeyword(keyword)
            .setSensitivity(sensitivity)
            .build(context, callback)
    }

    /**
     * Build PorcupineManager with custom model file
     */
    private fun buildWithCustomModel(
        accessKey: String,
        modelFileName: String,
        sensitivity: Float,
        callback: PorcupineManagerCallback
    ): PorcupineManager {
        // Custom model path in assets/
        val modelPath = "models/$modelFileName"

        return PorcupineManager.Builder()
            .setAccessKey(accessKey)
            .setKeywordPath(modelPath)
            .setSensitivity(sensitivity)
            .build(context, callback)
    }

    /**
     * Check if custom wake word model exists in assets
     */
    private fun hasCustomModel(modelFileName: String): Boolean {
        return try {
            val modelPath = "models/$modelFileName"
            context.assets.open(modelPath).use { true }
        } catch (e: Exception) {
            false
        }
    }
}
