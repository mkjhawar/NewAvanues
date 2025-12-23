/**
 * AzureEngineAdapter.kt - Adapter for Azure Cognitive Services Speech Recognition
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-22
 *
 * Part of SOLID Refactoring Phase 2: Open/Closed Principle (Factory Pattern)
 * Plan: VoiceOS-Plan-SOLID-Refactoring-5221222-V1.md
 *
 * PURPOSE:
 * Full adapter implementation for Microsoft Azure Cognitive Services Speech Recognition.
 * Provides ISpeechEngine interface for Azure cloud-based recognition.
 *
 * DESIGN PATTERN: Adapter Pattern
 * - Adapts Azure Speech SDK to match ISpeechEngine interface
 * - Single Responsibility: Only handles Azure integration
 * - Cloud-based recognition with advanced features
 *
 * AZURE SPEECH FEATURES:
 * - Industry-leading accuracy
 * - 100+ languages and dialects
 * - Custom models and acoustic adaptation
 * - Speaker recognition and diarization
 * - Real-time translation
 * - Neural voice synthesis
 */
package com.augmentalis.voiceoscore.accessibility.speech

import android.content.Context
import android.util.Log
import com.augmentalis.speechrecognition.SpeechConfig
import com.augmentalis.voiceos.speech.api.RecognitionResult
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechRecognitionEventArgs
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.SpeechConfig as AzureSpeechConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.Future
import kotlin.coroutines.resume

/**
 * Adapter for Microsoft Azure Cognitive Services Speech Recognition
 *
 * Wraps Azure Speech SDK to provide unified ISpeechEngine interface.
 * Uses cloud-based recognition with enterprise-grade accuracy and features.
 *
 * REQUIREMENTS:
 * - Azure Speech Services subscription key
 * - Azure region (e.g., "eastus", "westeurope")
 * - Network connectivity (online-only)
 * - RECORD_AUDIO and INTERNET permissions
 *
 * CONFIGURATION:
 * Credentials should be provided via:
 * - Environment variables: AZURE_SPEECH_KEY, AZURE_SPEECH_REGION
 * - OR configuration file
 * - OR passed in SpeechConfig
 *
 * BENEFITS:
 * - Industry-leading accuracy
 * - Advanced features (speaker recognition, translation, etc.)
 * - Continuous updates and improvements
 * - Enterprise support
 *
 * LIMITATIONS:
 * - Requires network connectivity
 * - Usage costs (pay per hour)
 * - Latency from cloud round-trip
 * - Privacy considerations (audio sent to cloud)
 *
 * PRICING:
 * - Standard: $1/hour for recognition
 * - Custom models: Additional fees
 * - Free tier: 5 hours/month
 *
 * @property context Android application context
 *
 * @see ISpeechEngine
 */
class AzureEngineAdapter(
    private val context: Context
) : ISpeechEngine {

    companion object {
        private const val TAG = "AzureEngineAdapter"

        // Default credentials (should be overridden via configuration)
        private const val DEFAULT_SUBSCRIPTION_KEY = ""  // Set via config
        private const val DEFAULT_REGION = "eastus"  // Set via config
    }

    /**
     * Azure Speech SDK config
     */
    private var azureSpeechConfig: AzureSpeechConfig? = null

    /**
     * Azure Speech recognizer
     */
    private var speechRecognizer: SpeechRecognizer? = null

    /**
     * Track initialization state
     */
    private var isInitialized: Boolean = false

    /**
     * Track listening state
     */
    private var isListening: Boolean = false

    /**
     * Result callback
     */
    private var resultCallback: ((RecognitionResult) -> Unit)? = null

    /**
     * Error callback
     */
    private var errorCallback: ((String, Int) -> Unit)? = null

    /**
     * Current language code
     */
    private var languageCode: String = "en-US"

    /**
     * Subscription key (loaded from config)
     */
    private var subscriptionKey: String = DEFAULT_SUBSCRIPTION_KEY

    /**
     * Region (loaded from config)
     */
    private var region: String = DEFAULT_REGION

    /**
     * Initialize Azure speech recognition
     *
     * Creates SpeechRecognizer with credentials and configuration.
     *
     * @param config Speech configuration (should include Azure credentials)
     * @return true if initialization succeeded
     */
    override suspend fun initialize(config: SpeechConfig): Boolean = suspendCancellableCoroutine { continuation ->
        try {
            Log.d(TAG, "Initializing Azure speech recognition")

            // Load credentials from config or environment
            loadCredentials(config)

            if (subscriptionKey.isEmpty()) {
                Log.e(TAG, "Azure subscription key not provided")
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }

            languageCode = config.language

            // Create Azure Speech Config
            azureSpeechConfig = AzureSpeechConfig.fromSubscription(subscriptionKey, region)
            azureSpeechConfig?.speechRecognitionLanguage = languageCode

            // Optional: Enable additional features
            azureSpeechConfig?.setProfanity(com.microsoft.cognitiveservices.speech.ProfanityOption.Raw)
            azureSpeechConfig?.enableDictation()

            // Create audio config (use default microphone)
            val audioConfig = AudioConfig.fromDefaultMicrophoneInput()

            // Create speech recognizer
            speechRecognizer = SpeechRecognizer(azureSpeechConfig!!, audioConfig)

            // Setup event handlers
            setupEventHandlers()

            isInitialized = true
            Log.i(TAG, "Azure speech recognition initialized successfully")
            continuation.resume(true)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Azure speech recognition", e)
            isInitialized = false
            continuation.resume(false)
        }
    }

    /**
     * Start listening for speech
     *
     * Starts continuous recognition with Azure Speech Services.
     *
     * @throws IllegalStateException if engine not initialized
     */
    override fun startListening() {
        checkInitialized()

        try {
            Log.d(TAG, "Starting Azure speech recognition")

            // Start continuous recognition
            val future: Future<Void> = speechRecognizer?.startContinuousRecognitionAsync()
                ?: throw IllegalStateException("Speech recognizer not initialized")

            // Wait for start to complete
            future.get()

            isListening = true
            Log.i(TAG, "Azure listening started")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Azure speech recognition", e)
            isListening = false
            throw e
        }
    }

    /**
     * Stop listening for speech
     *
     * Stops continuous recognition.
     */
    override fun stopListening() {
        try {
            Log.d(TAG, "Stopping Azure speech recognition")

            // Stop continuous recognition
            val future: Future<Void>? = speechRecognizer?.stopContinuousRecognitionAsync()
            future?.get()

            isListening = false
            Log.i(TAG, "Azure listening stopped")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Azure speech recognition", e)
            isListening = false
        }
    }

    /**
     * Update dynamic command vocabulary
     *
     * Azure supports phrase lists to improve recognition accuracy
     * for specific vocabulary. This adds phrases to the recognition context.
     *
     * @param commands List of voice commands to recognize
     * @throws IllegalStateException if engine not initialized
     */
    override suspend fun updateCommands(commands: List<String>) {
        checkInitialized()

        try {
            Log.d(TAG, "Updating Azure phrase list (${commands.size} commands)")

            // Create phrase list grammar
            val phraseListGrammar = com.microsoft.cognitiveservices.speech.PhraseListGrammar.fromRecognizer(speechRecognizer)

            // Clear existing phrases
            phraseListGrammar.clear()

            // Add new phrases
            commands.forEach { command ->
                phraseListGrammar.addPhrase(command)
            }

            Log.i(TAG, "Azure phrase list updated successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Azure phrase list", e)
            throw e
        }
    }

    /**
     * Update engine configuration
     *
     * For Azure, language and other settings can be updated at runtime.
     *
     * @param config New configuration data
     * @throws IllegalStateException if engine not initialized
     */
    override fun updateConfiguration(config: SpeechConfigurationData) {
        checkInitialized()

        Log.d(TAG, "Updating Azure configuration")

        // Update language if changed
        if (config.language != languageCode) {
            languageCode = config.language
            azureSpeechConfig?.speechRecognitionLanguage = languageCode
            Log.i(TAG, "Azure language updated to: $languageCode")
        }

        // Update profanity filter
        if (config.enableProfanityFilter) {
            azureSpeechConfig?.setProfanity(com.microsoft.cognitiveservices.speech.ProfanityOption.Masked)
        } else {
            azureSpeechConfig?.setProfanity(com.microsoft.cognitiveservices.speech.ProfanityOption.Raw)
        }
    }

    /**
     * Check if engine is currently recognizing
     *
     * @return true if actively listening
     */
    override fun isRecognizing(): Boolean {
        return isInitialized && isListening
    }

    /**
     * Get the underlying Azure SpeechRecognizer instance
     *
     * @return SpeechRecognizer instance
     */
    override fun getEngine(): Any? {
        return speechRecognizer
    }

    /**
     * Set result listener for speech recognition results
     *
     * @param listener Callback for recognition results
     */
    fun setResultListener(listener: (RecognitionResult) -> Unit) {
        resultCallback = listener
    }

    /**
     * Set error listener for speech recognition errors
     *
     * @param listener Callback for errors
     */
    fun setErrorListener(listener: (String, Int) -> Unit) {
        errorCallback = listener
    }

    /**
     * Clean up Azure speech recognition resources
     */
    override fun destroy() {
        try {
            Log.d(TAG, "Destroying Azure speech recognition")

            stopListening()

            speechRecognizer?.close()
            speechRecognizer = null

            azureSpeechConfig?.close()
            azureSpeechConfig = null

            isInitialized = false
            isListening = false

            Log.i(TAG, "Azure speech recognition destroyed")

        } catch (e: Exception) {
            Log.e(TAG, "Error destroying Azure speech recognition", e)
        }
    }

    /**
     * Setup Azure Speech SDK event handlers
     */
    private fun setupEventHandlers() {
        speechRecognizer?.recognized?.addEventListener { _, event ->
            handleRecognized(event)
        }

        speechRecognizer?.recognizing?.addEventListener { _, event ->
            handleRecognizing(event)
        }

        speechRecognizer?.canceled?.addEventListener { _, event ->
            handleCanceled(event)
        }

        speechRecognizer?.sessionStarted?.addEventListener { _, _ ->
            Log.d(TAG, "Azure session started")
        }

        speechRecognizer?.sessionStopped?.addEventListener { _, _ ->
            Log.d(TAG, "Azure session stopped")
            isListening = false
        }
    }

    /**
     * Handle recognized event (final result)
     */
    private fun handleRecognized(event: SpeechRecognitionEventArgs) {
        val result = event.result

        if (result.reason == ResultReason.RecognizedSpeech) {
            val text = result.text
            Log.d(TAG, "Azure recognized: $text")

            if (text.isNotEmpty()) {
                val recognitionResult = RecognitionResult(
                    text = text,
                    confidence = 1.0f  // Azure doesn't expose confidence in this API
                )
                resultCallback?.invoke(recognitionResult)
            }
        } else if (result.reason == ResultReason.NoMatch) {
            Log.d(TAG, "Azure: No speech recognized")
        }
    }

    /**
     * Handle recognizing event (partial result)
     */
    private fun handleRecognizing(event: SpeechRecognitionEventArgs) {
        val text = event.result.text
        Log.d(TAG, "Azure recognizing (partial): $text")
        // Partial results can be processed here if needed
    }

    /**
     * Handle canceled event (errors)
     */
    private fun handleCanceled(event: com.microsoft.cognitiveservices.speech.SpeechRecognitionCanceledEventArgs) {
        val reason = event.reason
        val errorDetails = event.errorDetails

        if (reason == CancellationReason.Error) {
            val errorMessage = "Azure recognition error: $errorDetails"
            Log.e(TAG, errorMessage)
            errorCallback?.invoke(errorMessage, event.errorCode.value)
        } else {
            Log.d(TAG, "Azure recognition canceled: $reason")
        }

        isListening = false
    }

    /**
     * Load Azure credentials from configuration
     *
     * Priority:
     * 1. SpeechConfig parameters
     * 2. Environment variables
     * 3. Default values
     */
    private fun loadCredentials(config: SpeechConfig) {
        // Try to get from config (if extended)
        // For now, load from environment or use defaults

        val envKey = System.getenv("AZURE_SPEECH_KEY")
        val envRegion = System.getenv("AZURE_SPEECH_REGION")

        subscriptionKey = envKey ?: DEFAULT_SUBSCRIPTION_KEY
        region = envRegion ?: DEFAULT_REGION

        if (subscriptionKey.isEmpty()) {
            Log.w(TAG, "Azure subscription key not configured. Set AZURE_SPEECH_KEY environment variable.")
        }

        Log.d(TAG, "Azure credentials loaded - Region: $region")
    }

    /**
     * Check if engine is initialized, throw if not
     *
     * @throws IllegalStateException if not initialized
     */
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("Azure speech recognition not initialized")
        }
    }
}
