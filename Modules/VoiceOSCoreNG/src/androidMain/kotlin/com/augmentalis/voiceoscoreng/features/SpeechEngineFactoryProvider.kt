/**
 * SpeechEngineFactoryProvider.kt - Android implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Android-specific factory provider for speech engines.
 */
package com.augmentalis.voiceoscoreng.features

import com.augmentalis.voiceoscoreng.features.VivokaEngineFactory

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
            SpeechEngine.VOSK -> {
                val ctx = context
                    ?: return Result.failure(IllegalStateException("Context required for VOSK"))
                Result.success(VoskEngineImpl(ctx))
            }
            SpeechEngine.GOOGLE_CLOUD -> Result.success(GoogleCloudEngine())
            SpeechEngine.AZURE -> Result.success(AzureEngine())
            SpeechEngine.VIVOKA -> {
                Result.success(VivokaEngineFactory.create(VivokaConfig.DEFAULT))
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
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.WORD_TIMESTAMPS
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
                permissions = listOf(
                    "android.permission.RECORD_AUDIO",
                    "android.permission.READ_EXTERNAL_STORAGE"
                ),
                requiresModelDownload = true,
                modelSizeMB = 500,  // ~500MB for full VSDK with models
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Models loaded from external storage: /sdcard/.voiceos/vivoka/vsdk/"
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

// VoskEngine is now implemented in VoskEngineImpl.kt
// Requires Context parameter for file access and permissions

/**
 * GoogleCloudEngine - Delegates to GoogleCloudEngineImpl for Google Cloud Speech-to-Text.
 *
 * This class is kept as an internal alias for backwards compatibility.
 * The actual implementation is in GoogleCloudEngineImpl.kt which provides:
 * - Streaming recognition via REST API
 * - Partial and final results via SharedFlow
 * - Multiple language support
 * - Word-level timestamps
 * - Speaker diarization
 * - Profanity filtering
 *
 * Requirements:
 * - Google Cloud API key with Speech-to-Text API enabled (apiKey in SpeechConfig)
 * - RECORD_AUDIO permission
 * - Network connectivity
 *
 * @see GoogleCloudEngineImpl
 */
internal class GoogleCloudEngine : ISpeechEngine by GoogleCloudEngineImpl()

/**
 * AzureEngine - Delegates to AzureEngineImpl for Azure Cognitive Services integration.
 *
 * This class is kept as an internal alias for backwards compatibility.
 * The actual implementation is in AzureEngineImpl.kt which provides:
 * - Continuous recognition via Azure Speech SDK
 * - Partial and final results via SharedFlow
 * - Phrase list support for command boosting
 * - Word-level timestamps
 * - Speaker diarization (enterprise tier)
 *
 * Requirements:
 * - Azure subscription key (apiKey in SpeechConfig)
 * - Azure region (apiRegion in SpeechConfig)
 * - RECORD_AUDIO permission
 * - Network connectivity
 *
 * SDK Dependency (add to build.gradle.kts):
 * implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.35.0")
 */
internal class AzureEngine : ISpeechEngine by AzureEngineImpl()
