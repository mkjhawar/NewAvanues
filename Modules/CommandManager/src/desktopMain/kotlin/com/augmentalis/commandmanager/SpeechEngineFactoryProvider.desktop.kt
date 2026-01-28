/**
 * SpeechEngineFactoryProvider.desktop.kt - Desktop actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * Desktop/JVM implementation of speech engine factory provider.
 */
package com.augmentalis.commandmanager

/**
 * Desktop speech engine factory provider implementation.
 */
actual object SpeechEngineFactoryProvider {
    /**
     * Create Desktop-specific speech engine factory.
     *
     * @return ISpeechEngineFactory for Desktop platform
     */
    actual fun create(): ISpeechEngineFactory = DesktopSpeechEngineFactory()
}

/**
 * Desktop speech engine factory implementation.
 *
 * Provides access to Desktop-supported speech engines:
 * - VOSK (offline)
 * - WHISPER (offline)
 * - GOOGLE_CLOUD (online, requires API key)
 * - AZURE (online, requires subscription)
 */
internal class DesktopSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> {
        return listOf(
            SpeechEngine.VOSK,
            SpeechEngine.WHISPER,
            SpeechEngine.GOOGLE_CLOUD,
            SpeechEngine.AZURE
        ).filter { isEngineAvailable(it) }
    }

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return when (engine) {
            SpeechEngine.VOSK -> true // JNI bindings available
            SpeechEngine.WHISPER -> true // Requires model download
            SpeechEngine.GOOGLE_CLOUD -> true // Requires API key
            SpeechEngine.AZURE -> true // Requires subscription
            SpeechEngine.ANDROID_STT -> false // Android only
            SpeechEngine.APPLE_SPEECH -> false // iOS/macOS only
            SpeechEngine.VIVOKA -> false // Android only
        }
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        // TODO: Implement actual engine creation
        return Result.failure(UnsupportedOperationException("Engine $engine not yet implemented on Desktop"))
    }

    override fun getRecommendedEngine(): SpeechEngine {
        return SpeechEngine.VOSK
    }

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.VOSK -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.CUSTOM_VOCABULARY
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
            SpeechEngine.VOSK -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = true,
                modelSizeMB = 50,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download language model for offline recognition"
            )
            SpeechEngine.WHISPER -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = true,
                modelSizeMB = 230,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download Whisper model for offline recognition"
            )
            SpeechEngine.GOOGLE_CLOUD -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Google Cloud API key"
            )
            SpeechEngine.AZURE -> EngineRequirements(
                permissions = emptyList(),
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
                notes = "Not available on Desktop"
            )
        }
    }
}
