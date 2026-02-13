/**
 * SpeechEngineFactoryProvider.ios.kt - iOS actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * iOS implementation of speech engine factory provider.
 */
package com.augmentalis.voiceoscore

/**
 * iOS speech engine factory provider implementation.
 */
actual object SpeechEngineFactoryProvider {
    /**
     * Create iOS-specific speech engine factory.
     *
     * @return ISpeechEngineFactory for iOS platform
     */
    actual fun create(): ISpeechEngineFactory = IOSSpeechEngineFactory()
}

/**
 * iOS speech engine factory implementation.
 *
 * Provides access to iOS-supported speech engines:
 * - APPLE_SPEECH (native)
 * - WHISPER (offline)
 * - GOOGLE_CLOUD (online, requires API key)
 * - AZURE (online, requires subscription)
 */
internal class IOSSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> {
        return listOf(
            SpeechEngine.APPLE_SPEECH,
            SpeechEngine.WHISPER,
            SpeechEngine.GOOGLE_CLOUD,
            SpeechEngine.AZURE
        ).filter { isEngineAvailable(it) }
    }

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return when (engine) {
            SpeechEngine.APPLE_SPEECH -> true // Always available on iOS
            SpeechEngine.WHISPER -> true // Requires model download
            SpeechEngine.GOOGLE_CLOUD -> true // Requires API key
            SpeechEngine.AZURE -> true // Requires subscription
            SpeechEngine.ANDROID_STT -> false // Android only
            SpeechEngine.VOSK -> false // Not yet implemented for iOS
            SpeechEngine.VIVOKA -> false // Android only
        }
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return when (engine) {
            SpeechEngine.APPLE_SPEECH -> Result.success(AppleSpeechEngineAdapter())
            SpeechEngine.WHISPER,
            SpeechEngine.GOOGLE_CLOUD,
            SpeechEngine.AZURE -> Result.failure(
                UnsupportedOperationException("Engine $engine not yet implemented on iOS")
            )
            else -> Result.failure(
                UnsupportedOperationException("Engine $engine is not available on iOS")
            )
        }
    }

    override fun getRecommendedEngine(): SpeechEngine {
        return SpeechEngine.APPLE_SPEECH
    }

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.APPLE_SPEECH -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.PUNCTUATION
            )
            SpeechEngine.WHISPER -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.LANGUAGE_DETECTION,
                EngineFeature.TRANSLATION,
                EngineFeature.PUNCTUATION
            )
            SpeechEngine.GOOGLE_CLOUD -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.SPEAKER_DIARIZATION,
                EngineFeature.PUNCTUATION
            )
            SpeechEngine.AZURE -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.SPEAKER_DIARIZATION,
                EngineFeature.TRANSLATION,
                EngineFeature.PUNCTUATION
            )
            else -> emptySet()
        }
    }

    override fun getSetupRequirements(engine: SpeechEngine): EngineRequirements {
        return when (engine) {
            SpeechEngine.APPLE_SPEECH -> EngineRequirements(
                permissions = listOf("NSSpeechRecognitionUsageDescription", "NSMicrophoneUsageDescription"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Uses iOS Speech framework"
            )
            SpeechEngine.WHISPER -> EngineRequirements(
                permissions = listOf("NSMicrophoneUsageDescription"),
                requiresModelDownload = true,
                modelSizeMB = 230,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download Whisper model for offline recognition"
            )
            SpeechEngine.GOOGLE_CLOUD -> EngineRequirements(
                permissions = listOf("NSMicrophoneUsageDescription"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Google Cloud API key"
            )
            SpeechEngine.AZURE -> EngineRequirements(
                permissions = listOf("NSMicrophoneUsageDescription"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Azure subscription key"
            )
            else -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Not available on iOS"
            )
        }
    }
}
