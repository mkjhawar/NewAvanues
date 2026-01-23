/**
 * StubVivokaEngine.kt - Stub implementation for platforms without Vivoka
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-22
 *
 * Provides a no-op implementation of IVivokaEngine for platforms
 * where Vivoka SDK is not available (iOS, Desktop).
 */
package com.augmentalis.voiceoscore

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * Stub Vivoka engine for unsupported platforms.
 *
 * All operations return NotSupported results.
 *
 * @param reason Human-readable reason why Vivoka is not available
 */
class StubVivokaEngine(
    private val reason: String
) : IVivokaEngine {

    // =========================================================================
    // ISpeechEngine implementation
    // =========================================================================

    private val _state = MutableStateFlow<EngineState>(
        EngineState.Error(reason, recoverable = false)
    )
    override val state: StateFlow<EngineState> = _state

    override val results: Flow<SpeechResult> = emptyFlow()
    override val errors: Flow<SpeechError> = emptyFlow()

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun startListening(): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun stopListening() {
        // No-op
    }

    override suspend fun updateCommands(commands: List<String>): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun updateConfiguration(config: SpeechConfig): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override fun isRecognizing(): Boolean = false

    override fun isInitialized(): Boolean = false

    override fun getEngineType(): SpeechEngine = SpeechEngine.VIVOKA

    override fun getSupportedFeatures(): Set<EngineFeature> = emptySet()

    override suspend fun destroy() {
        // No-op
    }

    // =========================================================================
    // IWakeWordCapable implementation
    // =========================================================================

    private val _isWakeWordEnabled = MutableStateFlow(false)
    override val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled

    private val _wakeWordDetected = MutableSharedFlow<WakeWordEvent>()
    override val wakeWordDetected: SharedFlow<WakeWordEvent> = _wakeWordDetected

    override suspend fun enableWakeWord(wakeWord: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun disableWakeWord(): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override fun getAvailableWakeWords(): List<String> = emptyList()

    // =========================================================================
    // IModelManageable implementation
    // =========================================================================

    private val _availableModels = MutableStateFlow<List<VivokaModel>>(emptyList())
    override val availableModels: StateFlow<List<VivokaModel>> = _availableModels

    private val _currentModel = MutableStateFlow<VivokaModel?>(null)
    override val currentModel: StateFlow<VivokaModel?> = _currentModel

    override suspend fun loadModel(modelId: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun unloadModel(): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun isModelDownloaded(modelId: String): Boolean = false

    override suspend fun downloadModel(
        modelId: String,
        progressCallback: ((Float) -> Unit)?
    ): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun getModelsDiskUsage(): Long = 0L
}
