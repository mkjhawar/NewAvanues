/**
 * StubVivokaEngine.kt - Stub Vivoka engine for unsupported platforms
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Stub implementation of IVivokaEngine for platforms where Vivoka is not available.
 * All operations return NotSupported errors gracefully.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.*
import kotlinx.coroutines.flow.*

/**
 * Stub Vivoka engine for unsupported platforms.
 *
 * @param reason Why Vivoka is not available
 */
class StubVivokaEngine(
    private val reason: String = "Vivoka not available on this platform"
) : IVivokaEngine {

    private val _state = MutableStateFlow<EngineState>(
        EngineState.Error(message = reason, recoverable = false)
    )
    private val _results = MutableSharedFlow<SpeechResult>()
    private val _errors = MutableSharedFlow<SpeechError>()
    private val _isWakeWordEnabled = MutableStateFlow(false)
    private val _wakeWordDetected = MutableSharedFlow<WakeWordEvent>()
    private val _availableModels = MutableStateFlow<List<VivokaModel>>(emptyList())
    private val _currentModel = MutableStateFlow<VivokaModel?>(null)

    override val state: StateFlow<EngineState> = _state.asStateFlow()
    override val results: Flow<SpeechResult> = _results.asSharedFlow()
    override val errors: Flow<SpeechError> = _errors.asSharedFlow()
    override val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()
    override val wakeWordDetected: SharedFlow<WakeWordEvent> = _wakeWordDetected.asSharedFlow()
    override val availableModels: StateFlow<List<VivokaModel>> = _availableModels.asStateFlow()
    override val currentModel: StateFlow<VivokaModel?> = _currentModel.asStateFlow()

    override suspend fun initialize(config: SpeechConfig): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun startListening(): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun stopListening() {
        // No-op for stub
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
        _state.value = EngineState.Destroyed
    }

    override suspend fun loadModel(modelId: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun unloadModel(): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun enableWakeWord(wakeWord: String): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun disableWakeWord(): Result<Unit> {
        return Result.success(Unit)
    }

    override fun getAvailableWakeWords(): List<String> = emptyList()

    override suspend fun isModelDownloaded(modelId: String): Boolean = false

    override suspend fun downloadModel(
        modelId: String,
        progressCallback: ((Float) -> Unit)?
    ): Result<Unit> {
        return Result.failure(UnsupportedOperationException(reason))
    }

    override suspend fun deleteModel(modelId: String): Result<Unit> {
        return Result.success(Unit)
    }

    override suspend fun getModelsDiskUsage(): Long = 0L
}
