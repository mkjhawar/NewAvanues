/**
 * AzureEngineImpl.kt - Azure Cognitive Services Speech SDK stub
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * Stub implementation of ISpeechEngine for Azure Cognitive Services.
 * Returns error when Azure SDK is not included in build.gradle.kts.
 *
 * To enable Azure Speech:
 * 1. Add dependency: implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.35.0")
 * 2. Replace this file with the full AzureEngineImpl implementation
 */
package com.augmentalis.voiceoscoreng.features

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Azure Cognitive Services Speech SDK stub.
 *
 * This is a placeholder implementation that returns an error when used.
 * To enable Azure Speech recognition, add the Azure Speech SDK dependency
 * and replace this stub with the full implementation.
 *
 * Requirements for full implementation:
 * - Azure subscription key and region
 * - RECORD_AUDIO permission
 * - Network connectivity
 *
 * SDK Dependency (add to build.gradle.kts):
 * implementation("com.microsoft.cognitiveservices.speech:client-sdk:1.35.0")
 */
class AzureEngineImpl : ISpeechEngine {

    companion object {
        private const val TAG = "AzureEngine"
        private const val NOT_AVAILABLE_MSG = "Azure Speech SDK not included. Add com.microsoft.cognitiveservices.speech:client-sdk to build.gradle.kts"
    }

    private val _state = MutableStateFlow<EngineState>(EngineState.Uninitialized)
    override val state: StateFlow<EngineState> = _state.asStateFlow()

    private val _results = MutableSharedFlow<SpeechResult>(extraBufferCapacity = 1)
    override val results: SharedFlow<SpeechResult> = _results.asSharedFlow()

    private val _errors = MutableSharedFlow<SpeechError>(extraBufferCapacity = 1)
    override val errors: SharedFlow<SpeechError> = _errors.asSharedFlow()

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        Log.e(TAG, NOT_AVAILABLE_MSG)
        _state.value = EngineState.Error(NOT_AVAILABLE_MSG, recoverable = false)
        return Result.failure(UnsupportedOperationException(NOT_AVAILABLE_MSG))
    }

    override suspend fun startListening(): Result<Unit> {
        return Result.failure(UnsupportedOperationException(NOT_AVAILABLE_MSG))
    }

    override suspend fun stopListening() {
        // No-op
    }

    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        return Result.failure(UnsupportedOperationException(NOT_AVAILABLE_MSG))
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        return Result.failure(UnsupportedOperationException(NOT_AVAILABLE_MSG))
    }

    override fun isRecognizing(): Boolean = false

    override fun isInitialized(): Boolean = false

    override fun getEngineType(): SpeechEngine = SpeechEngine.AZURE

    override fun getSupportedFeatures(): Set<EngineFeature> = setOf(
        EngineFeature.CONTINUOUS_RECOGNITION,
        EngineFeature.PUNCTUATION,
        EngineFeature.WORD_TIMESTAMPS,
        EngineFeature.TRANSLATION,
        EngineFeature.SPEAKER_DIARIZATION
    )

    override suspend fun destroy() {
        _state.value = EngineState.Destroyed
    }
}
