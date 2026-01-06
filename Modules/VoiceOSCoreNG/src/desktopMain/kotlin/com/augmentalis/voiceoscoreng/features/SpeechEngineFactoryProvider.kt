/**
 * SpeechEngineFactoryProvider.kt - Desktop (JVM) implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * Desktop-specific factory provider for speech engines.
 */
package com.augmentalis.voiceoscoreng.features

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Desktop implementation of SpeechEngineFactoryProvider.
 *
 * Creates DesktopSpeechEngineFactory which supports:
 * - VOSK (offline, recommended)
 * - GOOGLE_CLOUD (online)
 * - AZURE (online)
 * - WHISPER (offline, requires native libs)
 */
actual object SpeechEngineFactoryProvider {
    actual fun create(): ISpeechEngineFactory = DesktopSpeechEngineFactory()
}

/**
 * Desktop-specific speech engine factory.
 */
class DesktopSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> = listOf(
        SpeechEngine.VOSK,
        SpeechEngine.GOOGLE_CLOUD,
        SpeechEngine.AZURE
    )

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return engine in getAvailableEngines()
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return when (engine) {
            SpeechEngine.VOSK -> Result.success(DesktopVoskEngine())
            SpeechEngine.GOOGLE_CLOUD -> Result.success(DesktopGoogleCloudEngine())
            SpeechEngine.AZURE -> Result.success(DesktopAzureEngine())
            else -> Result.failure(
                IllegalArgumentException("Engine ${engine.name} not available on Desktop")
            )
        }
    }

    override fun getRecommendedEngine(): SpeechEngine = SpeechEngine.VOSK

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.VOSK -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.CUSTOM_VOCABULARY
            )
            SpeechEngine.GOOGLE_CLOUD -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.PUNCTUATION,
                EngineFeature.WORD_TIMESTAMPS
            )
            SpeechEngine.AZURE -> setOf(
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.PUNCTUATION,
                EngineFeature.TRANSLATION
            )
            SpeechEngine.WHISPER -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.WORD_TIMESTAMPS,
                EngineFeature.LANGUAGE_DETECTION,
                EngineFeature.TRANSLATION
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
                notes = "Download model from alphacephei.com/vosk/models"
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
            SpeechEngine.WHISPER -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = true,
                modelSizeMB = 244,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Requires whisper.cpp native library"
            )
            else -> EngineRequirements(
                permissions = emptyList(),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Engine not supported on Desktop"
            )
        }
    }
}

/**
 * Desktop VOSK implementation stub
 */
internal class DesktopVoskEngine : ISpeechEngine {
    // TODO: Implement using vosk-api JNI bindings
    override val state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val results = MutableSharedFlow<SpeechResult>()
    override val errors = MutableSharedFlow<SpeechError>()

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

internal class DesktopGoogleCloudEngine : ISpeechEngine {
    override val state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val results = MutableSharedFlow<SpeechResult>()
    override val errors = MutableSharedFlow<SpeechError>()

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

internal class DesktopAzureEngine : ISpeechEngine {
    override val state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val results = MutableSharedFlow<SpeechResult>()
    override val errors = MutableSharedFlow<SpeechError>()

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
