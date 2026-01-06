/**
 * SpeechEngineFactoryProvider.kt - Android implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Android-specific factory provider for speech engines.
 */
package com.augmentalis.voiceoscoreng.speech

import com.augmentalis.voiceoscoreng.speech.vivoka.VivokaEngineFactory

/**
 * Android implementation of SpeechEngineFactoryProvider.
 *
 * Creates AndroidSpeechEngineFactory which supports:
 * - VOSK (offline)
 * - ANDROID_STT (native)
 * - GOOGLE_CLOUD (online)
 * - AZURE (online)
 * - WHISPER (offline, requires NDK)
 * - VIVOKA (hybrid, commercial) - KMP interface to SpeechRecognition library
 */
actual object SpeechEngineFactoryProvider {
    actual fun create(): ISpeechEngineFactory = AndroidSpeechEngineFactory()

    /**
     * Create factory with Android context.
     * Required for engines that need SpeechRecognizer (ANDROID_STT) or Vivoka.
     */
    fun create(context: android.content.Context): ISpeechEngineFactory {
        // Initialize Vivoka factory with context
        VivokaEngineFactory.initialize(context)
        return AndroidSpeechEngineFactory(context)
    }
}

/**
 * Android-specific speech engine factory.
 *
 * @param context Android application context (required for SpeechRecognizer)
 */
class AndroidSpeechEngineFactory(
    private val context: android.content.Context? = null
) : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> = buildList {
        add(SpeechEngine.ANDROID_STT)
        add(SpeechEngine.VOSK)
        add(SpeechEngine.GOOGLE_CLOUD)
        add(SpeechEngine.AZURE)
        // Add VIVOKA if SDK is available
        if (VivokaEngineFactory.isAvailable()) {
            add(SpeechEngine.VIVOKA)
        }
    }

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return when (engine) {
            SpeechEngine.VIVOKA -> VivokaEngineFactory.isAvailable()
            else -> engine in getAvailableEngines()
        }
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> {
                val ctx = context
                    ?: return Result.failure(IllegalStateException("Context required for AndroidSTT"))
                Result.success(AndroidSTTEngineImpl(ctx))
            }
            SpeechEngine.VOSK -> Result.success(VoskEngine())
            SpeechEngine.GOOGLE_CLOUD -> Result.success(GoogleCloudEngine())
            SpeechEngine.AZURE -> Result.success(AzureEngine())
            SpeechEngine.VIVOKA -> {
                Result.success(VivokaEngineFactory.create())
            }
            else -> Result.failure(
                IllegalArgumentException("Engine ${engine.name} not available on Android")
            )
        }
    }

    override fun getRecommendedEngine(): SpeechEngine = SpeechEngine.ANDROID_STT

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.PUNCTUATION
            )
            SpeechEngine.VOSK -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CUSTOM_VOCABULARY,
                EngineFeature.CONTINUOUS_RECOGNITION
            )
            SpeechEngine.GOOGLE_CLOUD -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.PUNCTUATION,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.SPEAKER_DIARIZATION,
                EngineFeature.PROFANITY_FILTER
            )
            SpeechEngine.AZURE -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.PUNCTUATION,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.TRANSLATION,
                EngineFeature.SPEAKER_DIARIZATION
            )
            SpeechEngine.WHISPER -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.LANGUAGE_DETECTION,
                EngineFeature.TRANSLATION
            )
            SpeechEngine.VIVOKA -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.WAKE_WORD,
                EngineFeature.CUSTOM_VOCABULARY
            )
            else -> emptySet()
        }
    }

    override fun getSetupRequirements(engine: SpeechEngine): EngineRequirements {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = false,
                notes = "Uses Google services"
            )
            SpeechEngine.VOSK -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 50,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download model from alphacephei.com/vosk/models"
            )
            SpeechEngine.GOOGLE_CLOUD -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Google Cloud API key"
            )
            SpeechEngine.AZURE -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Azure subscription key and region"
            )
            SpeechEngine.WHISPER -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 244,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Requires NDK setup and model download"
            )
            SpeechEngine.VIVOKA -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 60,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Commercial license required"
            )
            else -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Engine not supported on Android"
            )
        }
    }
}

// AndroidSTTEngine is now implemented in AndroidSTTEngineImpl.kt
// Requires Context parameter for SpeechRecognizer

internal class VoskEngine : ISpeechEngine {
    // TODO: Implement using Vosk library
    override val state = kotlinx.coroutines.flow.MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val results = kotlinx.coroutines.flow.MutableSharedFlow<SpeechResult>()
    override val errors = kotlinx.coroutines.flow.MutableSharedFlow<SpeechError>()

    override suspend fun initialize(config: SpeechConfig) = Result.success(Unit)
    override suspend fun startListening() = Result.success(Unit)
    override suspend fun stopListening() {}
    override suspend fun updateCommands(commands: List<String>) = Result.success(Unit)
    override suspend fun updateConfiguration(config: SpeechConfig) = Result.success(Unit)
    override fun isRecognizing() = false
    override fun isInitialized() = true
    override fun getEngineType() = SpeechEngine.VOSK
    override fun getSupportedFeatures() = setOf(EngineFeature.OFFLINE_MODE)
    override suspend fun destroy() {}
}

internal class GoogleCloudEngine : ISpeechEngine {
    // TODO: Implement using Google Cloud Speech API
    override val state = kotlinx.coroutines.flow.MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val results = kotlinx.coroutines.flow.MutableSharedFlow<SpeechResult>()
    override val errors = kotlinx.coroutines.flow.MutableSharedFlow<SpeechError>()

    override suspend fun initialize(config: SpeechConfig) = Result.success(Unit)
    override suspend fun startListening() = Result.success(Unit)
    override suspend fun stopListening() {}
    override suspend fun updateCommands(commands: List<String>) = Result.success(Unit)
    override suspend fun updateConfiguration(config: SpeechConfig) = Result.success(Unit)
    override fun isRecognizing() = false
    override fun isInitialized() = true
    override fun getEngineType() = SpeechEngine.GOOGLE_CLOUD
    override fun getSupportedFeatures() = setOf<EngineFeature>()
    override suspend fun destroy() {}
}

internal class AzureEngine : ISpeechEngine {
    // TODO: Implement using Azure Cognitive Services SDK
    override val state = kotlinx.coroutines.flow.MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val results = kotlinx.coroutines.flow.MutableSharedFlow<SpeechResult>()
    override val errors = kotlinx.coroutines.flow.MutableSharedFlow<SpeechError>()

    override suspend fun initialize(config: SpeechConfig) = Result.success(Unit)
    override suspend fun startListening() = Result.success(Unit)
    override suspend fun stopListening() {}
    override suspend fun updateCommands(commands: List<String>) = Result.success(Unit)
    override suspend fun updateConfiguration(config: SpeechConfig) = Result.success(Unit)
    override fun isRecognizing() = false
    override fun isInitialized() = true
    override fun getEngineType() = SpeechEngine.AZURE
    override fun getSupportedFeatures() = setOf<EngineFeature>()
    override suspend fun destroy() {}
}
