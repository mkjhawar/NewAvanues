/**
 * VoskEngineImpl.kt - VOSK engine stub for Android
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-07
 *
 * Stub implementation of ISpeechEngine for VOSK offline recognition.
 * Returns error when VOSK SDK is not included in build.gradle.kts.
 *
 * To enable VOSK Speech:
 * 1. Add dependency: implementation("com.alphacephei:vosk-android:0.3.47")
 * 2. Replace this file with the full VoskEngineImpl implementation
 * 3. Download VOSK model to device storage
 */
package com.augmentalis.voiceoscoreng.features

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * VOSK offline speech recognition stub.
 *
 * This is a placeholder implementation that returns an error when used.
 * To enable VOSK offline recognition, add the VOSK AAR dependency
 * and replace this stub with the full implementation.
 *
 * Requirements for full implementation:
 * - RECORD_AUDIO permission
 * - VOSK model downloaded to device storage
 *
 * SDK Dependency (add to build.gradle.kts):
 * implementation("com.alphacephei:vosk-android:0.3.47")
 *
 * @param context Android application context
 */
class VoskEngineImpl(
    private val context: Context
) : ISpeechEngine {

    companion object {
        private const val TAG = "VoskEngine"
        private const val NOT_AVAILABLE_MSG = "VOSK SDK not included. Add com.alphacephei:vosk-android to build.gradle.kts"
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

    override fun getEngineType(): SpeechEngine = SpeechEngine.VOSK

    override fun getSupportedFeatures(): Set<EngineFeature> = setOf(
        EngineFeature.OFFLINE_MODE,
        EngineFeature.CUSTOM_VOCABULARY,
        EngineFeature.CONTINUOUS_RECOGNITION,
        EngineFeature.WORD_TIMESTAMPS
    )

    override suspend fun destroy() {
        _state.value = EngineState.Destroyed
    }
}
