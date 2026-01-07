/**
 * SpeechEngineFactoryProvider.kt - iOS implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-05
 *
 * iOS-specific factory provider for speech engines.
 */
package com.augmentalis.voiceoscoreng.features

import com.augmentalis.voiceoscoreng.speech.AppleSpeechEngine as RealAppleSpeechEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * iOS implementation of SpeechEngineFactoryProvider.
 *
 * Creates IOSSpeechEngineFactory which supports:
 * - APPLE_SPEECH (native Speech.framework)
 * - GOOGLE_CLOUD (online)
 * - AZURE (online)
 * - WHISPER (offline, requires model)
 */
actual object SpeechEngineFactoryProvider {
    actual fun create(): ISpeechEngineFactory = IOSSpeechEngineFactory()
}

/**
 * iOS-specific speech engine factory.
 */
class IOSSpeechEngineFactory : ISpeechEngineFactory {

    override fun getAvailableEngines(): List<SpeechEngine> = listOf(
        SpeechEngine.APPLE_SPEECH,
        SpeechEngine.GOOGLE_CLOUD,
        SpeechEngine.AZURE
    )

    override fun isEngineAvailable(engine: SpeechEngine): Boolean {
        return engine in getAvailableEngines()
    }

    override fun createEngine(engine: SpeechEngine): Result<ISpeechEngine> {
        return when (engine) {
            SpeechEngine.APPLE_SPEECH -> Result.success(RealAppleSpeechEngine())
            SpeechEngine.GOOGLE_CLOUD -> Result.success(IOSGoogleCloudEngine())
            SpeechEngine.AZURE -> Result.success(IOSAzureEngine())
            else -> Result.failure(
                IllegalArgumentException("Engine ${engine.name} not available on iOS")
            )
        }
    }

    override fun getRecommendedEngine(): SpeechEngine = SpeechEngine.APPLE_SPEECH

    override fun getEngineFeatures(engine: SpeechEngine): Set<EngineFeature> {
        return when (engine) {
            SpeechEngine.APPLE_SPEECH -> setOf(
                EngineFeature.OFFLINE_MODE,
                EngineFeature.CONTINUOUS_RECOGNITION,
                EngineFeature.PUNCTUATION
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
            else -> emptySet()
        }
    }

    override fun getSetupRequirements(engine: SpeechEngine): EngineRequirements {
        return when (engine) {
            SpeechEngine.APPLE_SPEECH -> EngineRequirements(
                permissions = listOf(
                    "NSMicrophoneUsageDescription",
                    "NSSpeechRecognitionUsageDescription"
                ),
                requiresModelDownload = false,
                modelSizeMB = 0,
                requiresNetwork = false,
                requiresApiKey = false,
                notes = "Uses native Speech.framework"
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
                notes = "Engine not supported on iOS"
            )
        }
    }
}

/**
 * iOS Apple Speech.framework implementation stub
 */
internal class AppleSpeechEngine : ISpeechEngine {
    // TODO: Implement using SFSpeechRecognizer
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
    override fun getEngineType() = SpeechEngine.APPLE_SPEECH
    override fun getSupportedFeatures() = setOf(EngineFeature.OFFLINE_MODE)
    override suspend fun destroy() {}
}

internal class IOSGoogleCloudEngine : ISpeechEngine {
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

internal class IOSAzureEngine : ISpeechEngine {
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
