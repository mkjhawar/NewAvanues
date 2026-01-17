/**
 * SpeechEngineFactoryProvider.android.kt - Android actual implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-17
 *
 * Android implementation of speech engine factory provider.
 */
package com.augmentalis.voiceoscore

/**
 * Android speech engine factory provider implementation.
 */
actual object SpeechEngineFactoryProvider {
    /**
     * Create Android-specific speech engine factory.
     *
     * @return ISpeechEngineFactory for Android platform
     */
    actual fun create(): ISpeechEngineFactory = AndroidSpeechEngineFactory()
}

/**
 * Android speech engine factory implementation.
 *
 * Provides access to Android-supported speech engines:
 * - ANDROID_STT (native)
 * - VOSK (offline)
 * - WHISPER (offline)
 * - VIVOKA (commercial, if SDK available)
 * - GOOGLE_CLOUD (online, requires API key)
 * - AZURE (online, requires subscription)
 */
internal class AndroidSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> {
        return listOf(
            SpeechEngine.ANDROID_STT,
            SpeechEngine.VOSK,
            SpeechEngine.WHISPER,
            SpeechEngine.GOOGLE_CLOUD,
            SpeechEngine.AZURE
        ).filter { isEngineAvailable(it) }
    }

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> true // Always available on Android
            SpeechEngine.VOSK -> true // Requires model download
            SpeechEngine.WHISPER -> true // Requires model download
            SpeechEngine.VIVOKA -> VivokaEngineFactory.isAvailable()
            SpeechEngine.GOOGLE_CLOUD -> true // Requires API key
            SpeechEngine.AZURE -> true // Requires subscription
            SpeechEngine.APPLE_SPEECH -> false // iOS only
        }
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return when (engine) {
            SpeechEngine.VIVOKA -> {
                if (VivokaEngineFactory.isAvailable()) {
                    Result.success(VivokaEngineFactory.create(VivokaConfig.DEFAULT))
                } else {
                    Result.failure(UnsupportedOperationException("Vivoka SDK not available"))
                }
            }
            else -> {
                // TODO: Implement actual engine creation
                Result.failure(UnsupportedOperationException("Engine $engine not yet implemented"))
            }
        }
    }

    override fun getRecommendedEngine(): SpeechEngine {
        // Prefer offline-capable engines, then native
        return when {
            isEngineAvailable(SpeechEngine.VIVOKA) -> SpeechEngine.VIVOKA
            isEngineAvailable(SpeechEngine.VOSK) -> SpeechEngine.VOSK
            else -> SpeechEngine.ANDROID_STT
        }
    }

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.ANDROID_STT -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.LANGUAGE_DETECTION
            )
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
            SpeechEngine.VIVOKA -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.WAKE_WORD,
                EngineFeature.CUSTOM_VOCABULARY
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
            SpeechEngine.APPLE_SPEECH -> emptySet() // Not available on Android
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
                notes = "Uses device's native speech recognizer"
            )
            SpeechEngine.VOSK -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 50,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download language model for offline recognition"
            )
            SpeechEngine.WHISPER -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 230,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Download Whisper model (tiny/base/small/medium)"
            )
            SpeechEngine.VIVOKA -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO"),
                requiresModelDownload = true,
                modelSizeMB = 60,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Requires Vivoka commercial license"
            )
            SpeechEngine.GOOGLE_CLOUD -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.INTERNET"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Google Cloud API key"
            )
            SpeechEngine.AZURE -> EngineRequirements(
                permissions = listOf("android.permission.RECORD_AUDIO", "android.permission.INTERNET"),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = true,
                requiresApiKey = true,
                notes = "Requires Azure subscription key"
            )
            SpeechEngine.APPLE_SPEECH -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Not available on Android"
            )
        }
    }
}
